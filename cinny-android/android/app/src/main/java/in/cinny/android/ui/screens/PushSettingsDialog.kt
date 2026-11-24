package `in`.cinny.android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.cinny.android.data.CinnyRepository

@Composable
fun PushSettingsDialog(
    repository: CinnyRepository,
    onDismiss: () -> Unit
) {
    val endpoint by repository.pushEndpoint.collectAsState()
    val savedGateway by repository.pushGatewayUrl.collectAsState()
    val status by repository.pushRegistrationStatus.collectAsState()

    var gatewayInput by remember(savedGateway) { mutableStateOf(savedGateway) }
    var pushKeyInput by remember(endpoint) { mutableStateOf(endpoint ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("UnifiedPush / Push Gateway", fontWeight = FontWeight.SemiBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Register an HTTP Matrix pusher (UnifiedPush or Sygnal gateway) so encrypted messages wake PushSyncWorker in the background.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = gatewayInput,
                    onValueChange = { gatewayInput = it },
                    label = { Text("Push Gateway URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = pushKeyInput,
                    onValueChange = { pushKeyInput = it },
                    label = { Text("UnifiedPush Endpoint / FCM Token") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (!status.isNullOrBlank()) {
                    Text(status!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { repository.registerHttpPusher(pushKeyInput.trim(), gatewayInput.trim()) },
                enabled = pushKeyInput.isNotBlank() && gatewayInput.isNotBlank()
            ) {
                Text("Register Pusher")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
