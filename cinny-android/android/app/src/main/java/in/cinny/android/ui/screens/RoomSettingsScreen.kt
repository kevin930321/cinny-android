package `in`.cinny.android.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import `in`.cinny.android.R
import `in`.cinny.android.data.CinnyRepository
import `in`.cinny.android.ui.theme.CinnyThemeColors
import `in`.cinny.android.ui.theme.colorMXID
import uniffi.cinny_core.RoomNotificationLevel
import uniffi.cinny_core.RoomSummary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomSettingsScreen(
    room: RoomSummary,
    repository: CinnyRepository,
    onBack: () -> Unit
) {
    val colors = CinnyThemeColors.current
    val clipboard = LocalClipboardManager.current
    val settings by repository.settings.collectAsState()
    val members by repository.roomMembers.collectAsState()
    val notifLevel by repository.activeRoomNotificationLevel.collectAsState()
    val mxcCachePaths by repository.mxcCachePaths.collectAsState()
    val actionNotice by repository.actionNotice.collectAsState()

    var activeTab by rememberSaveable { mutableIntStateOf(0) } // 0: General, 1: Members, 2: Developer
    var roomNameDraft by remember(room.roomId, room.name) { mutableStateOf(room.name) }
    var roomTopicDraft by remember(room.roomId, room.topic) { mutableStateOf(room.topic ?: "") }
    var inviteUserId by remember { mutableStateOf("") }
    var memberSearch by remember { mutableStateOf("") }
    var showLeaveConfirm by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(room.roomId) {
        repository.loadRoomSettingsData(room.roomId)
    }

    val filteredMembers = remember(members, memberSearch) {
        val q = memberSearch.trim()
        if (q.isEmpty()) members else members.filter {
            it.displayName.contains(q, ignoreCase = true) ||
                it.userId.contains(q, ignoreCase = true)
        }
    }

    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.surface,
                    titleContentColor = colors.onSurface
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                title = {
                    Column {
                        Text(room.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            text = stringResource(R.string.room_settings_title),
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceMuted
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(
                selectedTabIndex = activeTab,
                containerColor = colors.surface
            ) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = { Text(stringResource(R.string.settings_general)) }
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = { Text("${stringResource(R.string.room_settings_members)} (${members.size})") }
                )
                if (settings.developerTools) {
                    Tab(
                        selected = activeTab == 2,
                        onClick = { activeTab = 2 },
                        text = { Text("Dev") }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                if (!actionNotice.isNullOrBlank()) {
                    item {
                        Surface(
                            color = colors.successContainer,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(actionNotice!!, style = MaterialTheme.typography.bodySmall, color = colors.success)
                                TextButton(onClick = { repository.clearActionNotice() }) {
                                    Text("OK", color = colors.success)
                                }
                            }
                        }
                    }
                }

                when (activeTab) {
                    0 -> {
                        item {
                            CinnySequenceCard {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        MxcAvatar(
                                            name = room.name,
                                            seedId = room.roomId,
                                            mxcUrl = room.avatarMxc,
                                            mxcCachePaths = mxcCachePaths,
                                            onRequestMxc = { repository.requestMxcThumbnail(it, 128) },
                                            size = 54.dp,
                                            cornerRadius = if (room.isDirect) 27.dp else 14.dp
                                        )
                                        Column {
                                            Text(room.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    if (room.isEncrypted) Icons.Outlined.Lock else Icons.Outlined.LockOpen,
                                                    contentDescription = null,
                                                    tint = if (room.isEncrypted) colors.success else colors.onSurfaceMuted,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Text(
                                                    text = if (room.isEncrypted) "Megolm E2EE Enabled" else "Unencrypted Room",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (room.isEncrypted) colors.success else colors.onSurfaceMuted
                                                )
                                            }
                                        }
                                    }

                                    OutlinedTextField(
                                        value = roomNameDraft,
                                        onValueChange = { roomNameDraft = it },
                                        label = { Text(stringResource(R.string.room_name_label)) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Button(
                                        enabled = roomNameDraft.isNotBlank() && roomNameDraft.trim() != room.name,
                                        shape = RoundedCornerShape(8.dp),
                                        onClick = { repository.updateRoomName(room.roomId, roomNameDraft) }
                                    ) {
                                        Text(stringResource(R.string.action_save))
                                    }

                                    OutlinedTextField(
                                        value = roomTopicDraft,
                                        onValueChange = { roomTopicDraft = it },
                                        label = { Text(stringResource(R.string.topic_optional_label)) },
                                        maxLines = 3,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedButton(
                                        enabled = roomTopicDraft.trim() != (room.topic ?: ""),
                                        shape = RoundedCornerShape(8.dp),
                                        onClick = { repository.updateRoomTopic(room.roomId, roomTopicDraft) }
                                    ) {
                                        Text(stringResource(R.string.room_settings_update_topic))
                                    }
                                }
                            }
                        }

                        item {
                            CinnySectionTitle(stringResource(R.string.settings_notifications))
                            CinnySequenceCard {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(
                                        RoomNotificationLevel.ALL_MESSAGES to "All Messages",
                                        RoomNotificationLevel.MENTIONS_AND_KEYWORDS_ONLY to "Mentions & Keywords",
                                        RoomNotificationLevel.MUTE to "Mute"
                                    ).forEach { (level, label) ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { repository.setRoomNotificationLevel(room.roomId, level) }
                                                .padding(vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(label, style = MaterialTheme.typography.bodyMedium)
                                            RadioButton(
                                                selected = notifLevel == level,
                                                onClick = { repository.setRoomNotificationLevel(room.roomId, level) }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            CinnySequenceCard {
                                CinnyNavTile(
                                    title = "Copy Room ID",
                                    subtitle = room.roomId,
                                    leading = { Icon(Icons.Outlined.ContentCopy, contentDescription = null, tint = colors.primary) },
                                    onClick = { clipboard.setText(AnnotatedString(room.roomId)) }
                                )
                                HorizontalDivider(color = colors.surfaceLine)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showLeaveConfirm = true }
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(Icons.Outlined.ExitToApp, contentDescription = null, tint = colors.critical)
                                    Text(
                                        text = stringResource(R.string.action_leave_room),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.critical
                                    )
                                }
                            }
                        }
                    }

                    1 -> {
                        item {
                            CinnySequenceCard {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = inviteUserId,
                                            onValueChange = { inviteUserId = it },
                                            placeholder = { Text("@user:matrix.org") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Button(
                                            enabled = inviteUserId.isNotBlank(),
                                            shape = RoundedCornerShape(8.dp),
                                            onClick = {
                                                repository.inviteUser(room.roomId, inviteUserId.trim())
                                                inviteUserId = ""
                                            }
                                        ) {
                                            Text(stringResource(R.string.action_invite))
                                        }
                                    }
                                    OutlinedTextField(
                                        value = memberSearch,
                                        onValueChange = { memberSearch = it },
                                        placeholder = { Text("Filter ${members.size} members…") },
                                        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }

                        items(filteredMembers, key = { it.userId }) { member ->
                            CinnySequenceCard {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    MxcAvatar(
                                        name = member.displayName,
                                        seedId = member.userId,
                                        mxcUrl = member.avatarMxc,
                                        mxcCachePaths = mxcCachePaths,
                                        onRequestMxc = { repository.requestMxcThumbnail(it, 96) },
                                        size = 36.dp,
                                        cornerRadius = 18.dp
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = member.displayName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = colorMXID(member.userId, colors.isDark),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = member.userId,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = colors.onSurfaceMuted,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    val roleLabel = when {
                                        member.powerLevel >= 100L -> "Admin"
                                        member.powerLevel >= 50L -> "Mod"
                                        else -> null
                                    }
                                    if (roleLabel != null) {
                                        Surface(
                                            color = colors.primaryContainer,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = roleLabel,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = colors.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    else -> {
                        item {
                            CinnySequenceCard {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("Room ID: ${room.roomId}", fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                                    Text("Category: ${room.category.name}", fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                                    Text("Encrypted: ${room.isEncrypted}", fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                                    Text("Parents: ${room.parentSpaceIds.joinToString()}", fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showLeaveConfirm) {
        AlertDialog(
            onDismissRequest = { showLeaveConfirm = false },
            title = { Text(stringResource(R.string.action_leave_room)) },
            text = { Text("Leave ${room.name}?") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = colors.critical),
                    onClick = {
                        showLeaveConfirm = false
                        repository.leaveRoom(room.roomId)
                        onBack()
                    }
                ) {
                    Text(stringResource(R.string.action_leave_room))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveConfirm = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}
