use std::collections::{HashMap, HashSet};

use matrix_sdk::{
    deserialized_responses::RawAnySyncOrStrippedState,
    room::Room,
    ruma::{
        api::client::{
            room::{create_room::v3::Request as CreateRoomRequest, Visibility},
            space::get_hierarchy::v1::Request as SpaceHierarchyRequest,
        },
        events::{
            room::{
                encryption::RoomEncryptionEventContent,
                message::{MessageType, SyncRoomMessageEvent},
                power_levels::UserPowerLevel,
            },
            InitialStateEvent, StateEventType,
        },
        OwnedRoomId, OwnedUserId, RoomId, RoomOrAliasId,
    },
    Client, RoomMemberships, RoomState,
};
use matrix_sdk_ui::room_list_service::RoomListItem;

use crate::{
    error::CinnyError,
    models::{RoomCategory, RoomMemberSummary, RoomSummary, SpaceHierarchyEntry, SpaceNode},
};

/// Builds Cinny's Space-first hierarchy from RoomListService entries (or fallback rooms),
/// preserving parent-child Space nesting, unread notification rollups, and last-event activity sorting.
pub async fn build_space_hierarchy_from_items(
    client: &Client,
    items: &[RoomListItem],
) -> (Vec<SpaceNode>, Vec<RoomSummary>) {
    let rooms: Vec<Room> = if items.is_empty() {
        client.rooms()
    } else {
        items.iter().map(|item| (**item).clone()).collect()
    };
    build_space_hierarchy_from_rooms(&rooms).await
}

pub async fn build_space_hierarchy_from_rooms(
    rooms: &[Room],
) -> (Vec<SpaceNode>, Vec<RoomSummary>) {
    let mut space_children_map: HashMap<String, Vec<String>> = HashMap::new();
    let mut parent_spaces_by_node: HashMap<String, Vec<String>> = HashMap::new();
    let mut raw_spaces: HashMap<String, Room> = HashMap::new();
    let mut raw_rooms: Vec<Room> = Vec::new();

    for room in rooms {
        if room.state() != RoomState::Joined && room.state() != RoomState::Invited {
            continue;
        }
        if room.is_space() {
            let space_id = room.room_id().to_string();
            let children = extract_space_children(room).await;
            for child_id in &children {
                parent_spaces_by_node
                    .entry(child_id.clone())
                    .or_default()
                    .push(space_id.clone());
            }
            space_children_map.insert(space_id.clone(), children);
            raw_spaces.insert(space_id, room.clone());
        } else {
            raw_rooms.push(room.clone());
        }
    }

    let space_ids: HashSet<String> = raw_spaces.keys().cloned().collect();
    let mut summaries = Vec::with_capacity(raw_rooms.len());
    let mut unread_by_room: HashMap<String, (u32, u32)> = HashMap::new();

    for room in raw_rooms {
        let room_id = room.room_id().to_string();
        let parents = parent_spaces_by_node
            .get(&room_id)
            .cloned()
            .unwrap_or_default();
        let is_direct = room.is_direct().await.unwrap_or(false);
        let category = if room.state() == RoomState::Invited {
            RoomCategory::Invited
        } else if is_direct {
            RoomCategory::DirectMessage
        } else if !parents.is_empty() {
            RoomCategory::SpaceChannel
        } else {
            RoomCategory::OrphanRoom
        };

        let counts = room.unread_notification_counts();
        let unread_count = counts.notification_count as u32;
        let highlight_count = counts.highlight_count as u32;
        unread_by_room.insert(room_id.clone(), (unread_count, highlight_count));

        let display_name = room
            .cached_display_name()
            .map(|n| n.to_string())
            .unwrap_or_else(|| room_id.clone());

        let (last_message_preview, last_message_sender, last_activity_ts_ms) =
            extract_latest_event_preview(&room).await;

        summaries.push(RoomSummary {
            room_id,
            name: display_name,
            topic: room.topic(),
            avatar_mxc: room.avatar_url().map(|u| u.to_string()),
            is_encrypted: room
                .latest_encryption_state()
                .await
                .map(|s| s.is_encrypted())
                .unwrap_or(false),
            is_direct,
            category,
            unread_count,
            highlight_count,
            last_message_preview,
            last_message_sender,
            last_activity_ts_ms,
            parent_space_ids: parents,
        });
    }

    summaries.sort_by(|a, b| {
        b.highlight_count
            .cmp(&a.highlight_count)
            .then_with(|| b.last_activity_ts_ms.cmp(&a.last_activity_ts_ms))
            .then_with(|| b.unread_count.cmp(&a.unread_count))
            .then_with(|| a.name.to_lowercase().cmp(&b.name.to_lowercase()))
    });

    let mut spaces = Vec::with_capacity(raw_spaces.len());
    for (space_id, space_room) in raw_spaces {
        let all_children = space_children_map.remove(&space_id).unwrap_or_default();
        let mut child_room_ids = Vec::new();
        let mut child_space_ids = Vec::new();
        let mut agg_unread = 0u32;
        let mut agg_highlights = 0u32;

        for child in all_children {
            if space_ids.contains(&child) {
                child_space_ids.push(child);
            } else {
                if let Some((u, h)) = unread_by_room.get(&child) {
                    agg_unread = agg_unread.saturating_add(*u);
                    agg_highlights = agg_highlights.saturating_add(*h);
                }
                child_room_ids.push(child);
            }
        }

        let name = space_room
            .cached_display_name()
            .map(|n| n.to_string())
            .unwrap_or_else(|| space_id.clone());

        let parent_space_ids = parent_spaces_by_node
            .get(&space_id)
            .cloned()
            .unwrap_or_default();

        spaces.push(SpaceNode {
            room_id: space_id,
            name,
            topic: space_room.topic(),
            avatar_mxc: space_room.avatar_url().map(|u| u.to_string()),
            child_room_ids,
            child_space_ids,
            parent_space_ids,
            unread_notifications: agg_unread,
            highlight_count: agg_highlights,
        });
    }

    spaces.sort_by(|a, b| a.name.to_lowercase().cmp(&b.name.to_lowercase()));
    (spaces, summaries)
}

pub async fn browse_space_hierarchy(
    client: &Client,
    space_id: &str,
) -> Result<Vec<SpaceHierarchyEntry>, CinnyError> {
    let parsed_id = parse_room_id(space_id)?;
    let mut req = SpaceHierarchyRequest::new(parsed_id);
    req.limit = Some(50u32.into());

    let resp = client.send(req).await.map_err(|e| CinnyError::Network {
        detail: format!("Failed to fetch Space hierarchy: {e}"),
    })?;

    let mut entries = Vec::with_capacity(resp.rooms.len());
    for chunk in resp.rooms {
        let room_id_str = chunk.summary.room_id.to_string();
        let is_joined = client
            .get_room(&chunk.summary.room_id)
            .map(|r| r.state() == RoomState::Joined)
            .unwrap_or(false);
        let is_space = chunk
            .summary
            .room_type
            .as_ref()
            .map(|t| t.as_str() == "m.space")
            .unwrap_or(false);

        entries.push(SpaceHierarchyEntry {
            room_id: chunk.summary.room_id.to_string(),
            name: chunk
                .summary
                .name
                .unwrap_or_else(|| room_id_str.clone()),
            topic: chunk.summary.topic,
            avatar_mxc: chunk.summary.avatar_url.map(|u| u.to_string()),
            num_joined_members: u64::from(chunk.summary.num_joined_members) as u32,
            is_space,
            is_joined,
        });
    }
    Ok(entries)
}

pub async fn create_room(
    client: &Client,
    name: String,
    topic: Option<String>,
    is_direct: bool,
    is_encrypted: bool,
    invite_user_ids: Vec<String>,
) -> Result<String, CinnyError> {
    let mut invites = Vec::with_capacity(invite_user_ids.len());
    for uid in invite_user_ids {
        let trimmed = uid.trim();
        if !trimmed.is_empty() {
            invites.push(
                OwnedUserId::try_from(trimmed).map_err(|e| CinnyError::InvalidInput {
                    detail: format!("Invalid invitee '{trimmed}': {e}"),
                })?,
            );
        }
    }

    let mut req = CreateRoomRequest::new();
    req.name = Some(name);
    req.topic = topic.filter(|t| !t.trim().is_empty());
    req.is_direct = is_direct;
    req.visibility = Visibility::Private;
    req.invite = invites;

    if is_encrypted {
        let enc_event = InitialStateEvent::with_empty_state_key(
            RoomEncryptionEventContent::with_recommended_defaults(),
        );
        req.initial_state = vec![enc_event.to_raw_any()];
    }

    let room = client
        .create_room(req)
        .await
        .map_err(|e| CinnyError::Network {
            detail: format!("Create room failed: {e}"),
        })?;
    Ok(room.room_id().to_string())
}

pub async fn join_room_by_id_or_alias(
    client: &Client,
    room_id_or_alias: &str,
) -> Result<String, CinnyError> {
    let parsed = <&RoomOrAliasId>::try_from(room_id_or_alias.trim()).map_err(|e| {
        CinnyError::InvalidInput {
            detail: format!("Invalid Matrix room ID or alias: {e}"),
        }
    })?;
    let room = client
        .join_room_by_id_or_alias(parsed, &[])
        .await
        .map_err(|e| CinnyError::Network {
            detail: format!("Join room failed: {e}"),
        })?;
    Ok(room.room_id().to_string())
}

pub async fn accept_invite(client: &Client, room_id: &str) -> Result<(), CinnyError> {
    let parsed = parse_room_id(room_id)?;
    let room = client
        .get_room(&parsed)
        .ok_or_else(|| CinnyError::RoomNotFound {
            room_id: room_id.to_string(),
        })?;
    room.join().await.map_err(|e| CinnyError::Network {
        detail: format!("Accept invite failed: {e}"),
    })?;
    Ok(())
}

pub async fn leave_room(client: &Client, room_id: &str) -> Result<(), CinnyError> {
    let parsed = parse_room_id(room_id)?;
    let room = client
        .get_room(&parsed)
        .ok_or_else(|| CinnyError::RoomNotFound {
            room_id: room_id.to_string(),
        })?;
    room.leave().await.map_err(|e| CinnyError::Network {
        detail: format!("Leave room failed: {e}"),
    })?;
    Ok(())
}

pub async fn invite_user_to_room(
    client: &Client,
    room_id: &str,
    user_id: &str,
) -> Result<(), CinnyError> {
    let parsed_room = parse_room_id(room_id)?;
    let parsed_user = OwnedUserId::try_from(user_id.trim()).map_err(|e| CinnyError::InvalidInput {
        detail: format!("Invalid Matrix user ID: {e}"),
    })?;
    let room = client
        .get_room(&parsed_room)
        .ok_or_else(|| CinnyError::RoomNotFound {
            room_id: room_id.to_string(),
        })?;
    room.invite_user_by_id(&parsed_user)
        .await
        .map_err(|e| CinnyError::Network {
            detail: format!("Invite failed: {e}"),
        })?;
    Ok(())
}

pub async fn list_room_members(
    client: &Client,
    room_id: &str,
) -> Result<Vec<RoomMemberSummary>, CinnyError> {
    let parsed_room = parse_room_id(room_id)?;
    let room = client
        .get_room(&parsed_room)
        .ok_or_else(|| CinnyError::RoomNotFound {
            room_id: room_id.to_string(),
        })?;
    let members = room
        .members(RoomMemberships::ACTIVE)
        .await
        .map_err(|e| CinnyError::Network {
            detail: format!("Failed to load room members: {e}"),
        })?;

    let mut out: Vec<RoomMemberSummary> = members
        .into_iter()
        .map(|m| {
            let power_level = match m.power_level() {
                UserPowerLevel::Infinite => i64::MAX,
                UserPowerLevel::Int(v) => i64::from(v),
                _ => 0,
            };
            RoomMemberSummary {
                user_id: m.user_id().to_string(),
                display_name: m.name().to_string(),
                avatar_mxc: m.avatar_url().map(|u| u.to_string()),
                power_level,
            }
        })
        .collect();
    out.sort_by(|a, b| b.power_level.cmp(&a.power_level).then_with(|| a.display_name.cmp(&b.display_name)));
    Ok(out)
}

async fn extract_latest_event_preview(room: &Room) -> (Option<String>, Option<String>, u64) {
    let Some(latest) = room.latest_event() else {
        return (None, None, 0);
    };
    let mut ts: u64 = latest.event().timestamp().map(|t| t.0.into()).unwrap_or(0);
    let mut sender = latest.sender_display_name().map(|s| s.to_string());
    if let Ok(matrix_sdk::ruma::events::AnySyncTimelineEvent::MessageLike(
        matrix_sdk::ruma::events::AnySyncMessageLikeEvent::RoomMessage(
            SyncRoomMessageEvent::Original(orig),
        ),
    )) = latest.event().raw().deserialize()
    {
        if sender.is_none() {
            sender = Some(orig.sender.to_string());
        }
        if ts == 0 {
            ts = orig.origin_server_ts.0.into();
        }
        let preview = match orig.content.msgtype {
            MessageType::Text(t) => t.body,
            MessageType::Notice(n) => n.body,
            MessageType::Emote(e) => format!("* {}", e.body),
            MessageType::Image(i) => format!("[Image] {}", i.body),
            MessageType::File(f) => format!("[File] {}", f.body),
            other => other.body().to_string(),
        };
        return (Some(preview), sender, ts);
    }
    (None, sender, ts)
}

async fn extract_space_children(room: &Room) -> Vec<String> {
    let Ok(events) = room.get_state_events(StateEventType::SpaceChild).await else {
        return Vec::new();
    };
    let mut children = Vec::new();
    for raw in events {
        let raw_json_str = match &raw {
            RawAnySyncOrStrippedState::Sync(r) => r.json().get(),
            RawAnySyncOrStrippedState::Stripped(r) => r.json().get(),
        };
        if let Ok(json_val) = serde_json::from_str::<serde_json::Value>(raw_json_str) {
            let has_via = json_val
                .get("content")
                .and_then(|c| c.get("via"))
                .and_then(|v| v.as_array())
                .map(|arr| !arr.is_empty())
                .unwrap_or(false);
            if has_via {
                if let Some(state_key) = json_val.get("state_key").and_then(|k| k.as_str()) {
                    if OwnedRoomId::try_from(state_key).is_ok() {
                        children.push(state_key.to_string());
                    }
                }
            }
        }
    }
    children
}

pub fn parse_room_id(room_id: &str) -> Result<OwnedRoomId, CinnyError> {
    RoomId::parse(room_id).map_err(|e| CinnyError::InvalidInput {
        detail: format!("Invalid Matrix room ID '{}': {}", room_id, e),
    })
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn parse_room_id_validates_matrix_format() {
        assert!(parse_room_id("!abc123:matrix.org").is_ok());
        assert!(parse_room_id("not-a-room-id").is_err());
    }
}
