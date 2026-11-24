use matrix_sdk::{
    ruma::{
        api::client::push::{
            set_pusher::v3::Request as SetPusherRequest, HttpPusherData, Pusher, PusherIds,
            PusherInit, PusherKind,
        },
        events::{
            room::message::{MessageType, SyncRoomMessageEvent},
            AnySyncMessageLikeEvent, AnySyncTimelineEvent,
        },
    },
    Client,
};

use crate::{
    error::CinnyError,
    models::PushNotificationSummary,
    spaces::parse_room_id,
    timeline::parse_event_id,
};

/// Registers a UnifiedPush / Sygnal HTTP pusher with the Matrix homeserver.
pub async fn register_http_pusher(
    client: &Client,
    push_key: String,
    gateway_url: String,
    app_id: String,
    app_display_name: String,
    device_display_name: String,
) -> Result<(), CinnyError> {
    if push_key.trim().is_empty() || gateway_url.trim().is_empty() {
        return Err(CinnyError::InvalidInput {
            message: "Push key and gateway URL are required".to_string(),
        });
    }

    let mut data = HttpPusherData::new(gateway_url);
    data.format = Some("event_id_only".into());

    let pusher = Pusher::from(PusherInit {
        ids: PusherIds::new(push_key, app_id),
        kind: PusherKind::Http(data),
        app_display_name,
        device_display_name,
        profile_tag: None,
        lang: "en".to_string(),
    });

    let request = SetPusherRequest::post(pusher);
    client
        .send(request)
        .await
        .map_err(|e| CinnyError::Network {
            message: format!("Failed to register HTTP pusher: {e}"),
        })?;
    Ok(())
}

/// Fetches a push-notified event from the homeserver, decrypts it with local Megolm keys if E2EE,
/// and builds a notification summary safe for Android `NotificationManager`.
pub async fn process_push_event(
    client: &Client,
    room_id: String,
    event_id: String,
) -> Result<PushNotificationSummary, CinnyError> {
    let parsed_room = parse_room_id(&room_id)?;
    let parsed_event = parse_event_id(&event_id)?;

    let room = client
        .get_room(&parsed_room)
        .ok_or_else(|| CinnyError::RoomNotFound {
            room_id: room_id.clone(),
        })?;

    let room_name = room
        .cached_display_name()
        .map(|n| n.to_string())
        .unwrap_or_else(|| room_id.clone());
    let is_encrypted = room
        .latest_encryption_state()
        .await
        .map(|s| s.is_encrypted())
        .unwrap_or(false);
    let unread_count = room.unread_notification_counts().notification_count as u32;

    let timeline_event = room
        .event(&parsed_event, None)
        .await
        .map_err(|e| CinnyError::Network {
            message: format!("Push event fetch failed: {e}"),
        })?;

    let mut sender_name = "Matrix User".to_string();
    let mut body_preview = if is_encrypted {
        "New encrypted message".to_string()
    } else {
        "New message".to_string()
    };

    if let Ok(AnySyncTimelineEvent::MessageLike(AnySyncMessageLikeEvent::RoomMessage(
        SyncRoomMessageEvent::Original(orig),
    ))) = timeline_event.raw().deserialize()
    {
        sender_name = orig.sender.to_string();
        body_preview = match orig.content.msgtype {
            MessageType::Text(t) => t.body,
            MessageType::Notice(n) => n.body,
            MessageType::Emote(e) => format!("* {}", e.body),
            MessageType::Image(i) => format!("[Image] {}", i.body),
            MessageType::File(f) => format!("[File] {}", f.body),
            other => other.body().to_string(),
        };
    }

    Ok(PushNotificationSummary {
        room_id,
        event_id,
        room_name,
        sender_name,
        body_preview,
        is_encrypted,
        unread_count,
    })
}
