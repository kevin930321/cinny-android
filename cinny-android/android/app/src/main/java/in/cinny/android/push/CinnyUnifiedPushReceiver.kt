package `in`.cinny.android.push

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import `in`.cinny.android.CinnyApplication

/**
 * Receives UnifiedPush distributor broadcasts (`NEW_ENDPOINT`, `MESSAGE`, `UNREGISTERED`)
 * and forwards encrypted Matrix push payloads to `PushSyncWorker` for Rust Megolm decryption.
 */
class CinnyUnifiedPushReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as? CinnyApplication
        when (intent.action) {
            "org.unifiedpush.android.connector.NEW_ENDPOINT" -> {
                val endpoint = intent.getStringExtra("endpoint") ?: return
                app?.repository?.onUnifiedPushEndpointUpdated(endpoint)
            }
            "org.unifiedpush.android.connector.MESSAGE" -> {
                val bytes = intent.getByteArrayExtra("message") ?: return
                val payload = String(bytes, Charsets.UTF_8)
                PushSyncWorker.enqueue(context, payload)
            }
            "org.unifiedpush.android.connector.UNREGISTERED" -> {
                app?.repository?.onUnifiedPushEndpointUpdated(null)
            }
        }
    }
}
