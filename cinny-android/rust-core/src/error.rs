use thiserror::Error;

#[derive(Debug, Error, uniffi::Error)]
pub enum CinnyError {
    #[error("Homeserver discovery failed: {message}")]
    HomeserverDiscovery { message: String },

    #[error("Authentication failed: {message}")]
    Authentication { message: String },

    #[error("Session token expired or revoked: {message}")]
    TokenInvalid { message: String },

    #[error("Session store error: {message}")]
    Storage { message: String },

    #[error("Room not found: {room_id}")]
    RoomNotFound { room_id: String },

    #[error("Encryption error: {message}")]
    Crypto { message: String },

    #[error("Network or sync error: {message}")]
    Network { message: String },

    #[error("Invalid input: {message}")]
    InvalidInput { message: String },
}

impl From<matrix_sdk::Error> for CinnyError {
    fn from(err: matrix_sdk::Error) -> Self {
        let msg = err.to_string();
        if msg.contains("M_UNKNOWN_TOKEN") || msg.contains("UnknownToken") {
            Self::TokenInvalid { message: msg }
        } else {
            Self::Network { message: msg }
        }
    }
}

impl From<matrix_sdk::ClientBuildError> for CinnyError {
    fn from(err: matrix_sdk::ClientBuildError) -> Self {
        Self::HomeserverDiscovery {
            message: err.to_string(),
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn error_variants_format_readably() {
        let err = CinnyError::TokenInvalid {
            message: "M_UNKNOWN_TOKEN".to_string(),
        };
        assert!(err.to_string().contains("M_UNKNOWN_TOKEN"));
    }
}
