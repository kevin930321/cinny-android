package `in`.cinny.android.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.cinny.android.data.CinnyRepository
import uniffi.cinny_core.VerificationPhase

@Composable
fun CryptoVerifyDialog(
    repository: CinnyRepository,
    phase: VerificationPhase?,
    onDismiss: () -> Unit
) {
    val devices by repository.devices.collectAsState()
    val backupStatus by repository.secretBackupStatus.collectAsState()
    val generatedKey by repository.generatedRecoveryKey.collectAsState()
    val cryptoNotice by repository.cryptoNotice.collectAsState()
    var activeTab by remember { mutableIntStateOf(0) }
    var recoveryKeyInput by remember { mutableStateOf("") }

    LaunchedEffect(phase) {
        if (phase != null) activeTab = 0
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("E2EE Security & Key Backup", fontWeight = FontWeight.SemiBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (phase == null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = activeTab == 0, onClick = { activeTab = 0 }, label = { Text("SAS Devices") })
                        FilterChip(selected = activeTab == 1, onClick = { activeTab = 1 }, label = { Text("4S Recovery") })
                    }
                }

                if (activeTab == 1 && phase == null) {
                    Text(
                        text = buildString {
                            append("Cross-Signing Master: ")
                            append(if (backupStatus?.hasMasterKey == true) "Ready" else "Missing")
                            append("\nRoom Key Backup: ")
                            append(if (backupStatus?.backupEnabled == true) "Active" else "Locked / Off")
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = recoveryKeyInput,
                        onValueChange = { recoveryKeyInput = it },
                        label = { Text("Enter 4S Recovery Key / Passphrase") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { repository.recoverWithSecurityKey(recoveryKeyInput) },
                            enabled = recoveryKeyInput.isNotBlank()
                        ) {
                            Text("Unlock Backup")
                        }
                        OutlinedButton(onClick = { repository.enableRecoveryAndBackup() }) {
                            Text("Create Key")
                        }
                    }
                    if (!generatedKey.isNullOrBlank()) {
                        Text(
                            text = "Save this Recovery Key safely:\n$generatedKey",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (!cryptoNotice.isNullOrBlank()) {
                        Text(cryptoNotice!!, style = MaterialTheme.typography.bodySmall)
                    }
                } else {
                    when (phase) {
                        null -> {
                            val unverified = devices.filter { !it.isCurrentDevice && !it.isVerified }
                            if (unverified.isEmpty()) {
                                Text("All of your other Matrix devices are verified, or no other sessions were found.")
                            } else {
                                Text("Select one of your unverified sessions to verify:")
                                LazyColumn(modifier = Modifier.heightIn(max = 220.dp)) {
                                    items(unverified, key = { it.deviceId }) { dev ->
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    repository.startSasVerification(dev.userId, dev.deviceId)
                                                }
                                                .padding(vertical = 8.dp)
                                        ) {
                                            Text(dev.displayName ?: dev.deviceId, fontWeight = FontWeight.Medium)
                                            Text(
                                                dev.deviceId,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        HorizontalDivider()
                                    }
                                }
                            }
                        }
                        is VerificationPhase.IncomingRequest -> {
                            Text("${phase.otherUserId} (${phase.otherDeviceId ?: "unknown device"}) wants to verify keys.")
                        }
                        is VerificationPhase.SasStarted -> {
                            Text("Confirm these 7 emojis match on the other device:")
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                phase.emojis.forEach { item ->
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(item.symbol, style = MaterialTheme.typography.headlineSmall)
                                        Text(item.description, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                        is VerificationPhase.WaitingForOtherParty -> {
                            Text("Confirmed on this device. Waiting for the other device…")
                        }
                        is VerificationPhase.Confirmed -> {
                            Text("Device cross-signing verified via Rust matrix-sdk-crypto.")
                        }
                        is VerificationPhase.Cancelled -> {
                            Text("Verification cancelled: ${phase.reason}")
                        }
                        else -> {
                            Text("Negotiating Olm/Megolm keys…")
                        }
                    }
                }
            }
        },
        confirmButton = {
            when (phase) {
                is VerificationPhase.IncomingRequest -> {
                    Button(onClick = { repository.acceptIncomingVerification(phase.flowId) }) {
                        Text("Accept")
                    }
                }
                is VerificationPhase.SasStarted -> {
                    Button(onClick = { repository.confirmSas(phase.flowId) }) {
                        Text("They Match")
                    }
                }
                else -> {
                    Button(onClick = onDismiss) { Text("Done") }
                }
            }
        },
        dismissButton = {
            when (phase) {
                is VerificationPhase.IncomingRequest -> {
                    TextButton(onClick = { repository.cancelSas(phase.flowId) }) { Text("Decline") }
                }
                is VerificationPhase.SasStarted -> {
                    TextButton(onClick = { repository.cancelSas(phase.flowId) }) { Text("They Do Not Match") }
                }
                else -> {
                    TextButton(onClick = onDismiss) { Text("Close") }
                }
            }
        }
    )
}
