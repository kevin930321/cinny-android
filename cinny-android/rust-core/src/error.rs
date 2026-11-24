use thiserror::Error;

#[derive(Debug, Error, uniffi::Error)]
pub enum CinnyError {
    #[error("Homeserver discovery failed: {detail}")]
    HomeserverDiscovery { detail: String },

    #[error("Authentication failed: {detail}")]
    Authentication { detail: String },

    #[error("Session token expired or revoked: {detail}")]
    TokenInvalid { detail: String },

    #[error("Session store error: {detail}")]
    Storage { detail: String },

    #[error("Room not found: {room_id}")]
    RoomNotFound { room_id: String },

    #[error("Encryption error: {detail}")]
    Crypto { detail: String },

    #[error("Network or sync error: {detail}")]
    Network { detail: String },

    #[error("Invalid input: {detail}")]
    InvalidInput { detail: String },
}

impl From<matrix_sdk::Error> for CinnyError {
    fn from(err: matrix_sdk::Error) -> Self {
        let msg = err.to_string();
        if msg.contains("M_UNKNOWN_TOKEN") || msg.contains("UnknownToken") {
            Self::TokenInvalid { detail: msg }
        } else {
            Self::Network { detail: msg }
        }
    }
}

impl From<matrix_sdk::ClientBuildError> for CinnyError {
    fn from(err: matrix_sdk::ClientBuildError) -> Self {
        Self::HomeserverDiscovery {
            detail: err.to_string(),
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn error_variants_format_readably() {
        let err = CinnyError::TokenInvalid {
            detail: "M_UNKNOWN_TOKEN".to_string(),
        };
        assert!(err.to_string().contains("M_UNKNOWN_TOKEN"));
    }
}
