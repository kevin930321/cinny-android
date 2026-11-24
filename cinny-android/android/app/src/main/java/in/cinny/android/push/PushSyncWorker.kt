package `in`.cinny.android.push

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import `in`.cinny.android.data.SecureSessionStore
import org.json.JSONObject
import uniffi.cinny_core.CinnyClient
import java.io.File

class PushSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val rawPayload = inputData.getString(KEY_PAYLOAD) ?: return Result.failure()
        val store = SecureSessionStore(applicationContext)
        val savedSession = store.loadSession() ?: return Result.success()

        return try {
            val json = JSONObject(rawPayload)
            val notificationObj = json.optJSONObject("notification") ?: json
            val roomId = notificationObj.optString("room_id", "")
            val eventId = notificationObj.optString("event_id", "")
            if (roomId.isBlank() || eventId.isBlank()) {
                return Result.success()
            }

            val sqliteRoot = File(applicationContext.filesDir, "matrix_stores").apply { mkdirs() }
            val passphrase = store.getOrCreateSqlitePassphrase()
            val client = CinnyClient.restoreSession(
                session = savedSession,
                sqliteRootDir = sqliteRoot.absolutePath,
                sqlitePassphrase = passphrase
            )
            val summary = client.processPushEvent(roomId = roomId, eventId = eventId)
            PushNotificationHelper.showMessageNotification(applicationContext, summary)
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val KEY_PAYLOAD = "push_payload"

        fun enqueue(context: Context, payload: String) {
            val data = Data.Builder()
                .putString(KEY_PAYLOAD, payload)
                .build()
            val request = OneTimeWorkRequestBuilder<PushSyncWorker>()
                .setInputData(data)
                .build()
            WorkManager.getInstance(context).enqueue(request)
        }
    }
}
