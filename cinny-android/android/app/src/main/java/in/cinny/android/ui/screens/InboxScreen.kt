package `in`.cinny.android.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.MarkEmailUnread
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import `in`.cinny.android.R
import `in`.cinny.android.data.CinnyRepository
import `in`.cinny.android.ui.theme.CinnyThemeColors
import uniffi.cinny_core.RoomCategory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxScreen(
    repository: CinnyRepository,
    onBack: () -> Unit,
    onOpenRoom: (String) -> Unit
) {
    val colors = CinnyThemeColors.current
    val rooms by repository.rooms.collectAsState()
    val mxcCachePaths by repository.mxcCachePaths.collectAsState()
    var activeTab by rememberSaveable { mutableIntStateOf(0) } // 0 = Notifications, 1 = Invites

    val invites = remember(rooms) { rooms.filter { it.category == RoomCategory.INVITED } }
    val unreadRooms = remember(rooms) {
        rooms.filter {
            it.category != RoomCategory.INVITED && (it.unreadCount > 0u || it.highlightCount > 0u)
        }.sortedWith(
            compareByDescending<uniffi.cinny_core.RoomSummary> { it.highlightCount }
                .thenByDescending { it.unreadCount }
                .thenByDescending { it.lastActivityTsMs }
        )
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Outlined.Inbox, contentDescription = null, tint = colors.primary)
                        Text(stringResource(R.string.inbox_title), fontWeight = FontWeight.SemiBold)
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
                    text = { Text("${stringResource(R.string.inbox_notifications_tab)} (${unreadRooms.size})") }
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = { Text("${stringResource(R.string.inbox_invites_tab)} (${invites.size})") }
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                if (activeTab == 0) {
                    if (unreadRooms.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(
                                        Icons.Outlined.MarkEmailUnread,
                                        contentDescription = null,
                                        tint = colors.onSurfaceMuted,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.inbox_notifications_empty),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = colors.onSurfaceMuted
                                    )
                                }
                            }
                        }
                    } else {
                        items(unreadRooms, key = { it.roomId }) { room ->
                            CinnySequenceCard {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onOpenRoom(room.roomId) }
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    MxcAvatar(
                                        name = room.name,
                                        seedId = room.roomId,
                                        mxcUrl = room.avatarMxc,
                                        mxcCachePaths = mxcCachePaths,
                                        onRequestMxc = { repository.requestMxcThumbnail(it, 96) },
                                        size = 42.dp,
                                        cornerRadius = if (room.isDirect) 21.dp else 12.dp
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = room.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = colors.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        val preview = room.lastMessagePreview
                                        if (!preview.isNullOrBlank()) {
                                            val sender = room.lastMessageSender
                                            val line = if (!sender.isNullOrBlank()) "$sender: $preview" else preview
                                            Text(
                                                text = line,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = colors.onSurfaceMuted,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    CinnyCountBadge(
                                        count = room.unreadCount,
                                        highlight = room.highlightCount > 0u
                                    )
                                }
                            }
                        }
                    }
                } else {
                    if (invites.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.inbox_invites_empty),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.onSurfaceMuted
                                )
                            }
                        }
                    } else {
                        items(invites, key = { it.roomId }) { room ->
                            CinnySequenceCard {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        MxcAvatar(
                                            name = room.name,
                                            seedId = room.roomId,
                                            mxcUrl = room.avatarMxc,
                                            mxcCachePaths = mxcCachePaths,
                                            onRequestMxc = { repository.requestMxcThumbnail(it, 96) },
                                            size = 42.dp,
                                            cornerRadius = if (room.isDirect) 21.dp else 12.dp
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = room.name,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = colors.onSurface
                                            )
                                            val topic = room.topic
                                            if (!topic.isNullOrBlank()) {
                                                Text(
                                                    text = topic,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = colors.onSurfaceMuted,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        TextButton(onClick = { repository.leaveRoom(room.roomId) }) {
                                            Text(stringResource(R.string.action_decline), color = colors.critical)
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Button(
                                            shape = RoundedCornerShape(8.dp),
                                            onClick = { repository.acceptInvite(room.roomId) }
                                        ) {
                                            Text(stringResource(R.string.action_join))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
