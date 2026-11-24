use std::path::{Path, PathBuf};

use matrix_sdk::{
    attachment::AttachmentConfig,
    media::{MediaFormat, MediaRequestParameters},
    room::Room,
    ruma::events::room::{
        message::{FileMessageEventContent, ImageMessageEventContent, MessageType},
        MediaSource,
    },
    Client,
};

use crate::{error::CinnyError, models::MediaAttachmentInfo};

/// Sends an image or binary file to the room.
/// When the room has Megolm encryption enabled, `matrix-sdk` automatically encrypts the payload
/// with AES-CTR + SHA-256 and attaches an `EncryptedFile` (`MediaSource::Encrypted`).
pub async fn send_media_attachment(
    room: &Room,
    filename: String,
    mime_type: String,
    bytes: Vec<u8>,
) -> Result<(), CinnyError> {
    let parsed_mime: mime::Mime = mime_type.parse().unwrap_or(mime::APPLICATION_OCTET_STREAM);
    let safe_name = sanitize_cache_filename(&filename);
    let config = AttachmentConfig::new();
    room.send_attachment(&safe_name, &parsed_mime, bytes, config)
        .await
        .map_err(|e| CinnyError::Network {
            message: format!("Attachment upload failed: {e}"),
        })?;
    Ok(())
}

/// Downloads and (if E2EE) decrypts `mxc://` media bytes to `<cache_dir>/media_cache/`.
/// Returns the verified local file path for Android `BitmapFactory` or `FileProvider`.
pub async fn download_media_to_cache(
    client: &Client,
    media_source_json: &str,
    filename: &str,
    cache_dir: &str,
) -> Result<String, CinnyError> {
    let source: MediaSource =
        serde_json::from_str(media_source_json).map_err(|e| CinnyError::InvalidInput {
            message: format!("Invalid MediaSource JSON: {e}"),
        })?;

    let media_dir = PathBuf::from(cache_dir).join("media_cache");
    std::fs::create_dir_all(&media_dir).map_err(|e| CinnyError::Storage {
        message: format!("Failed to create media cache directory: {e}"),
    })?;

    let hash_prefix = fnv1a_hex(media_source_json.as_bytes());
    let safe_name = sanitize_cache_filename(filename);
    let target_path = media_dir.join(format!("{hash_prefix}_{safe_name}"));

    if target_path.exists() {
        return Ok(target_path.to_string_lossy().to_string());
    }

    let request = MediaRequestParameters {
        source,
        format: MediaFormat::File,
    };
    let bytes = client
        .media()
        .get_media_content(&request, true)
        .await
        .map_err(|e| CinnyError::Network {
            message: format!("Failed to fetch or decrypt media: {e}"),
        })?;

    std::fs::write(&target_path, &bytes).map_err(|e| CinnyError::Storage {
        message: format!("Failed to write cached media file: {e}"),
    })?;

    Ok(target_path.to_string_lossy().to_string())
}

pub fn extract_attachment_info(msgtype: &MessageType) -> Option<MediaAttachmentInfo> {
    match msgtype {
        MessageType::Image(ImageMessageEventContent {
            body, source, info, ..
        }) => {
            let source_json = serde_json::to_string(source).ok()?;
            let (mxc_url, is_encrypted) = match source {
                MediaSource::Plain(uri) => (uri.to_string(), false),
                MediaSource::Encrypted(file) => (file.url.to_string(), true),
            };
            Some(MediaAttachmentInfo {
                mxc_url,
                filename: body.clone(),
                mime_type: info
                    .as_ref()
                    .and_then(|i| i.mimetype.clone())
                    .unwrap_or_else(|| "image/*".to_string()),
                size_bytes: info.as_ref().and_then(|i| i.size).map(|s| s.into()).unwrap_or(0),
                is_encrypted,
                width: info.as_ref().and_then(|i| i.width).map(|w| u64::from(w) as u32),
                height: info.as_ref().and_then(|i| i.height).map(|h| u64::from(h) as u32),
                media_source_json: source_json,
            })
        }
        MessageType::File(FileMessageEventContent {
            body, source, info, ..
        }) => {
            let source_json = serde_json::to_string(source).ok()?;
            let (mxc_url, is_encrypted) = match source {
                MediaSource::Plain(uri) => (uri.to_string(), false),
                MediaSource::Encrypted(file) => (file.url.to_string(), true),
            };
            Some(MediaAttachmentInfo {
                mxc_url,
                filename: body.clone(),
                mime_type: info
                    .as_ref()
                    .and_then(|i| i.mimetype.clone())
                    .unwrap_or_else(|| "application/octet-stream".to_string()),
                size_bytes: info.as_ref().and_then(|i| i.size).map(|s| s.into()).unwrap_or(0),
                is_encrypted,
                width: None,
                height: None,
                media_source_json: source_json,
            })
        }
        _ => None,
    }
}

pub fn sanitize_cache_filename(raw: &str) -> String {
    let base = Path::new(raw)
        .file_name()
        .and_then(|n| n.to_str())
        .unwrap_or("attachment.bin");
    let cleaned: String = base
        .chars()
        .map(|c| {
            if c.is_ascii_alphanumeric() || c == '.' || c == '-' || c == '_' {
                c
            } else {
                '_'
            }
        })
        .collect();
    if cleaned.is_empty() {
        "attachment.bin".to_string()
    } else {
        cleaned
    }
}

fn fnv1a_hex(data: &[u8]) -> String {
    let mut hash: u64 = 0xcbf29ce484222325;
    for byte in data {
        hash ^= *byte as u64;
        hash = hash.wrapping_mul(0x100000001b3);
    }
    format!("{hash:016x}")
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn sanitize_cache_filename_strips_traversal_and_unsafe_chars() {
        assert_eq!(sanitize_cache_filename("../../etc/passwd"), "passwd");
        assert_eq!(sanitize_cache_filename("photo 2026!.png"), "photo_2026_.png");
        assert_eq!(sanitize_cache_filename(""), "attachment.bin");
    }
}
