use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Serialize, Deserialize, uniffi::Record)]
pub struct CinnyAuthSession {
    pub homeserver_url: String,
    pub user_id: String,
    pub device_id: String,
    pub access_token: String,
    pub refresh_token: Option<String>,
    pub sqlite_store_subpath: String,
}

#[derive(Debug, Clone, uniffi::Record)]
pub struct SsoProviderInfo {
    pub id: String,
    pub name: String,
}

#[derive(Debug, Clone, uniffi::Record)]
pub struct HomeserverLoginCapabilities {
    pub homeserver_url: String,
    pub supports_password: bool,
    pub supports_sso: bool,
    pub sso_providers: Vec<SsoProviderInfo>,
}

#[derive(Debug, Clone, PartialEq, Eq, uniffi::Enum)]
pub enum SyncConnectionState {
    Offline,
    Syncing,
    CatchingUp,
    Live,
    TokenInvalid { message: String },
    Error { message: String },
}

#[derive(Debug, Clone, uniffi::Record)]
pub struct SpaceNode {
    pub room_id: String,
    pub name: String,
    pub topic: Option<String>,
    pub avatar_mxc: Option<String>,
    pub child_room_ids: Vec<String>,
    pub child_space_ids: Vec<String>,
    pub parent_space_ids: Vec<String>,
    pub unread_notifications: u32,
    pub highlight_count: u32,
}

#[derive(Debug, Clone, uniffi::Record)]
pub struct SpaceHierarchyEntry {
    pub room_id: String,
    pub name: String,
    pub topic: Option<String>,
    pub avatar_mxc: Option<String>,
    pub num_joined_members: u32,
    pub is_space: bool,
    pub is_joined: bool,
}

#[derive(Debug, Clone, PartialEq, Eq, uniffi::Enum)]
pub enum RoomCategory {
    DirectMessage,
    SpaceChannel,
    OrphanRoom,
    Invited,
}

#[derive(Debug, Clone, uniffi::Record)]
pub struct RoomSummary {
    pub room_id: String,
    pub name: String,
    pub topic: Option<String>,
    pub avatar_mxc: Option<String>,
    pub is_encrypted: bool,
    pub is_direct: bool,
    pub category: RoomCategory,
    pub unread_count: u32,
    pub highlight_count: u32,
    pub last_message_preview: Option<String>,
    pub last_message_sender: Option<String>,
    pub last_activity_ts_ms: u64,
    pub parent_space_ids: Vec<String>,
}

#[derive(Debug, Clone, uniffi::Record)]
pub struct RoomMemberSummary {
    pub user_id: String,
    pub display_name: String,
    pub avatar_mxc: Option<String>,
    pub power_level: i64,
}

#[derive(Debug, Clone, uniffi::Record)]
pub struct MediaAttachmentInfo {
    pub mxc_url: String,
    pub filename: String,
    pub mime_type: String,
    pub size_bytes: u64,
    pub is_encrypted: bool,
    pub width: Option<u32>,
    pub height: Option<u32>,
    pub media_source_json: String,
}

#[derive(Debug, Clone, uniffi::Record)]
pub struct ReactionBadge {
    pub key: String,
    pub count: u32,
    pub reacted_by_me: bool,
}

#[derive(Debug, Clone, uniffi::Record)]
pub struct ReplyPreview {
    pub event_id: String,
    pub sender_name: String,
    pub body: String,
}

#[derive(Debug, Clone, PartialEq, Eq, uniffi::Enum)]
pub enum MessageDeliveryStatus {
    Sending,
    Sent,
    Failed { reason: String },
}

#[derive(Debug, Clone, uniffi::Record)]
pub struct TimelineMessage {
    pub event_id: String,
    pub sender_id: String,
    pub sender_name: String,
    pub sender_avatar_mxc: Option<String>,
    pub timestamp_ms: u64,
    pub body: String,
    pub formatted_html: Option<String>,
    pub is_edited: bool,
    pub is_redacted: bool,
    pub is_encrypted: bool,
    pub is_unable_to_decrypt: bool,
    pub is_own: bool,
    pub can_reply: bool,
    pub thread_root_id: Option<String>,
    pub thread_reply_count: u32,
    pub reply_to: Option<ReplyPreview>,
    pub attachment: Option<MediaAttachmentInfo>,
    pub reactions: Vec<ReactionBadge>,
    pub delivery_status: MessageDeliveryStatus,
}

#[derive(Debug, Clone, uniffi::Record)]
pub struct DeviceSummary {
    pub user_id: String,
    pub device_id: String,
    pub display_name: Option<String>,
    pub is_verified: bool,
    pub is_current_device: bool,
}

#[derive(Debug, Clone, uniffi::Record)]
pub struct SecretBackupStatus {
    pub has_master_key: bool,
    pub has_self_signing_key: bool,
    pub has_user_signing_key: bool,
    pub backup_enabled: bool,
    pub backup_exists_on_server: bool,
    pub secret_storage_exists: bool,
}

#[derive(Debug, Clone, uniffi::Record)]
pub struct SasEmojiItem {
    pub symbol: String,
    pub description: String,
}

#[derive(Debug, Clone, PartialEq, Eq, uniffi::Enum)]
pub enum VerificationPhase {
    IncomingRequest {
        flow_id: String,
        other_user_id: String,
        other_device_id: Option<String>,
    },
    Requested {
        flow_id: String,
    },
    Ready {
        flow_id: String,
    },
    SasStarted {
        flow_id: String,
        emojis: Vec<SasEmojiItem>,
    },
    WaitingForOtherParty {
        flow_id: String,
    },
    Confirmed {
        flow_id: String,
    },
    Cancelled {
        flow_id: String,
        reason: String,
    },
}

#[derive(Debug, Clone, uniffi::Record)]
pub struct PushNotificationSummary {
    pub room_id: String,
    pub event_id: String,
    pub room_name: String,
    pub sender_name: String,
    pub body_preview: String,
    pub is_encrypted: bool,
    pub unread_count: u32,
}
