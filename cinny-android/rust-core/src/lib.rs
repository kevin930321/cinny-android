pub mod client;
pub mod crypto;
pub mod error;
pub mod media;
pub mod models;
pub mod push;
pub mod spaces;
pub mod timeline;

use std::sync::Once;

uniffi::setup_scaffolding!();

static INIT_LOGGER: Once = Once::new();

#[uniffi::export]
pub fn init_cinny_logging() {
    INIT_LOGGER.call_once(|| {
        #[cfg(target_os = "android")]
        {
            use tracing_subscriber::layer::SubscriberExt;
            use tracing_subscriber::util::SubscriberInitExt;
            let android_layer = tracing_android::layer("CinnyRustCore").unwrap();
            let _ = tracing_subscriber::registry().with(android_layer).try_init();
        }
        #[cfg(not(target_os = "android"))]
        {
            let _ = tracing_subscriber::fmt::try_init();
        }
    });
}
