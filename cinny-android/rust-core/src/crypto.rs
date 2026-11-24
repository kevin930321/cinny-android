use std::{collections::HashMap, sync::Arc};

use futures_util::StreamExt;
use matrix_sdk::{
    encryption::{
        backups::BackupState,
        recovery::RecoveryState,
        verification::{
            SasState, SasVerification, Verification, VerificationRequest, VerificationRequestState,
        },
    },
    ruma::{
        events::key::verification::request::ToDeviceKeyVerificationRequestEvent,
        OwnedDeviceId, OwnedUserId,
    },
    Client,
};
use tokio::sync::Mutex;

use crate::{
    error::CinnyError,
    models::{DeviceSummary, SasEmojiItem, SecretBackupStatus, VerificationPhase},
};

#[uniffi::export(callback_interface)]
pub trait VerificationListener: Send + Sync {
    fn on_phase_changed(&self, phase: VerificationPhase);
}

#[derive(Default)]
pub struct VerificationRegistry {
    active_sas: Mutex<HashMap<String, SasVerification>>,
    pending_requests: Mutex<HashMap<String, VerificationRequest>>,
    global_listener: Mutex<Option<Arc<dyn VerificationListener>>>,
}

impl VerificationRegistry {
    pub async fn set_global_listener(&self, listener: Arc<dyn VerificationListener>) {
        *self.global_listener.lock().await = Some(listener);
    }

    pub async fn get_sas(&self, flow_id: &str) -> Option<SasVerification> {
        self.active_sas.lock().await.get(flow_id).cloned()
    }

    pub async fn get_pending_request(&self, flow_id: &str) -> Option<VerificationRequest> {
        self.pending_requests.lock().await.get(flow_id).cloned()
    }
}

pub fn register_incoming_verification_handler(
    client: &Client,
    registry: Arc<VerificationRegistry>,
) {
    client.add_event_handler(
        move |ev: ToDeviceKeyVerificationRequestEvent, client: Client| {
            let registry = Arc::clone(&registry);
            async move {
                let flow_id = ev.content.transaction_id.to_string();
                let Some(request) = client
                    .encryption()
                    .get_verification_request(&ev.sender, &flow_id)
                    .await
                else {
                    return;
                };

                registry
                    .pending_requests
                    .lock()
                    .await
                    .insert(flow_id.clone(), request.clone());

                let listener_opt = registry.global_listener.lock().await.clone();
                if let Some(listener) = listener_opt {
                    listener.on_phase_changed(VerificationPhase::IncomingRequest {
                        flow_id: flow_id.clone(),
                        other_user_id: ev.sender.to_string(),
                        other_device_id: Some(ev.content.from_device.to_string()),
                    });
                    spawn_verification_watcher(request, flow_id, listener, registry);
                }
            }
        },
    );
}

pub async fn get_secret_backup_status(client: &Client) -> SecretBackupStatus {
    let encryption = client.encryption();
    let cross_signing = encryption.cross_signing_status().await;
    let backup_state = encryption.backups().state();
    let backup_exists = encryption.backups().exists_on_server().await.unwrap_or(false);
    let recovery_state = encryption.recovery().state();

    SecretBackupStatus {
        has_master_key: cross_signing.as_ref().map(|c| c.has_master).unwrap_or(false),
        has_self_signing_key: cross_signing.as_ref().map(|c| c.has_self_signing).unwrap_or(false),
        has_user_signing_key: cross_signing.as_ref().map(|c| c.has_user_signing).unwrap_or(false),
        backup_enabled: matches!(backup_state, BackupState::Enabled),
        backup_exists_on_server: backup_exists,
        secret_storage_exists: !matches!(recovery_state, RecoveryState::Disabled | RecoveryState::Unknown),
    }
}

/// Unlocks 4S Secret Storage with a Recovery Key / Passphrase, restores cross-signing keys
/// and downloads Megolm room key backups so encrypted message history decrypts.
pub async fn recover_with_security_key(
    client: &Client,
    recovery_key: &str,
) -> Result<SecretBackupStatus, CinnyError> {
    let trimmed = recovery_key.trim();
    if trimmed.is_empty() {
        return Err(CinnyError::InvalidInput {
            detail: "Recovery key cannot be empty".to_string(),
        });
    }

    client
        .encryption()
        .recovery()
        .recover(trimmed)
        .await
        .map_err(|e| CinnyError::Crypto {
            detail: format!("4S secret recovery failed: {e}"),
        })?;

    Ok(get_secret_backup_status(client).await)
}

/// Sets up 4S Secret Storage + Server Key Backup and returns the newly generated Recovery Key.
pub async fn enable_recovery_and_backup(client: &Client) -> Result<String, CinnyError> {
    let recovery_key = client
        .encryption()
        .recovery()
        .enable()
        .await
        .map_err(|e| CinnyError::Crypto {
            detail: format!("Failed to enable 4S backup: {e}"),
        })?;
    Ok(recovery_key)
}

pub async fn list_user_devices(
    client: &Client,
    user_id: &str,
) -> Result<Vec<DeviceSummary>, CinnyError> {
    let parsed_user = OwnedUserId::try_from(user_id).map_err(|e| CinnyError::InvalidInput {
        detail: format!("Invalid Matrix user ID: {e}"),
    })?;
    let own_device_id = client.device_id().map(|d| d.to_string());
    let devices = client
        .encryption()
        .get_user_devices(&parsed_user)
        .await
        .map_err(|e| CinnyError::Crypto {
            detail: format!("Failed to load devices: {e}"),
        })?;

    let mut out = Vec::new();
    for dev in devices.devices() {
        let dev_id = dev.device_id().to_string();
        out.push(DeviceSummary {
            user_id: parsed_user.to_string(),
            device_id: dev_id.clone(),
            display_name: dev.display_name().map(|s| s.to_string()),
            is_verified: dev.is_verified(),
            is_current_device: own_device_id.as_deref() == Some(dev_id.as_str()),
        });
    }
    Ok(out)
}

pub async fn start_sas_verification(
    client: &Client,
    registry: Arc<VerificationRegistry>,
    user_id: &str,
    device_id: &str,
    listener: Arc<dyn VerificationListener>,
) -> Result<String, CinnyError> {
    let parsed_user = OwnedUserId::try_from(user_id).map_err(|e| CinnyError::InvalidInput {
        detail: format!("Invalid Matrix user ID: {e}"),
    })?;
    let parsed_device = OwnedDeviceId::from(device_id);

    let device = client
        .encryption()
        .get_device(&parsed_user, &parsed_device)
        .await
        .map_err(|e| CinnyError::Crypto {
            detail: format!("Device lookup failed: {e}"),
        })?
        .ok_or_else(|| CinnyError::Crypto {
            detail: "Target device not found".to_string(),
        })?;

    let request = device
        .request_verification()
        .await
        .map_err(|e| CinnyError::Crypto {
            detail: format!("Verification request failed: {e}"),
        })?;

    let flow_id = request.flow_id().to_string();
    registry
        .pending_requests
        .lock()
        .await
        .insert(flow_id.clone(), request.clone());

    spawn_verification_watcher(request, flow_id.clone(), listener, registry);
    Ok(flow_id)
}

pub async fn accept_incoming_verification(
    registry: &VerificationRegistry,
    flow_id: &str,
) -> Result<(), CinnyError> {
    let request = registry
        .get_pending_request(flow_id)
        .await
        .ok_or_else(|| CinnyError::Crypto {
            detail: format!("Verification request '{flow_id}' not found"),
        })?;

    request.accept().await.map_err(|e| CinnyError::Crypto {
        detail: format!("Failed to accept verification request: {e}"),
    })?;
    Ok(())
}

pub async fn confirm_sas(registry: &VerificationRegistry, flow_id: &str) -> Result<(), CinnyError> {
    let sas = registry
        .get_sas(flow_id)
        .await
        .ok_or_else(|| CinnyError::Crypto {
            detail: format!("Active SAS session '{flow_id}' not found"),
        })?;

    sas.confirm().await.map_err(|e| CinnyError::Crypto {
        detail: format!("Failed to confirm SAS match: {e}"),
    })?;
    Ok(())
}

pub async fn cancel_sas(registry: &VerificationRegistry, flow_id: &str) -> Result<(), CinnyError> {
    if let Some(sas) = registry.get_sas(flow_id).await {
        sas.cancel().await.map_err(|e| CinnyError::Crypto {
            detail: format!("Failed to cancel SAS verification: {e}"),
        })?;
        return Ok(());
    }
    if let Some(req) = registry.get_pending_request(flow_id).await {
        req.cancel().await.map_err(|e| CinnyError::Crypto {
            detail: format!("Failed to cancel verification request: {e}"),
        })?;
    }
    Ok(())
}

fn spawn_verification_watcher(
    request: VerificationRequest,
    flow_id: String,
    listener: Arc<dyn VerificationListener>,
    registry: Arc<VerificationRegistry>,
) {
    tokio::spawn(async move {
        let mut changes = request.changes();
        while let Some(state) = changes.next().await {
            match state {
                VerificationRequestState::Requested { .. } => {
                    listener.on_phase_changed(VerificationPhase::Requested {
                        flow_id: flow_id.clone(),
                    });
                }
                VerificationRequestState::Ready { .. } => {
                    listener.on_phase_changed(VerificationPhase::Ready {
                        flow_id: flow_id.clone(),
                    });
                    if let Ok(Some(sas)) = request.start_sas().await {
                        registry
                            .active_sas
                            .lock()
                            .await
                            .insert(flow_id.clone(), sas.clone());
                        watch_sas_stream(sas, flow_id.clone(), Arc::clone(&listener)).await;
                    }
                }
                VerificationRequestState::Transitioned { verification } => {
                    if let Verification::SasV1(sas) = verification {
                        let _ = sas.accept().await;
                        registry
                            .active_sas
                            .lock()
                            .await
                            .insert(flow_id.clone(), sas.clone());
                        watch_sas_stream(sas, flow_id.clone(), Arc::clone(&listener)).await;
                    }
                }
                VerificationRequestState::Done => {
                    listener.on_phase_changed(VerificationPhase::Confirmed {
                        flow_id: flow_id.clone(),
                    });
                    break;
                }
                VerificationRequestState::Cancelled(info) => {
                    listener.on_phase_changed(VerificationPhase::Cancelled {
                        flow_id: flow_id.clone(),
                        reason: info.reason().to_string(),
                    });
                    break;
                }
                _ => {}
            }
        }
    });
}

async fn watch_sas_stream(
    sas: SasVerification,
    flow_id: String,
    listener: Arc<dyn VerificationListener>,
) {
    let mut sas_changes = sas.changes();
    while let Some(sas_state) = sas_changes.next().await {
        match sas_state {
            SasState::KeysExchanged { emojis, .. } => {
                let mapped = emojis
                    .map(|e| {
                        e.emojis
                            .iter()
                            .map(|item| SasEmojiItem {
                                symbol: item.symbol.to_string(),
                                description: item.description.to_string(),
                            })
                            .collect()
                    })
                    .unwrap_or_default();
                listener.on_phase_changed(VerificationPhase::SasStarted {
                    flow_id: flow_id.clone(),
                    emojis: mapped,
                });
            }
            SasState::Confirmed => {
                listener.on_phase_changed(VerificationPhase::WaitingForOtherParty {
                    flow_id: flow_id.clone(),
                });
            }
            SasState::Done { .. } => {
                listener.on_phase_changed(VerificationPhase::Confirmed {
                    flow_id: flow_id.clone(),
                });
                break;
            }
            SasState::Cancelled(info) => {
                listener.on_phase_changed(VerificationPhase::Cancelled {
                    flow_id: flow_id.clone(),
                    reason: info.reason().to_string(),
                });
                break;
            }
            _ => {}
        }
    }
}
