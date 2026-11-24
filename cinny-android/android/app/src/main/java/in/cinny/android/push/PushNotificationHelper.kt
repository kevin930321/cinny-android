package `in`.cinny.android.push

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import `in`.cinny.android.MainActivity
import `in`.cinny.android.R
import uniffi.cinny_core.PushNotificationSummary

object PushNotificationHelper {
    const val CHANNEL_MESSAGES = "cinny_matrix_messages"
    const val EXTRA_ROOM_ID = "extra_room_id"

    fun ensureNotificationChannel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_MESSAGES,
            context.getString(R.string.notification_channel_messages),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notification_channel_messages_desc)
        }
        manager.createNotificationChannel(channel)
    }

    fun showMessageNotification(context: Context, summary: PushNotificationSummary) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        ensureNotificationChannel(context)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_ROOM_ID, summary.roomId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            summary.roomId.hashCode(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (summary.isEncrypted) {
            "${summary.roomName} · E2EE"
        } else {
            summary.roomName
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_MESSAGES)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText("${summary.senderName}: ${summary.bodyPreview}")
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(
            summary.roomId.hashCode(),
            notification
        )
    }
}
