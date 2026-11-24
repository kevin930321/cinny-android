use std::{collections::HashMap, sync::Arc};

use futures_util::StreamExt;
use matrix_sdk::{
    room::{edit::EditedContent, Room},
    ruma::{
        api::client::receipt::create_receipt::v3::ReceiptType,
        events::{
            reaction::ReactionEventContent,
            relation::Annotation,
            room::message::{
                MessageType, RoomMessageEventContent, RoomMessageEventContentWithoutRelation,
            },
        },
        EventId, OwnedEventId, UserId,
    },
};
use matrix_sdk_ui::timeline::{
    EventSendState, EventTimelineItem, InReplyToDetails, MsgLikeKind, RoomExt, Timeline,
    TimelineDetails, TimelineEventItemId, TimelineFocus, TimelineItem, TimelineItemContent,
};
use tokio::sync::Mutex;

use crate::{
    error::CinnyError,
    media::{extract_attachment_info, send_media_attachment},
    models::{MessageDeliveryStatus, ReactionBadge, ReplyPreview, TimelineMessage},
};

#[uniffi::export(callback_interface)]
pub trait TimelineListener: Send + Sync {
    fn on_timeline_updated(&self, room_id: String, messages: Vec<TimelineMessage>);
}

pub struct ActiveRoomTimeline {
    room: Room,
    own_user_id: String,
    timeline: Arc<Timeline>,
    _task_handle: tokio::task::JoinHandle<()>,
}

pub struct ActiveThreadTimeline {
    timeline: Arc<Timeline>,
    _task_handle: tokio::task::JoinHandle<()>,
}

pub async fn open_room_timeline(
    room: Room,
    own_user_id: String,
    listener: Box<dyn TimelineListener>,
) -> Result<ActiveRoomTimeline, CinnyError> {
    let timeline = Arc::new(room.timeline().await.map_err(|e| CinnyError::Network {
        detail: format!("Failed to initialize room timeline: {e}"),
    })?);

    let (initial_items, mut stream) = timeline.subscribe().await;
    let room_id = room.room_id().to_string();
    let listener: Arc<dyn TimelineListener> = Arc::from(listener);

    let initial_messages = map_timeline_items(&initial_items, &own_user_id, None);
    listener.on_timeline_updated(room_id.clone(), initial_messages);

    let timeline_for_task = Arc::clone(&timeline);
    let own_for_task = own_user_id.clone();
    let task_handle = tokio::spawn(async move {
        let mut current = initial_items;
        while let Some(diffs) = stream.next().await {
            for diff in diffs {
                diff.apply(&mut current);
            }
            let mapped = map_timeline_items(&current, &own_for_task, None);
            listener.on_timeline_updated(room_id.clone(), mapped);
        }
        drop(timeline_for_task);
    });

    Ok(ActiveRoomTimeline {
        room,
        own_user_id,
        timeline,
        _task_handle: task_handle,
    })
}

pub async fn open_thread_timeline(
    room: Room,
    own_user_id: String,
    root_event_id: String,
    listener: Box<dyn TimelineListener>,
) -> Result<ActiveThreadTimeline, CinnyError> {
    let parsed_root = parse_event_id(&root_event_id)?;
    let timeline = Arc::new(
        room.timeline_builder()
            .with_focus(TimelineFocus::Thread {
                root_event_id: parsed_root,
            })
            .build()
            .await
            .map_err(|e| CinnyError::Network {
                detail: format!("Failed to initialize thread timeline: {e}"),
            })?,
    );

    let (initial_items, mut stream) = timeline.subscribe().await;
    let room_id = room.room_id().to_string();
    let listener: Arc<dyn TimelineListener> = Arc::from(listener);

    let initial_messages =
        map_timeline_items(&initial_items, &own_user_id, Some(root_event_id.as_str()));
    listener.on_timeline_updated(room_id.clone(), initial_messages);

    let timeline_for_task = Arc::clone(&timeline);
    let root_for_task = root_event_id.clone();
    let task_handle = tokio::spawn(async move {
        let mut current = initial_items;
        while let Some(diffs) = stream.next().await {
            for diff in diffs {
                diff.apply(&mut current);
            }
            let mapped = map_timeline_items(&current, &own_user_id, Some(root_for_task.as_str()));
            listener.on_timeline_updated(room_id.clone(), mapped);
        }
        drop(timeline_for_task);
    });

    Ok(ActiveThreadTimeline {
        timeline,
        _task_handle: task_handle,
    })
}

impl ActiveThreadTimeline {
    pub async fn send_thread_reply(&self, markdown: String) -> Result<(), CinnyError> {
    let content = RoomMessageEventContent::text_markdown(markdown);
    self.timeline
        .send(content.into())
        .await
        .map_err(|e| CinnyError::Network {
            detail: format!("Failed to send thread reply: {e}"),
        })?;
        Ok(())
    }
}

impl ActiveRoomTimeline {
    pub async fn send_markdown(
        &self,
        markdown: String,
        reply_to_event_id: Option<String>,
    ) -> Result<(), CinnyError> {
        if let Some(target_id) = reply_to_event_id {
            let parsed_id = parse_event_id(&target_id)?;
            let reply_content = RoomMessageEventContentWithoutRelation::text_markdown(markdown);
            self.timeline
                .send_reply(reply_content, parsed_id)
                .await
                .map_err(|e| CinnyError::Network {
                    detail: format!("Failed to send reply: {e}"),
                })?;
        } else {
            let content = RoomMessageEventContent::text_markdown(markdown);
            self.timeline
                .send(content.into())
                .await
                .map_err(|e| CinnyError::Network {
                    detail: format!("Failed to send message: {e}"),
                })?;
        }
        Ok(())
    }

    pub async fn send_attachment(
        &self,
        filename: String,
        mime_type: String,
        bytes: Vec<u8>,
    ) -> Result<(), CinnyError> {
        send_media_attachment(&self.room, filename, mime_type, bytes).await
    }

    pub async fn edit_markdown(
        &self,
        event_id: String,
        new_markdown: String,
    ) -> Result<(), CinnyError> {
        let parsed_id = parse_event_id(&event_id)?;
        let item_id = TimelineEventItemId::EventId(parsed_id);
        let edited = RoomMessageEventContentWithoutRelation::text_markdown(new_markdown);
        self.timeline
            .edit(&item_id, EditedContent::RoomMessage(edited))
            .await
            .map_err(|e| CinnyError::Network {
                detail: format!("Failed to edit message: {e}"),
            })?;
        Ok(())
    }

    pub async fn toggle_reaction(&self, event_id: String, emoji: String) -> Result<(), CinnyError> {
        let parsed_id = parse_event_id(&event_id)?;
        let item_id = TimelineEventItemId::EventId(parsed_id.clone());
        if self.timeline.toggle_reaction(&item_id, &emoji).await.is_err() {
            let annotation = Annotation::new(parsed_id, emoji);
            let reaction = ReactionEventContent::new(annotation);
            self.room
                .send(reaction)
                .await
                .map_err(|e| CinnyError::Network {
                    detail: format!("Reaction failed: {e}"),
                })?;
        }
        Ok(())
    }

    pub async fn redact_message(
        &self,
        event_id: String,
        reason: Option<String>,
    ) -> Result<(), CinnyError> {
        let parsed_id = parse_event_id(&event_id)?;
        self.room
            .redact(&parsed_id, reason.as_deref(), None)
            .await
            .map_err(|e| CinnyError::Network {
                detail: format!("Failed to redact event: {e}"),
            })?;
        Ok(())
    }

    pub async fn paginate_backwards(&self, limit: u16) -> Result<bool, CinnyError> {
        self.timeline
            .paginate_backwards(limit)
            .await
            .map_err(|e| CinnyError::Network {
                detail: format!("Pagination failed: {e}"),
            })
    }

    pub async fn mark_as_read(&self) -> Result<bool, CinnyError> {
        self.timeline
            .mark_as_read(ReceiptType::Read)
            .await
            .map_err(|e| CinnyError::Network {
                detail: format!("Mark as read failed: {e}"),
            })
    }

    /// Searches decrypted timeline messages locally so E2EE rooms remain searchable on-device.
    pub async fn search_loaded_messages(&self, query: &str) -> Vec<TimelineMessage> {
        let needle = query.trim().to_lowercase();
        if needle.is_empty() {
            return Vec::new();
        }
        let items = self.timeline.items().await;
        map_timeline_items(&items, &self.own_user_id, None)
            .into_iter()
            .filter(|m| {
                !m.is_redacted
                    && (m.body.to_lowercase().contains(&needle)
                        || m.sender_name.to_lowercase().contains(&needle))
            })
            .collect()
    }
}

fn map_timeline_items<'a, I>(
    items: I,
    own_user_id: &str,
    thread_root_override: Option<&str>,
) -> Vec<TimelineMessage>
where
    I: IntoIterator<Item = &'a Arc<TimelineItem>>,
{
    items
        .into_iter()
        .filter_map(|item| {
            item.as_event()
                .map(|ev| map_event_item(ev, own_user_id, thread_root_override))
        })
        .collect()
}

fn map_event_item(
    event: &EventTimelineItem,
    own_user_id: &str,
    thread_root_override: Option<&str>,
) -> TimelineMessage {
    let event_id = event
        .event_id()
        .map(|id| id.to_string())
        .unwrap_or_else(|| match event.identifier() {
            TimelineEventItemId::EventId(id) => id.to_string(),
            TimelineEventItemId::TransactionId(txn) => txn.to_string(),
        });

    let sender_id = event.sender().to_string();
    let (sender_name, sender_avatar_mxc) = match event.sender_profile() {
        TimelineDetails::Ready(profile) => (
            profile
                .display_name
                .clone()
                .unwrap_or_else(|| sender_id.clone()),
            profile.avatar_url.as_ref().map(|u| u.to_string()),
        ),
        _ => (sender_id.clone(), None),
    };

    let own_parsed = UserId::parse(own_user_id).ok();

    let (
        body,
        formatted_html,
        is_edited,
        is_redacted,
        is_unable_to_decrypt,
        reply_to,
        attachment,
        thread_reply_count,
        reactions,
    ) = match event.content() {
        TimelineItemContent::MsgLike(msg_like) => {
            let reactions = map_reactions(&msg_like.reactions, own_parsed.as_deref());
            let reply = msg_like.in_reply_to.as_ref().map(map_reply_details);
            let thread_root_count = msg_like
                .thread_summary
                .as_ref()
                .map(|s| s.num_replies)
                .unwrap_or(0);

            match &msg_like.kind {
                MsgLikeKind::Message(msg) => {
                    let (text, html) = extract_message_body(msg.msgtype());
                    let attach = extract_attachment_info(msg.msgtype());
                    (
                        text,
                        html,
                        msg.is_edited(),
                        false,
                        false,
                        reply,
                        attach,
                        thread_root_count,
                        reactions,
                    )
                }
                MsgLikeKind::Redacted => (
                    "Message deleted".to_string(),
                    None,
                    false,
                    true,
                    false,
                    None,
                    None,
                    0,
                    Vec::new(),
                ),
                MsgLikeKind::UnableToDecrypt(_) => (
                    "Waiting for Megolm decryption key...".to_string(),
                    None,
                    false,
                    false,
                    true,
                    reply,
                    None,
                    0,
                    reactions,
                ),
                MsgLikeKind::Sticker(sticker) => (
                    format!("[Sticker] {}", sticker.content().body),
                    None,
                    false,
                    false,
                    false,
                    reply,
                    None,
                    0,
                    reactions,
                ),
                _ => (
                    "Unsupported event".to_string(),
                    None,
                    false,
                    false,
                    false,
                    reply,
                    None,
                    0,
                    reactions,
                ),
            }
        }
        _ => (
            "Room state updated".to_string(),
            None,
            false,
            false,
            false,
            None,
            None,
            0,
            Vec::new(),
        ),
    };

    let delivery_status = match event.send_state() {
        Some(EventSendState::NotSentYet { .. }) => MessageDeliveryStatus::Sending,
        Some(EventSendState::SendingFailed { error, .. }) => MessageDeliveryStatus::Failed {
            reason: error.to_string(),
        },
        _ => MessageDeliveryStatus::Sent,
    };

    TimelineMessage {
        event_id,
        sender_id,
        sender_name,
        sender_avatar_mxc,
        timestamp_ms: event.timestamp().0.into(),
        body,
        formatted_html,
        is_edited,
        is_redacted,
        is_encrypted: event.encryption_info().is_some(),
        is_unable_to_decrypt,
        is_own: event.is_own(),
        can_reply: event.can_be_replied_to(),
        thread_root_id: thread_root_override.map(|s| s.to_string()),
        thread_reply_count,
        reply_to,
        attachment,
        reactions,
        delivery_status,
    }
}

fn map_reactions(
    reactions: &matrix_sdk_ui::timeline::ReactionsByKeyBySender,
    own_user_id: Option<&UserId>,
) -> Vec<ReactionBadge> {
    reactions
        .iter()
        .map(|(key, senders)| {
            let reacted_by_me = own_user_id
                .map(|uid| senders.contains_key(uid))
                .unwrap_or(false);
            ReactionBadge {
                key: key.clone(),
                count: senders.len() as u32,
                reacted_by_me,
            }
        })
        .collect()
}

fn map_reply_details(details: &InReplyToDetails) -> ReplyPreview {
    let event_id = details.event_id.to_string();
    match &details.event {
        TimelineDetails::Ready(embedded) => {
            let sender_name = match &embedded.sender_profile {
                TimelineDetails::Ready(profile) => profile
                    .display_name
                    .clone()
                    .unwrap_or_else(|| embedded.sender.to_string()),
                _ => embedded.sender.to_string(),
            };
            let body = match &embedded.content {
                TimelineItemContent::MsgLike(msg_like) => match &msg_like.kind {
                    MsgLikeKind::Message(msg) => extract_message_body(msg.msgtype()).0,
                    MsgLikeKind::Redacted => "Message deleted".to_string(),
                    _ => "Message".to_string(),
                },
                _ => "Event".to_string(),
            };
            ReplyPreview {
                event_id,
                sender_name,
                body,
            }
        }
        TimelineDetails::Pending => ReplyPreview {
            event_id,
            sender_name: "Loading...".to_string(),
            body: "Fetching replied message...".to_string(),
        },
        TimelineDetails::Unavailable | TimelineDetails::Error(_) => ReplyPreview {
            event_id,
            sender_name: "Unknown".to_string(),
            body: "Quoted message unavailable".to_string(),
        },
    }
}

fn extract_message_body(msgtype: &MessageType) -> (String, Option<String>) {
    match msgtype {
        MessageType::Text(t) => (
            t.body.clone(),
            t.formatted.as_ref().map(|f| f.body.clone()),
        ),
        MessageType::Notice(n) => (n.body.clone(), n.formatted.as_ref().map(|f| f.body.clone())),
        MessageType::Emote(e) => (format!("* {}", e.body), None),
        MessageType::Image(img) => (format!("[Image] {}", img.body), None),
        MessageType::File(file) => (format!("[File] {}", file.body), None),
        other => (other.body().to_string(), None),
    }
}

pub fn parse_event_id(id: &str) -> Result<OwnedEventId, CinnyError> {
    EventId::parse(id).map_err(|e| CinnyError::InvalidInput {
        detail: format!("Invalid Matrix event ID '{}': {}", id, e),
    })
}

pub type SharedTimelineMap = Arc<Mutex<HashMap<String, Arc<ActiveRoomTimeline>>>>;
pub type SharedThreadTimelineMap = Arc<Mutex<HashMap<String, Arc<ActiveThreadTimeline>>>>;

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn parse_event_id_rejects_invalid_ids() {
        assert!(parse_event_id("$valid_event_id").is_ok());
        assert!(parse_event_id("invalid_without_dollar").is_err());
    }
}
