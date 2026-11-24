use std::{collections::HashMap, path::PathBuf, sync::Arc};

use futures_util::StreamExt;
use matrix_sdk::{
    authentication::matrix::MatrixSession,
    config::SyncSettings,
    cross_process_lock::CrossProcessLockConfig,
    ruma::api::client::session::get_login_types::v3::LoginType,
    Client, RoomLoadSettings, SessionMeta, SessionTokens,
};
use matrix_sdk_ui::sync_service::{State as SyncServiceState, SyncService};
use tokio::sync::Mutex;

use crate::{
    crypto::{
        accept_incoming_verification, cancel_sas, confirm_sas, enable_recovery_and_backup,
        get_secret_backup_status, list_user_devices, recover_with_security_key,
        register_incoming_verification_handler, start_sas_verification, VerificationListener,
        VerificationRegistry,
    },
    error::CinnyError,
    media::download_media_to_cache,
    models::{
        CinnyAuthSession, DeviceSummary, HomeserverLoginCapabilities, PushNotificationSummary,
        RoomMemberSummary, RoomSummary, SecretBackupStatus, SpaceHierarchyEntry, SpaceNode,
        SsoProviderInfo, SyncConnectionState, TimelineMessage,
    },
    push::{process_push_event, register_http_pusher},
    spaces::{
        accept_invite, browse_space_hierarchy, build_space_hierarchy_from_items, create_room,
        invite_user_to_room, join_room_by_id_or_alias, leave_room, list_room_members, parse_room_id,
    },
    timeline::{
        open_room_timeline, open_thread_timeline, SharedThreadTimelineMap, SharedTimelineMap,
        TimelineListener,
    },
};

#[uniffi::export(callback_interface)]
pub trait SyncObserver: Send + Sync {
    fn on_sync_state(&self, state: SyncConnectionState);
    fn on_spaces_updated(&self, spaces: Vec<SpaceNode>, rooms: Vec<RoomSummary>);
}

#[uniffi::export(async_runtime = "tokio")]
pub async fn discover_login_capabilities(
    homeserver_url: String,
) -> Result<HomeserverLoginCapabilities, CinnyError> {
    let client = Client::builder().homeserver_url(&homeserver_url).build().await?;
    let types = client
        .matrix_auth()
        .get_login_types()
        .await
        .map_err(|e| CinnyError::HomeserverDiscovery {
            message: format!("Failed to fetch login flows: {e}"),
        })?;

    let mut supports_password = false;
    let mut supports_sso = false;
    let mut sso_providers = Vec::new();

    for flow in &types.flows {
        match flow {
            LoginType::Password(_) => supports_password = true,
            LoginType::Sso(sso) => {
                supports_sso = true;
                for idp in &sso.identity_providers {
                    sso_providers.push(SsoProviderInfo {
                        id: idp.id.clone(),
                        name: idp.name.clone(),
                    });
                }
            }
            _ => {}
        }
    }

    Ok(HomeserverLoginCapabilities {
        homeserver_url: client.homeserver().to_string(),
        supports_password,
        supports_sso,
        sso_providers,
    })
}

#[uniffi::export(async_runtime = "tokio")]
pub async fn build_sso_login_url(
    homeserver_url: String,
    redirect_url: String,
    idp_id: Option<String>,
) -> Result<String, CinnyError> {
    let client = Client::builder().homeserver_url(&homeserver_url).build().await?;
    client
        .matrix_auth()
        .get_sso_login_url(&redirect_url, idp_id.as_deref())
        .await
        .map_err(|e| CinnyError::Authentication {
            message: format!("Failed to construct SSO redirect URL: {e}"),
        })
}

#[derive(uniffi::Object)]
pub struct CinnyClient {
    inner: Client,
    data_dir: PathBuf,
    store_subpath: String,
    timelines: SharedTimelineMap,
    thread_timelines: SharedThreadTimelineMap,
    verification_registry: Arc<VerificationRegistry>,
    sync_service: Mutex<Option<Arc<SyncService>>>,
    sync_tasks: Mutex<Vec<tokio::task::JoinHandle<()>>>,
}

fn new_store_subpath() -> String {
    format!(
        "acct_{}",
        std::time::SystemTime::now()
            .duration_since(std::time::UNIX_EPOCH)
            .map(|d| d.as_millis())
            .unwrap_or(0)
    )
}

#[uniffi::export(async_runtime = "tokio")]
impl CinnyClient {
    #[uniffi::constructor]
    pub async fn login_with_password(
        homeserver_url: String,
        username: String,
        password: String,
        sqlite_root_dir: String,
        sqlite_passphrase: Option<String>,
    ) -> Result<Arc<Self>, CinnyError> {
        let store_subpath = new_store_subpath();
        let data_dir = PathBuf::from(&sqlite_root_dir).join(&store_subpath);

        let client = Client::builder()
            .homeserver_url(&homeserver_url)
            .sqlite_store(&data_dir, sqlite_passphrase.as_deref())
            .cross_process_store_config(CrossProcessLockConfig::SingleProcess)
            .build()
            .await?;

        client
            .matrix_auth()
            .login_username(&username, &password)
            .initial_device_display_name("Cinny Android (Rust)")
            .await
            .map_err(|e| CinnyError::Authentication {
                message: e.to_string(),
            })?;

        let verification_registry = Arc::new(VerificationRegistry::default());
        register_incoming_verification_handler(&client, Arc::clone(&verification_registry));

        Ok(Arc::new(Self {
            inner: client,
            data_dir,
            store_subpath,
            timelines: Arc::new(Mutex::new(HashMap::new())),
            thread_timelines: Arc::new(Mutex::new(HashMap::new())),
            verification_registry,
            sync_service: Mutex::new(None),
            sync_tasks: Mutex::new(Vec::new()),
        }))
    }

    #[uniffi::constructor]
    pub async fn login_with_sso_token(
        homeserver_url: String,
        login_token: String,
        sqlite_root_dir: String,
        sqlite_passphrase: Option<String>,
    ) -> Result<Arc<Self>, CinnyError> {
        let store_subpath = new_store_subpath();
        let data_dir = PathBuf::from(&sqlite_root_dir).join(&store_subpath);

        let client = Client::builder()
            .homeserver_url(&homeserver_url)
            .sqlite_store(&data_dir, sqlite_passphrase.as_deref())
            .cross_process_store_config(CrossProcessLockConfig::SingleProcess)
            .build()
            .await?;

        client
            .matrix_auth()
            .login_token(&login_token)
            .initial_device_display_name("Cinny Android (Rust SSO)")
            .await
            .map_err(|e| CinnyError::Authentication {
                message: format!("SSO token exchange failed: {e}"),
            })?;

        let verification_registry = Arc::new(VerificationRegistry::default());
        register_incoming_verification_handler(&client, Arc::clone(&verification_registry));

        Ok(Arc::new(Self {
            inner: client,
            data_dir,
            store_subpath,
            timelines: Arc::new(Mutex::new(HashMap::new())),
            thread_timelines: Arc::new(Mutex::new(HashMap::new())),
            verification_registry,
            sync_service: Mutex::new(None),
            sync_tasks: Mutex::new(Vec::new()),
        }))
    }

    #[uniffi::constructor]
    pub async fn restore_session(
        session: CinnyAuthSession,
        sqlite_root_dir: String,
        sqlite_passphrase: Option<String>,
    ) -> Result<Arc<Self>, CinnyError> {
        let data_dir = PathBuf::from(&sqlite_root_dir).join(&session.sqlite_store_subpath);
        let client = Client::builder()
            .homeserver_url(&session.homeserver_url)
            .sqlite_store(&data_dir, sqlite_passphrase.as_deref())
            .cross_process_store_config(CrossProcessLockConfig::SingleProcess)
            .build()
            .await?;

        let matrix_session = MatrixSession {
            meta: SessionMeta {
                user_id: session.user_id.try_into().map_err(|e| CinnyError::InvalidInput {
                    message: format!("Invalid user ID: {e}"),
                })?,
                device_id: session.device_id.into(),
            },
            tokens: SessionTokens {
                access_token: session.access_token,
                refresh_token: session.refresh_token,
            },
        };

        client
            .matrix_auth()
            .restore_session(matrix_session, RoomLoadSettings::All)
            .await
            .map_err(|e| CinnyError::Storage {
                message: format!("Failed to restore session: {e}"),
            })?;

        let verification_registry = Arc::new(VerificationRegistry::default());
        register_incoming_verification_handler(&client, Arc::clone(&verification_registry));

        Ok(Arc::new(Self {
            inner: client,
            data_dir,
            store_subpath: session.sqlite_store_subpath,
            timelines: Arc::new(Mutex::new(HashMap::new())),
            thread_timelines: Arc::new(Mutex::new(HashMap::new())),
            verification_registry,
            sync_service: Mutex::new(None),
            sync_tasks: Mutex::new(Vec::new()),
        }))
    }

    pub async fn export_session(&self) -> Result<CinnyAuthSession, CinnyError> {
        let session = self
            .inner
            .matrix_auth()
            .session()
            .ok_or_else(|| CinnyError::Authentication {
                message: "No active Matrix session".to_string(),
            })?;

        Ok(CinnyAuthSession {
            homeserver_url: self.inner.homeserver().to_string(),
            user_id: session.meta.user_id.to_string(),
            device_id: session.meta.device_id.to_string(),
            access_token: session.tokens.access_token,
            refresh_token: session.tokens.refresh_token,
            sqlite_store_subpath: self.store_subpath.clone(),
        })
    }

    pub fn current_user_id(&self) -> Option<String> {
        self.inner.user_id().map(|u| u.to_string())
    }

    pub async fn logout(&self) -> Result<(), CinnyError> {
        self.pause_sync().await;
        self.inner
            .matrix_auth()
            .logout()
            .await
            .map_err(|e| CinnyError::Network {
                message: e.to_string(),
            })?;
        Ok(())
    }

    pub async fn supports_sso(&self) -> Result<bool, CinnyError> {
        let types = self
            .inner
            .matrix_auth()
            .get_login_types()
            .await
            .map_err(|e| CinnyError::Network {
                message: e.to_string(),
            })?;

        Ok(types
            .flows
            .iter()
            .any(|flow| matches!(flow, LoginType::Sso(_))))
    }

    pub async fn register_verification_listener(&self, listener: Box<dyn VerificationListener>) {
        self.verification_registry
            .set_global_listener(Arc::from(listener))
            .await;
    }

    /// Starts Matrix synchronization using `SyncService` + `RoomListService`.
    /// Falls back cleanly to standard `sync_stream` if Sliding Sync isn't supported by the homeserver.
    pub async fn start_sync(&self, observer: Box<dyn SyncObserver>) {
        self.pause_sync().await;
        let observer: Arc<dyn SyncObserver> = Arc::from(observer);
        observer.on_sync_state(SyncConnectionState::Syncing);

        let client = self.inner.clone();
        if let Ok(service) = SyncService::builder(client.clone()).build().await {
            let service = Arc::new(service);
            *self.sync_service.lock().await = Some(Arc::clone(&service));

            let mut state_stream = service.state();
            let obs_state = Arc::clone(&observer);
            let state_task = tokio::spawn(async move {
                while let Some(state) = state_stream.next().await {
                    let mapped = match state {
                        SyncServiceState::Idle => SyncConnectionState::Syncing,
                        SyncServiceState::Running => SyncConnectionState::Live,
                        SyncServiceState::Offline => SyncConnectionState::Offline,
                        SyncServiceState::Terminated => SyncConnectionState::Offline,
                        SyncServiceState::Error(_) => SyncConnectionState::Error {
                            message: "SyncService encountered an error".to_string(),
                        },
                    };
                    obs_state.on_sync_state(mapped);
                }
            });

            let room_list_service = service.room_list_service();
            let obs_rooms = Arc::clone(&observer);
            let client_for_rooms = client.clone();
            let room_task = tokio::spawn(async move {
                if let Ok(all_rooms) = room_list_service.all_rooms().await {
                    let (mut stream, _controller) = all_rooms.entries_with_dynamic_adapters(50);
                    let mut current_items = Vec::new();
                    while let Some(diffs) = stream.next().await {
                        for diff in diffs {
                            diff.apply(&mut current_items);
                        }
                        let (spaces, rooms) =
                            build_space_hierarchy_from_items(&client_for_rooms, &current_items)
                                .await;
                        obs_rooms.on_spaces_updated(spaces, rooms);
                    }
                }
            });

            service.start().await;
            let mut guard = self.sync_tasks.lock().await;
            guard.push(state_task);
            guard.push(room_task);
        } else {
            let handle = tokio::spawn(async move {
                let settings = SyncSettings::default();
                let mut sync_stream = Box::pin(client.sync_stream(settings).await);
                while let Some(res) = sync_stream.next().await {
                    match res {
                        Ok(_) => {
                            observer.on_sync_state(SyncConnectionState::Live);
                            let (spaces, rooms) =
                                build_space_hierarchy_from_items(&client, &[]).await;
                            observer.on_spaces_updated(spaces, rooms);
                        }
                        Err(err) => {
                            let msg = err.to_string();
                            if msg.contains("M_UNKNOWN_TOKEN") || msg.contains("UnknownToken") {
                                observer.on_sync_state(SyncConnectionState::TokenInvalid {
                                    message: msg,
                                });
                                break;
                            }
                            observer.on_sync_state(SyncConnectionState::Error { message: msg });
                        }
                    }
                }
            });
            self.sync_tasks.lock().await.push(handle);
        }
    }

    pub async fn pause_sync(&self) {
        if let Some(service) = self.sync_service.lock().await.take() {
            service.stop().await;
        }
        let mut guard = self.sync_tasks.lock().await;
        for task in guard.drain(..) {
            task.abort();
        }
    }

    pub async fn subscribe_room_timeline(
        &self,
        room_id: String,
        listener: Box<dyn TimelineListener>,
    ) -> Result<(), CinnyError> {
        let parsed = parse_room_id(&room_id)?;
        let room = self
            .inner
            .get_room(&parsed)
            .ok_or_else(|| CinnyError::RoomNotFound {
                room_id: room_id.clone(),
            })?;

        let own_user = self
            .inner
            .user_id()
            .map(|u| u.to_string())
            .unwrap_or_default();
        let active = open_room_timeline(room, own_user, listener).await?;
        self.timelines
            .lock()
            .await
            .insert(room_id, Arc::new(active));
        Ok(())
    }

    pub async fn subscribe_thread_timeline(
        &self,
        room_id: String,
        root_event_id: String,
        listener: Box<dyn TimelineListener>,
    ) -> Result<(), CinnyError> {
        let parsed = parse_room_id(&room_id)?;
        let room = self
            .inner
            .get_room(&parsed)
            .ok_or_else(|| CinnyError::RoomNotFound {
                room_id: room_id.clone(),
            })?;

        let own_user = self
            .inner
            .user_id()
            .map(|u| u.to_string())
            .unwrap_or_default();
        let active = open_thread_timeline(room, own_user, root_event_id.clone(), listener).await?;
        let key = format!("{room_id}::{root_event_id}");
        self.thread_timelines
            .lock()
            .await
            .insert(key, Arc::new(active));
        Ok(())
    }

    pub async fn send_thread_reply(
        &self,
        room_id: String,
        root_event_id: String,
        markdown: String,
    ) -> Result<(), CinnyError> {
        let key = format!("{room_id}::{root_event_id}");
        let guard = self.thread_timelines.lock().await;
        let active = guard.get(&key).cloned().ok_or_else(|| CinnyError::RoomNotFound {
            room_id: key,
        })?;
        drop(guard);
        active.send_thread_reply(markdown).await
    }

    pub async fn close_thread_timeline(&self, room_id: String, root_event_id: String) {
        let key = format!("{room_id}::{root_event_id}");
        self.thread_timelines.lock().await.remove(&key);
    }

    pub async fn send_markdown_message(
        &self,
        room_id: String,
        markdown: String,
        reply_to_event_id: Option<String>,
    ) -> Result<(), CinnyError> {
        let timeline = self.get_timeline(&room_id).await?;
        timeline.send_markdown(markdown, reply_to_event_id).await
    }

    pub async fn send_media_attachment(
        &self,
        room_id: String,
        filename: String,
        mime_type: String,
        bytes: Vec<u8>,
    ) -> Result<(), CinnyError> {
        let timeline = self.get_timeline(&room_id).await?;
        timeline.send_attachment(filename, mime_type, bytes).await
    }

    pub async fn download_media_to_cache(
        &self,
        media_source_json: String,
        filename: String,
        cache_dir: String,
    ) -> Result<String, CinnyError> {
        download_media_to_cache(&self.inner, &media_source_json, &filename, &cache_dir).await
    }

    pub async fn edit_markdown_message(
        &self,
        room_id: String,
        event_id: String,
        new_markdown: String,
    ) -> Result<(), CinnyError> {
        let timeline = self.get_timeline(&room_id).await?;
        timeline.edit_markdown(event_id, new_markdown).await
    }

    pub async fn redact_message(
        &self,
        room_id: String,
        event_id: String,
        reason: Option<String>,
    ) -> Result<(), CinnyError> {
        let timeline = self.get_timeline(&room_id).await?;
        timeline.redact_message(event_id, reason).await
    }

    pub async fn toggle_reaction(
        &self,
        room_id: String,
        event_id: String,
        emoji: String,
    ) -> Result<(), CinnyError> {
        let timeline = self.get_timeline(&room_id).await?;
        timeline.toggle_reaction(event_id, emoji).await
    }

    pub async fn paginate_backwards(&self, room_id: String, limit: u16) -> Result<bool, CinnyError> {
        let timeline = self.get_timeline(&room_id).await?;
        timeline.paginate_backwards(limit).await
    }

    pub async fn mark_room_as_read(&self, room_id: String) -> Result<bool, CinnyError> {
        let timeline = self.get_timeline(&room_id).await?;
        timeline.mark_as_read().await
    }

    pub async fn search_room_messages(
        &self,
        room_id: String,
        query: String,
    ) -> Result<Vec<TimelineMessage>, CinnyError> {
        let timeline = self.get_timeline(&room_id).await?;
        Ok(timeline.search_loaded_messages(&query).await)
    }

    pub async fn send_typing(&self, room_id: String, typing: bool) -> Result<(), CinnyError> {
        let parsed = parse_room_id(&room_id)?;
        let room = self
            .inner
            .get_room(&parsed)
            .ok_or_else(|| CinnyError::RoomNotFound { room_id })?;
        room.typing_notice(typing)
            .await
            .map_err(|e| CinnyError::Network {
                message: e.to_string(),
            })
    }

    // --- Spaces & Room Management ---

    pub async fn browse_space_hierarchy(
        &self,
        space_id: String,
    ) -> Result<Vec<SpaceHierarchyEntry>, CinnyError> {
        browse_space_hierarchy(&self.inner, &space_id).await
    }

    pub async fn create_room(
        &self,
        name: String,
        topic: Option<String>,
        is_direct: bool,
        is_encrypted: bool,
        invite_user_ids: Vec<String>,
    ) -> Result<String, CinnyError> {
        create_room(
            &self.inner,
            name,
            topic,
            is_direct,
            is_encrypted,
            invite_user_ids,
        )
        .await
    }

    pub async fn join_room(&self, room_id_or_alias: String) -> Result<String, CinnyError> {
        join_room_by_id_or_alias(&self.inner, &room_id_or_alias).await
    }

    pub async fn accept_invite(&self, room_id: String) -> Result<(), CinnyError> {
        accept_invite(&self.inner, &room_id).await
    }

    pub async fn leave_room(&self, room_id: String) -> Result<(), CinnyError> {
        leave_room(&self.inner, &room_id).await
    }

    pub async fn invite_user(&self, room_id: String, user_id: String) -> Result<(), CinnyError> {
        invite_user_to_room(&self.inner, &room_id, &user_id).await
    }

    pub async fn list_room_members(
        &self,
        room_id: String,
    ) -> Result<Vec<RoomMemberSummary>, CinnyError> {
        list_room_members(&self.inner, &room_id).await
    }

    // --- E2EE, Cross-Signing, 4S Backup & SAS ---

    pub async fn get_secret_backup_status(&self) -> SecretBackupStatus {
        get_secret_backup_status(&self.inner).await
    }

    pub async fn recover_with_security_key(
        &self,
        recovery_key: String,
    ) -> Result<SecretBackupStatus, CinnyError> {
        recover_with_security_key(&self.inner, &recovery_key).await
    }

    pub async fn enable_recovery_and_backup(&self) -> Result<String, CinnyError> {
        enable_recovery_and_backup(&self.inner).await
    }

    pub async fn list_own_devices(&self) -> Result<Vec<DeviceSummary>, CinnyError> {
        let user_id = self
            .inner
            .user_id()
            .map(|u| u.to_string())
            .ok_or_else(|| CinnyError::Authentication {
                message: "No active user session".to_string(),
            })?;
        list_user_devices(&self.inner, &user_id).await
    }

    pub async fn verify_device_sas(
        &self,
        user_id: String,
        device_id: String,
        listener: Box<dyn VerificationListener>,
    ) -> Result<String, CinnyError> {
        start_sas_verification(
            &self.inner,
            Arc::clone(&self.verification_registry),
            &user_id,
            &device_id,
            Arc::from(listener),
        )
        .await
    }

    pub async fn accept_verification_request(&self, flow_id: String) -> Result<(), CinnyError> {
        accept_incoming_verification(&self.verification_registry, &flow_id).await
    }

    pub async fn confirm_sas_verification(&self, flow_id: String) -> Result<(), CinnyError> {
        confirm_sas(&self.verification_registry, &flow_id).await
    }

    pub async fn cancel_sas_verification(&self, flow_id: String) -> Result<(), CinnyError> {
        cancel_sas(&self.verification_registry, &flow_id).await
    }

    // --- UnifiedPush / Sygnal Push Notifications ---

    pub async fn register_http_pusher(
        &self,
        push_key: String,
        gateway_url: String,
        app_id: String,
        app_display_name: String,
        device_display_name: String,
    ) -> Result<(), CinnyError> {
        register_http_pusher(
            &self.inner,
            push_key,
            gateway_url,
            app_id,
            app_display_name,
            device_display_name,
        )
        .await
    }

    pub async fn process_push_event(
        &self,
        room_id: String,
        event_id: String,
    ) -> Result<PushNotificationSummary, CinnyError> {
        process_push_event(&self.inner, room_id, event_id).await
    }

    pub fn data_directory(&self) -> String {
        self.data_dir.to_string_lossy().to_string()
    }

    async fn get_timeline(
        &self,
        room_id: &str,
    ) -> Result<Arc<crate::timeline::ActiveRoomTimeline>, CinnyError> {
        let guard = self.timelines.lock().await;
        guard
            .get(room_id)
            .cloned()
            .ok_or_else(|| CinnyError::RoomNotFound {
                room_id: room_id.to_string(),
            })
    }
}
