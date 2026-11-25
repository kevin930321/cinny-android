package `in`.cinny.android.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import `in`.cinny.android.R
import `in`.cinny.android.data.CinnyRepository
import `in`.cinny.android.ui.CinnyViewModel
import `in`.cinny.android.ui.theme.CinnyThemeColors
import uniffi.cinny_core.RoomCategory
import uniffi.cinny_core.RoomSummary
import uniffi.cinny_core.SyncConnectionState

@Composable
fun HomeSpaceScreen(
    repository: CinnyRepository,
    onOpenVerify: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenExplore: () -> Unit,
    onOpenInbox: () -> Unit
) {
    val colors = CinnyThemeColors.current
    val settings by repository.settings.collectAsState()
    val ownProfile by repository.ownProfile.collectAsState()
    val spaces by repository.spaces.collectAsState()
    val rooms by repository.rooms.collectAsState()
    val selectedSpaceId by repository.selectedSpaceId.collectAsState()
    val spaceHierarchy by repository.spaceHierarchy.collectAsState()
    val syncState by repository.syncState.collectAsState()
    val devices by repository.devices.collectAsState()
    val mxcCachePaths by repository.mxcCachePaths.collectAsState()

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showCreateHub by rememberSaveable { mutableStateOf(false) }
    var showExploreSpaceDialog by rememberSaveable { mutableStateOf(false) }
    var invitesExpanded by rememberSaveable { mutableStateOf(true) }
    var channelsExpanded by rememberSaveable { mutableStateOf(true) }
    var dmsExpanded by rememberSaveable { mutableStateOf(true) }

    val filteredRooms = remember(rooms, selectedSpaceId, searchQuery, settings.sortRoomsByActivity) {
        val base = CinnyViewModel.filterRooms(rooms, selectedSpaceId, searchQuery)
        CinnyViewModel.sortRooms(base, settings.sortRoomsByActivity)
    }

    val invitedRooms = remember(filteredRooms) {
        filteredRooms.filter { it.category == RoomCategory.INVITED }
    }
    val regularRooms = remember(filteredRooms) {
        filteredRooms.filter { it.category != RoomCategory.INVITED && !it.isDirect }
    }
    val dmRooms = remember(filteredRooms) {
        filteredRooms.filter { it.category != RoomCategory.INVITED && it.isDirect }
    }
    val joinedNonInvited = remember(filteredRooms) {
        filteredRooms.filter { it.category != RoomCategory.INVITED }
    }

    val dmUnreadTotal = remember(rooms) {
        rooms.filter { it.isDirect && it.category != RoomCategory.INVITED }
            .sumOf { it.unreadCount.toInt() }
            .toUInt()
    }
    val inboxBadgeTotal = remember(rooms) {
        val unreadRooms = rooms.count { it.category != RoomCategory.INVITED && it.unreadCount > 0u }.toUInt()
        val inviteCount = rooms.count { it.category == RoomCategory.INVITED }.toUInt()
        unreadRooms + inviteCount
    }
    val hasUnverified = remember(devices) {
        devices.any { !it.isVerified }
    }

    val subSpaces = remember(spaces, selectedSpaceId) {
        if (selectedSpaceId != null && selectedSpaceId != "dm") {
            CinnyViewModel.childSpacesFor(spaces, selectedSpaceId)
        } else {
            emptyList()
        }
    }

    val selectedSpace = remember(spaces, selectedSpaceId) {
        spaces.find { it.roomId == selectedSpaceId }
    }
    val activeSpaceName = selectedSpace?.name
        ?: if (selectedSpaceId == "dm") {
            stringResource(R.string.space_direct_messages)
        } else {
            stringResource(R.string.space_home)
        }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        // Left Cinny Space Rail (66dp matching upstream Sidebar.css.ts)
        Column(
            modifier = Modifier
                .width(66.dp)
                .fillMaxHeight()
                .background(colors.background)
                .padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Sticky: Home & Direct Messages
            SpaceRailIconButton(
                icon = Icons.Outlined.Home,
                contentDescription = stringResource(R.string.space_home),
                selected = selectedSpaceId == null,
                badgeCount = 0u,
                onClick = { repository.selectSpace(null) }
            )
            Spacer(modifier = Modifier.height(6.dp))
            SpaceRailIconButton(
                icon = Icons.Outlined.ChatBubbleOutline,
                contentDescription = stringResource(R.string.space_direct_messages),
                selected = selectedSpaceId == "dm",
                badgeCount = dmUnreadTotal,
                onClick = { repository.selectSpace("dm") }
            )

            HorizontalDivider(
                color = colors.surfaceLine,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
            )

            // Scrollable Spaces
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                spaces.filter { it.parentSpaceIds.isEmpty() }.forEach { space ->
                    val selected = selectedSpaceId == space.roomId
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        if (selected) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .width(3.dp)
                                    .height(24.dp)
                                    .clip(RoundedCornerShape(topEnd = 3.dp, bottomEnd = 3.dp))
                                    .background(colors.primary)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(if (selected) 12.dp else 21.dp))
                                .border(
                                    width = if (selected) 1.5.dp else 1.dp,
                                    color = if (selected) colors.primary else colors.surfaceLine,
                                    shape = RoundedCornerShape(if (selected) 12.dp else 21.dp)
                                )
                                .clickable { repository.selectSpace(space.roomId) },
                            contentAlignment = Alignment.Center
                        ) {
                            MxcAvatar(
                                name = space.name,
                                seedId = space.roomId,
                                mxcUrl = space.avatarMxc,
                                mxcCachePaths = mxcCachePaths,
                                onRequestMxc = { repository.requestMxcThumbnail(it, 96) },
                                size = 42.dp,
                                cornerRadius = if (selected) 12.dp else 21.dp
                            )
                        }
                        if (space.unreadNotifications > 0u) {
                            CinnyCountBadge(
                                count = space.unreadNotifications,
                                highlight = space.highlightCount > 0u,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(end = 6.dp)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(
                color = colors.surfaceLine,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Bottom Sticky: Create, Explore, Inbox, Verification Warning, User Settings
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SpaceRailIconButton(
                    icon = Icons.Outlined.Add,
                    contentDescription = stringResource(R.string.action_create),
                    selected = false,
                    badgeCount = 0u,
                    onClick = { showCreateHub = true }
                )
                SpaceRailIconButton(
                    icon = Icons.Outlined.Explore,
                    contentDescription = stringResource(R.string.explore_title),
                    selected = false,
                    badgeCount = 0u,
                    onClick = onOpenExplore
                )
                SpaceRailIconButton(
                    icon = Icons.Outlined.Inbox,
                    contentDescription = stringResource(R.string.inbox_title),
                    selected = false,
                    badgeCount = inboxBadgeTotal,
                    onClick = onOpenInbox
                )
                if (hasUnverified) {
                    IconButton(onClick = onOpenVerify) {
                        Icon(
                            imageVector = Icons.Outlined.GppMaybe,
                            contentDescription = stringResource(R.string.verify_warning_desc),
                            tint = colors.warning
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onOpenSettings),
                    contentAlignment = Alignment.Center
                ) {
                    MxcAvatar(
                        name = ownProfile?.displayName ?: ownProfile?.userId ?: "Me",
                        seedId = ownProfile?.userId ?: "me",
                        mxcUrl = ownProfile?.avatarMxc,
                        mxcCachePaths = mxcCachePaths,
                        onRequestMxc = { repository.requestMxcThumbnail(it, 96) },
                        size = 38.dp,
                        cornerRadius = 19.dp
                    )
                }
            }
        }

        VerticalDivider(color = colors.surfaceLine)

        // Right Room List Surface
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(colors.surface)
        ) {
            // Cinny Room List Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (selectedSpace == null) {
                        CinnyBrandLogo(size = 30.dp)
                    } else {
                        MxcAvatar(
                            name = selectedSpace.name,
                            seedId = selectedSpace.roomId,
                            mxcUrl = selectedSpace.avatarMxc,
                            mxcCachePaths = mxcCachePaths,
                            onRequestMxc = { repository.requestMxcThumbnail(it, 96) },
                            size = 30.dp,
                            cornerRadius = 9.dp
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = activeSpaceName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val (dotColor, syncText) = when (syncState) {
                                is SyncConnectionState.Live -> colors.success to stringResource(R.string.sync_live)
                                is SyncConnectionState.Syncing,
                                is SyncConnectionState.CatchingUp -> colors.warning to stringResource(R.string.sync_syncing)
                                is SyncConnectionState.Offline -> colors.onSurfaceMuted to stringResource(R.string.sync_offline)
                                is SyncConnectionState.TokenInvalid -> colors.critical to stringResource(R.string.sync_token_invalid)
                                is SyncConnectionState.Error -> colors.critical to (syncState as SyncConnectionState.Error).message
                            }
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(dotColor)
                            )
                            Text(
                                text = syncText,
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.onSurfaceMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (selectedSpaceId != null && selectedSpaceId != "dm") {
                        IconButton(onClick = {
                            repository.loadSpaceHierarchy(selectedSpaceId!!)
                            showExploreSpaceDialog = true
                        }) {
                            Icon(
                                Icons.Outlined.AccountTree,
                                contentDescription = "Space Hierarchy",
                                tint = colors.onSurfaceMuted
                            )
                        }
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            Icons.Outlined.Settings,
                            contentDescription = stringResource(R.string.settings_title),
                            tint = colors.onSurfaceMuted
                        )
                    }
                }
            }

            HorizontalDivider(color = colors.surfaceLine)

            // Search Input
            Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            stringResource(R.string.search_rooms_hint),
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Outlined.Search,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Outlined.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (subSpaces.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    items(subSpaces, key = { it.roomId }) { sub ->
                        FilterChip(
                            selected = false,
                            onClick = { repository.selectSpace(sub.roomId) },
                            label = { Text(sub.name) }
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (selectedSpace != null) {
                    item(key = "space_lobby_hero_${selectedSpace.roomId}") {
                        Surface(
                            color = colors.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, colors.surfaceLine),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    MxcAvatar(
                                        name = selectedSpace.name,
                                        seedId = selectedSpace.roomId,
                                        mxcUrl = selectedSpace.avatarMxc,
                                        mxcCachePaths = mxcCachePaths,
                                        onRequestMxc = { repository.requestMxcThumbnail(it, 96) },
                                        size = 44.dp,
                                        cornerRadius = 12.dp
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = selectedSpace.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        val heroSubtitle = selectedSpace.topic?.takeIf { it.isNotBlank() }
                                            ?: "${selectedSpace.childRoomIds.size} rooms"
                                        Text(
                                            text = heroSubtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.onSurfaceMuted,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            repository.loadSpaceHierarchy(selectedSpace.roomId)
                                            showExploreSpaceDialog = true
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            Icons.Outlined.AccountTree,
                                            contentDescription = null,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            stringResource(R.string.space_hero_browse_channels),
                                            style = MaterialTheme.typography.labelMedium,
                                            maxLines = 1
                                        )
                                    }
                                    FilledTonalButton(
                                        onClick = { showCreateHub = true },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            Icons.Outlined.Add,
                                            contentDescription = null,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            stringResource(R.string.space_hero_new_room),
                                            style = MaterialTheme.typography.labelMedium,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (invitedRooms.isNotEmpty()) {
                    item {
                        CollapsibleRoomCategoryHeader(
                            title = "${stringResource(R.string.inbox_invites_tab)} (${invitedRooms.size})",
                            expanded = invitesExpanded,
                            onToggle = { invitesExpanded = !invitesExpanded }
                        )
                    }
                    if (invitesExpanded) {
                        items(invitedRooms, key = { it.roomId }) { room ->
                            CinnyRoomListRow(
                                room = room,
                                showAvatar = settings.showRoomAvatars,
                                mxcCachePaths = mxcCachePaths,
                                onRequestMxc = { repository.requestMxcThumbnail(it, 96) },
                                onClick = { },
                                onAcceptInvite = { repository.acceptInvite(room.roomId) },
                                onDeclineInvite = { repository.leaveRoom(room.roomId) }
                            )
                        }
                    }
                }

                if (selectedSpaceId == null && !settings.sortRoomsByActivity) {
                    if (regularRooms.isNotEmpty()) {
                        item {
                            CollapsibleRoomCategoryHeader(
                                title = stringResource(R.string.category_rooms),
                                expanded = channelsExpanded,
                                onToggle = { channelsExpanded = !channelsExpanded }
                            )
                        }
                        if (channelsExpanded) {
                            items(regularRooms, key = { it.roomId }) { room ->
                                CinnyRoomListRow(
                                    room = room,
                                    showAvatar = settings.showRoomAvatars,
                                    mxcCachePaths = mxcCachePaths,
                                    onRequestMxc = { repository.requestMxcThumbnail(it, 96) },
                                    onClick = { repository.openRoom(room) },
                                    onAcceptInvite = {},
                                    onDeclineInvite = {}
                                )
                            }
                        }
                    }
                    if (dmRooms.isNotEmpty()) {
                        item {
                            CollapsibleRoomCategoryHeader(
                                title = stringResource(R.string.space_direct_messages),
                                expanded = dmsExpanded,
                                onToggle = { dmsExpanded = !dmsExpanded }
                            )
                        }
                        if (dmsExpanded) {
                            items(dmRooms, key = { it.roomId }) { room ->
                                CinnyRoomListRow(
                                    room = room,
                                    showAvatar = settings.showRoomAvatars,
                                    mxcCachePaths = mxcCachePaths,
                                    onRequestMxc = { repository.requestMxcThumbnail(it, 96) },
                                    onClick = { repository.openRoom(room) },
                                    onAcceptInvite = {},
                                    onDeclineInvite = {}
                                )
                            }
                        }
                    }
                } else {
                    items(joinedNonInvited, key = { it.roomId }) { room ->
                        CinnyRoomListRow(
                            room = room,
                            showAvatar = settings.showRoomAvatars,
                            mxcCachePaths = mxcCachePaths,
                            onRequestMxc = { repository.requestMxcThumbnail(it, 96) },
                            onClick = { repository.openRoom(room) },
                            onAcceptInvite = {},
                            onDeclineInvite = {}
                        )
                    }
                }
            }
        }
    }

    if (showCreateHub) {
        CreateHubDialog(
            onCreateRoom = { name, topic, isDirect, encrypted, inviteIds ->
                repository.createRoom(name, topic, isDirect, encrypted, inviteIds)
                showCreateHub = false
            },
            onCreateSpace = { name, topic, isPublic ->
                repository.createSpace(name, topic, isPublic)
                showCreateHub = false
            },
            onJoinRoom = { aliasOrId ->
                repository.joinRoom(aliasOrId)
                showCreateHub = false
            },
            onDismiss = { showCreateHub = false }
        )
    }

    if (showExploreSpaceDialog && selectedSpaceId != null) {
        AlertDialog(
            onDismissRequest = { showExploreSpaceDialog = false },
            title = { Text("Space Hierarchy", fontWeight = FontWeight.SemiBold) },
            text = {
                if (spaceHierarchy.isEmpty()) {
                    Text("Loading Space rooms…")
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
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
                                        color = colors.onSurfaceMuted
                                    )
                                }
                                if (entry.isJoined) {
                                    Text("Joined", style = MaterialTheme.typography.labelMedium, color = colors.success)
                                } else {
                                    TextButton(onClick = { repository.joinRoom(entry.roomId) }) {
                                        Text(stringResource(R.string.action_join))
                                    }
                                }
                            }
                            HorizontalDivider(color = colors.surfaceLine)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showExploreSpaceDialog = false }) {
                    Text(stringResource(R.string.action_close))
                }
            }
        )
    }
}

@Composable
private fun SpaceRailIconButton(
    icon: ImageVector,
    contentDescription: String,
    selected: Boolean,
    badgeCount: UInt,
    onClick: () -> Unit
) {
    val colors = CinnyThemeColors.current
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(3.dp)
                    .height(24.dp)
                    .clip(RoundedCornerShape(topEnd = 3.dp, bottomEnd = 3.dp))
                    .background(colors.primary)
            )
        }
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(if (selected) 12.dp else 21.dp))
                .background(if (selected) colors.primaryContainer else colors.surface)
                .border(
                    width = 1.dp,
                    color = if (selected) colors.primary else colors.surfaceLine,
                    shape = RoundedCornerShape(if (selected) 12.dp else 21.dp)
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (selected) colors.onPrimaryContainer else colors.onSurface,
                modifier = Modifier.size(20.dp)
            )
        }
        if (badgeCount > 0u) {
            CinnyCountBadge(
                count = badgeCount,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 6.dp)
            )
        }
    }
}

@Composable
private fun CollapsibleRoomCategoryHeader(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    val colors = CinnyThemeColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = if (expanded) Icons.Outlined.ExpandMore else Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = colors.onSurfaceMuted,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = colors.onSurfaceMuted
        )
    }
}

@Composable
private fun CinnyRoomListRow(
    room: RoomSummary,
    showAvatar: Boolean,
    mxcCachePaths: Map<String, String>,
    onRequestMxc: (String?) -> Unit,
    onClick: () -> Unit,
    onAcceptInvite: () -> Unit,
    onDeclineInvite: () -> Unit
) {
    val colors = CinnyThemeColors.current
    val preview = room.lastMessagePreview
    val sender = room.lastMessageSender
    val subtitle = when {
        !preview.isNullOrBlank() && !sender.isNullOrBlank() -> "$sender: $preview"
        !preview.isNullOrBlank() -> preview
        else -> room.topic
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(enabled = room.category != RoomCategory.INVITED, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (showAvatar) {
            MxcAvatar(
                name = room.name,
                seedId = room.roomId,
                mxcUrl = room.avatarMxc,
                mxcCachePaths = mxcCachePaths,
                onRequestMxc = onRequestMxc,
                size = 38.dp,
                cornerRadius = if (room.isDirect) 19.dp else 10.dp
            )
        } else {
            Icon(
                imageVector = if (room.isEncrypted) Icons.Outlined.Lock else Icons.Outlined.Tag,
                contentDescription = null,
                tint = if (room.isEncrypted) colors.success else colors.onSurfaceMuted,
                modifier = Modifier.size(18.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (showAvatar && room.isEncrypted) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = colors.success,
                        modifier = Modifier.size(12.dp)
                    )
                }
                Text(
                    text = room.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (room.unreadCount > 0u) FontWeight.Bold else FontWeight.Medium,
                    color = colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (room.category == RoomCategory.INVITED) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onAcceptInvite) { Text(stringResource(R.string.action_join)) }
                TextButton(onClick = onDeclineInvite) { Text(stringResource(R.string.action_decline), color = colors.critical) }
            }
        } else if (room.unreadCount > 0u) {
            CinnyCountBadge(
                count = room.unreadCount,
                highlight = room.highlightCount > 0u
            )
        }
    }
}
