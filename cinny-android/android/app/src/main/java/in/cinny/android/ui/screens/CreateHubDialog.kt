package `in`.cinny.android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.cinny.android.R

@Composable
fun CreateHubDialog(
    onCreateRoom: (name: String, topic: String?, isDirect: Boolean, isEncrypted: Boolean, inviteIds: List<String>) -> Unit,
    onCreateSpace: (name: String, topic: String?, isPublic: Boolean) -> Unit,
    onJoinRoom: (aliasOrId: String) -> Unit,
    onDismiss: () -> Unit
) {
    var mode by remember { mutableIntStateOf(0) } // 0: Room, 1: Space, 2: Direct, 3: Join
    var name by remember { mutableStateOf("") }
    var topic by remember { mutableStateOf("") }
    var inviteIds by remember { mutableStateOf("") }
    var dmUserId by remember { mutableStateOf("") }
    var encrypted by remember { mutableStateOf(true) }
    var spacePublic by remember { mutableStateOf(false) }
    var joinAlias by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = when (mode) {
                    0 -> stringResource(R.string.create_room_title)
                    1 -> stringResource(R.string.create_space_title)
                    2 -> stringResource(R.string.start_dm_title)
                    else -> stringResource(R.string.join_room_title)
                },
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = mode == 0,
                        onClick = { mode = 0 },
                        label = { Text(stringResource(R.string.tab_room)) }
                    )
                    FilterChip(
                        selected = mode == 1,
                        onClick = { mode = 1 },
                        label = { Text(stringResource(R.string.tab_space)) }
                    )
                    FilterChip(
                        selected = mode == 2,
                        onClick = { mode = 2 },
                        label = { Text(stringResource(R.string.tab_dm)) }
                    )
                    FilterChip(
                        selected = mode == 3,
                        onClick = { mode = 3 },
                        label = { Text(stringResource(R.string.tab_join)) }
                    )
                }

                when (mode) {
                    0 -> {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text(stringResource(R.string.room_name_label)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = topic,
                            onValueChange = { topic = it },
                            label = { Text(stringResource(R.string.topic_optional_label)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = inviteIds,
                            onValueChange = { inviteIds = it },
                            label = { Text("Invite @user:server (comma-separated)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.enable_e2ee_label), style = MaterialTheme.typography.bodyMedium)
                            Switch(checked = encrypted, onCheckedChange = { encrypted = it })
                        }
                    }
                    1 -> {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text(stringResource(R.string.space_name_label)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = topic,
                            onValueChange = { topic = it },
                            label = { Text(stringResource(R.string.topic_optional_label)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.public_space_label), style = MaterialTheme.typography.bodyMedium)
                            Switch(checked = spacePublic, onCheckedChange = { spacePublic = it })
                        }
                    }
                    2 -> {
                        OutlinedTextField(
                            value = dmUserId,
                            onValueChange = { dmUserId = it },
                            label = { Text("@user:matrix.org") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.enable_e2ee_label), style = MaterialTheme.typography.bodyMedium)
                            Switch(checked = encrypted, onCheckedChange = { encrypted = it })
                        }
                    }
                    else -> {
                        OutlinedTextField(
                            value = joinAlias,
                            onValueChange = { joinAlias = it },
                            label = { Text("#alias:matrix.org or !roomId:server") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            val isValid = when (mode) {
                0, 1 -> name.isNotBlank()
                2 -> dmUserId.isNotBlank()
                else -> joinAlias.isNotBlank()
            }
            Button(
                enabled = isValid,
                shape = RoundedCornerShape(8.dp),
                onClick = {
                    when (mode) {
                        0 -> onCreateRoom(
                            name.trim(),
                            topic.trim().ifBlank { null },
                            false,
                            encrypted,
                            inviteIds.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        )
                        1 -> onCreateSpace(name.trim(), topic.trim().ifBlank { null }, spacePublic)
                        2 -> {
                            val cleanUid = dmUserId.trim()
                            val dmLabel = cleanUid.removePrefix("@").substringBefore(":")
                            onCreateRoom(dmLabel, null, true, encrypted, listOf(cleanUid))
                        }
                        else -> onJoinRoom(joinAlias.trim())
                    }
                }
            ) {
                Text(
                    when (mode) {
                        0, 1 -> stringResource(R.string.action_create)
                        2 -> stringResource(R.string.action_start_chat)
                        else -> stringResource(R.string.action_join)
                    }
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}
