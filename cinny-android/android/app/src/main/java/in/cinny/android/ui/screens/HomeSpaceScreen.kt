package `in`.cinny.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import `in`.cinny.android.R
import `in`.cinny.android.data.CinnyRepository
import `in`.cinny.android.ui.CinnyViewModel
import uniffi.cinny_core.RoomCategory
import uniffi.cinny_core.RoomSummary
import uniffi.cinny_core.SyncConnectionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeSpaceScreen(
    repository: CinnyRepository,
    onOpenVerify: () -> Unit,
    onOpenPushSettings: () -> Unit
) {
    val spaces by repository.spaces.collectAsState()
    val rooms by repository.rooms.collectAsState()
    val selectedSpaceId by repository.selectedSpaceId.collectAsState()
    val spaceHierarchy by repository.spaceHierarchy.collectAsState()
    val syncState by repository.syncState.collectAsState()
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showCreateOrJoinDialog by rememberSaveable { mutableStateOf(false) }
    var showExploreSpaceDialog by rememberSaveable { mutableStateOf(false) }

    val filteredRooms = remember(rooms, selectedSpaceId, searchQuery) {
        CinnyViewModel.filterRooms(rooms, selectedSpaceId, searchQuery)
    }
    val subSpaces = remember(spaces, selectedSpaceId) {
        if (selectedSpaceId != null && selectedSpaceId != "dm") {
            CinnyViewModel.childSpacesFor(spaces, selectedSpaceId)
        } else {
            emptyList()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val activeSpaceName = spaces.find { it.roomId == selectedSpaceId }?.name
                        ?: if (selectedSpaceId == "dm") "Direct Messages" else "Home"
                    Column {
                        Text(activeSpaceName, fontWeight = FontWeight.SemiBold)
                        val syncText = when (syncState) {
                            is SyncConnectionState.Live -> stringResource(R.string.sync_live)
                            is SyncConnectionState.Syncing,
                            is SyncConnectionState.CatchingUp -> stringResource(R.string.sync_syncing)
                            is SyncConnectionState.Offline -> stringResource(R.string.sync_offline)
                            is SyncConnectionState.TokenInvalid -> stringResource(R.string.sync_token_invalid)
                            is SyncConnectionState.Error -> (syncState as SyncConnectionState.Error).message
                        }
                        Text(
                            text = syncText,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (syncState is SyncConnectionState.Error || syncState is SyncConnectionState.TokenInvalid) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                actions = {
                    if (selectedSpaceId != null && selectedSpaceId != "dm") {
                        IconButton(onClick = {
                            repository.loadSpaceHierarchy(selectedSpaceId!!)
                            showExploreSpaceDialog = true
                        }) {
                            Icon(Icons.Outlined.Explore, contentDescription = "Explore Space")
                        }
                    }
                    IconButton(onClick = { showCreateOrJoinDialog = true }) {
                        Icon(Icons.Outlined.Add, contentDescription = "New or Join Room")
                    }
                    IconButton(onClick = onOpenPushSettings) {
                        Icon(Icons.Outlined.Notifications, contentDescription = "Push Settings")
                    }
                    IconButton(onClick = onOpenVerify) {
                        Icon(Icons.Outlined.VerifiedUser, contentDescription = "Security Center")
                    }
                    IconButton(onClick = { repository.cycleThemePreference() }) {
                        Icon(Icons.Outlined.DarkMode, contentDescription = "Cycle Theme")
                    }
                    IconButton(onClick = { repository.logout() }) {
                        Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = "Sign out")
                    }
                }
            )
        }
    ) { padding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Left Cinny Space Rail (64dp)
            Column(
                modifier = Modifier
                    .width(64.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SpaceRailTile(
                    label = "All",
                    selected = selectedSpaceId == null,
                    unread = 0u,
                    onClick = { repository.selectSpace(null) }
                )
                SpaceRailTile(
                    label = "DM",
                    selected = selectedSpaceId == "dm",
                    unread = 0u,
                    onClick = { repository.selectSpace("dm") }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp))
                spaces.filter { it.parentSpaceIds.isEmpty() }.forEach { space ->
                    SpaceRailTile(
                        label = space.name.take(2).uppercase(),
                        selected = selectedSpaceId == space.roomId,
                        unread = space.unreadNotifications,
                        onClick = { repository.selectSpace(space.roomId) }
                    )
                }
            }

            // Room List Column
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(horizontal = 12.dp)
            ) {
                if (subSpaces.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        items(subSpaces, key = { it.roomId }) { sub ->
                            AssistChip(
                                onClick = { repository.selectSpace(sub.roomId) },
                                label = { Text(sub.name) }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(stringResource(R.string.search_rooms_hint)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredRooms, key = { it.roomId }) { room ->
                        RoomListRow(
                            room = room,
                            onClick = { repository.openRoom(room) },
                            onAcceptInvite = { repository.acceptInvite(room.roomId) },
                            onDeclineInvite = { repository.leaveRoom(room.roomId) }
                        )
                    }
                }
            }
        }
    }

    if (showCreateOrJoinDialog) {
        CreateOrJoinRoomDialog(
            onCreate = { name, topic, encrypted, invite ->
                repository.createRoom(
                    name = name,
                    topic = topic.ifBlank { null },
                    isDirect = false,
                    isEncrypted = encrypted,
                    inviteIds = invite.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                )
                showCreateOrJoinDialog = false
            },
            onJoin = { aliasOrId ->
                repository.joinRoom(aliasOrId)
                showCreateOrJoinDialog = false
            },
            onDismiss = { showCreateOrJoinDialog = false }
        )
    }

    if (showExploreSpaceDialog && selectedSpaceId != null) {
        AlertDialog(
            onDismissRequest = { showExploreSpaceDialog = false },
            title = { Text("Discover Space Rooms", fontWeight = FontWeight.SemiBold) },
            text = {
                if (spaceHierarchy.isEmpty()) {
                    Text("Loading rooms in Space…")
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        items(spaceHierarchy, key = { it.roomId }) { entry ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(entry.name, fontWeight = FontWeight.Medium)
                                    Text(
                                        "${entry.numJoinedMembers} members",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (entry.isJoined) {
                                    Text("Joined", style = MaterialTheme.typography.labelMedium)
                                } else {
                                    TextButton(onClick = { repository.joinRoom(entry.roomId) }) {
                                        Text("Join")
                                    }
                                }
                            }
                            HorizontalDivider()
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showExploreSpaceDialog = false }) { Text("Close") }
            }
        )
    }
}

@Composable
private fun CreateOrJoinRoomDialog(
    onCreate: (String, String, Boolean, String) -> Unit,
    onJoin: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var mode by remember { mutableIntStateOf(0) }
    var roomName by remember { mutableStateOf("") }
    var roomTopic by remember { mutableStateOf("") }
    var inviteIds by remember { mutableStateOf("") }
    var encrypted by remember { mutableStateOf(true) }
    var joinAlias by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (mode == 0) "Create Matrix Room" else "Join Room or Alias") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = mode == 0, onClick = { mode = 0 }, label = { Text("Create") })
                    FilterChip(selected = mode == 1, onClick = { mode = 1 }, label = { Text("Join") })
                }
                if (mode == 0) {
                    OutlinedTextField(value = roomName, onValueChange = { roomName = it }, label = { Text("Room Name") }, singleLine = true)
                    OutlinedTextField(value = roomTopic, onValueChange = { roomTopic = it }, label = { Text("Topic (optional)") }, singleLine = true)
                    OutlinedTextField(value = inviteIds, onValueChange = { inviteIds = it }, label = { Text("Invite @user:server (comma-separated)") }, singleLine = true)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Enable Megolm E2EE")
                        Switch(checked = encrypted, onCheckedChange = { encrypted = it })
                    }
                } else {
                    OutlinedTextField(
                        value = joinAlias,
                        onValueChange = { joinAlias = it },
                        label = { Text("#alias:matrix.org or !roomId:server") },
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (mode == 0 && roomName.isNotBlank()) {
                        onCreate(roomName.trim(), roomTopic.trim(), encrypted, inviteIds)
                    } else if (mode == 1 && joinAlias.isNotBlank()) {
                        onJoin(joinAlias.trim())
                    }
                }
            ) {
                Text(if (mode == 0) "Create" else "Join")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun SpaceRailTile(
    label: String,
    selected: Boolean,
    unread: UInt,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(if (selected) 12.dp else 22.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surface
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurface
        )
        if (unread > 0u) {
            Text(
                text = unread.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(2.dp)
            )
        }
    }
}

@Composable
private fun RoomListRow(
    room: RoomSummary,
    onClick: () -> Unit,
    onAcceptInvite: () -> Unit,
    onDeclineInvite: () -> Unit
) {
    val subtitle = room.lastMessagePreview ?: room.topic
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(enabled = room.category != RoomCategory.INVITED, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (room.isEncrypted) Icons.Outlined.Lock else Icons.Outlined.Tag,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = room.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (room.unreadCount > 0u) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (room.category == RoomCategory.INVITED) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onAcceptInvite) { Text("Join") }
                TextButton(onClick = onDeclineInvite) { Text("Decline") }
            }
        } else if (room.unreadCount > 0u) {
            Text(
                text = room.unreadCount.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
