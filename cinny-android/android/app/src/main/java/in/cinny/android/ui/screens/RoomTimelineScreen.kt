package `in`.cinny.android.ui.screens

import android.content.Intent
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import `in`.cinny.android.R
import `in`.cinny.android.data.CinnyMessageLayout
import `in`.cinny.android.data.CinnyMessageSpacing
import `in`.cinny.android.data.CinnyRepository
import `in`.cinny.android.ui.CinnyViewModel
import `in`.cinny.android.ui.theme.CinnyThemeColors
import `in`.cinny.android.ui.theme.colorMXID
import kotlinx.coroutines.launch
import uniffi.cinny_core.MessageDeliveryStatus
import uniffi.cinny_core.RoomMemberSummary
import uniffi.cinny_core.RoomSummary
import uniffi.cinny_core.TimelineMessage
import java.io.File

private val QUICK_EMOJIS = listOf("👍", "❤️", "😂", "😮", "🎉", "🦀")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomTimelineScreen(
    room: RoomSummary,
    repository: CinnyRepository,
    onBack: () -> Unit,
    onOpenRoomSettings: () -> Unit,
    onOpenVerify: () -> Unit
) {
    val colors = CinnyThemeColors.current
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    val settings by repository.settings.collectAsState()
    val messages by repository.messages.collectAsState()
    val typingUsers by repository.typingUsers.collectAsState()
    val searchResults by repository.roomSearchResults.collectAsState()
    val activeThreadRoot by repository.activeThreadRoot.collectAsState()
    val threadMessages by repository.threadMessages.collectAsState()
    val downloadedMedia by repository.downloadedMediaPaths.collectAsState()
    val mxcCachePaths by repository.mxcCachePaths.collectAsState()
    val roomMembers by repository.roomMembers.collectAsState()

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var draft by remember { mutableStateOf("") }
    var replyingTo by remember { mutableStateOf<TimelineMessage?>(null) }
    var editingMessage by remember { mutableStateOf<TimelineMessage?>(null) }
    var actionSheetTarget by remember { mutableStateOf<TimelineMessage?>(null) }
    var sourceViewTarget by remember { mutableStateOf<TimelineMessage?>(null) }
    var showSearchBar by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showTopicDialog by remember { mutableStateOf(false) }
    var showMembersSheet by remember { mutableStateOf(false) }

    val mediaPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            repository.sendAttachmentUri(uri)
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty() && !showSearchBar) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    val shouldPaginate by remember {
        derivedStateOf {
            messages.isNotEmpty() && listState.firstVisibleItemIndex <= 2 && !showSearchBar
        }
    }
    LaunchedEffect(shouldPaginate) {
        if (shouldPaginate) {
            repository.paginateOlderMessages()
        }
    }

    val visibleMessages = if (showSearchBar && searchQuery.isNotBlank()) searchResults else messages
    val spacingDp = when (settings.messageSpacing) {
        CinnyMessageSpacing.Compact -> 2.dp
        CinnyMessageSpacing.Normal -> 6.dp
        CinnyMessageSpacing.Relaxed -> 12.dp
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onOpenRoomSettings),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MxcAvatar(
                            name = room.name,
                            seedId = room.roomId,
                            mxcUrl = room.avatarMxc,
                            mxcCachePaths = mxcCachePaths,
                            onRequestMxc = { repository.requestMxcThumbnail(it, 96) },
                            size = 34.dp,
                            cornerRadius = if (room.isDirect) 17.dp else 10.dp
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (room.isEncrypted) {
                                    Icon(
                                        Icons.Outlined.Lock,
                                        contentDescription = "Encrypted",
                                        tint = colors.success,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Text(
                                    text = room.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            val topicText = room.topic
                            if (!topicText.isNullOrBlank()) {
                                Text(
                                    text = topicText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.onSurfaceMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.clickable { showTopicDialog = true }
                                )
                            }
                        }
                    }
                },
                actions = {
                    IconButton(onClick = {
                        showSearchBar = !showSearchBar
                        if (!showSearchBar) searchQuery = ""
                    }) {
                        Icon(Icons.Outlined.Search, contentDescription = stringResource(R.string.action_search))
                    }
                    IconButton(onClick = {
                        repository.loadRoomMembers(room.roomId)
                        showMembersSheet = true
                    }) {
                        Icon(Icons.Outlined.Group, contentDescription = stringResource(R.string.members_drawer_title))
                    }
                    IconButton(onClick = onOpenRoomSettings) {
                        Icon(Icons.Outlined.Tune, contentDescription = stringResource(R.string.room_settings_title))
                    }
                }
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surface)
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                HorizontalDivider(color = colors.surfaceLine)

                if (typingUsers.isNotEmpty()) {
                    val typingText = if (typingUsers.size == 1) {
                        "${typingUsers[0]} is typing…"
                    } else {
                        "${typingUsers.take(2).joinToString(", ")} typing…"
                    }
                    Text(
                        text = typingText,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.primary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }

                if (replyingTo != null || editingMessage != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.primaryContainer.copy(alpha = 0.45f))
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (editingMessage != null) {
                                "Editing message"
                            } else {
                                "Replying to ${replyingTo?.senderName}"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                replyingTo = null
                                editingMessage = null
                                draft = ""
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.action_cancel), modifier = Modifier.size(16.dp))
                        }
                    }
                }

                if (settings.markdownEnabled) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        items(
                            listOf(
                                "B" to "**bold**",
                                "I" to "*italic*",
                                "</>" to "`code`",
                                "Quote" to "> ",
                                "Code Block" to "```\n\n```"
                            )
                        ) { (label, snippet) ->
                            SuggestionChip(
                                onClick = { draft = if (draft.isEmpty()) snippet else "$draft $snippet" },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(onClick = { mediaPicker.launch("*/*") }) {
                        Icon(
                            Icons.Outlined.AddCircleOutline,
                            contentDescription = "Attach encrypted file",
                            tint = colors.onSurfaceMuted
                        )
                    }
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { next ->
                            val wasBlank = draft.isBlank()
                            draft = next
                            val nowBlank = next.isBlank()
                            if (wasBlank && !nowBlank) {
                                repository.sendTypingNotice(true)
                            } else if (!wasBlank && nowBlank) {
                                repository.sendTypingNotice(false)
                            }
                        },
                        placeholder = { Text(stringResource(R.string.message_hint, room.name)) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                        maxLines = 5
                    )
                    FilledIconButton(
                        onClick = {
                            val text = draft.trim()
                            if (text.isNotEmpty()) {
                                val targetEdit = editingMessage
                                if (targetEdit != null) {
                                    repository.editMessage(targetEdit.eventId, text)
                                    editingMessage = null
                                } else {
                                    repository.sendMessage(text, replyingTo?.eventId)
                                    replyingTo = null
                                }
                                draft = ""
                            }
                        },
                        enabled = draft.isNotBlank(),
                        modifier = Modifier.size(46.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = stringResource(R.string.action_send))
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (showSearchBar) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        repository.searchRoomMessages(it)
                    },
                    placeholder = { Text("Search decrypted messages…") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }

            val showJumpToLatest by remember(visibleMessages.size) {
                derivedStateOf {
                    visibleMessages.isNotEmpty() && listState.canScrollForward
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(spacingDp),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    itemsIndexed(visibleMessages, key = { _, m -> m.eventId }) { index, msg ->
                        val prevMsg = if (index > 0) visibleMessages[index - 1] else null
                        val showDateDivider = prevMsg == null ||
                            !CinnyViewModel.isSameCalendarDay(prevMsg.timestampMs, msg.timestampMs)
                        val groupedWithPrevious = !showDateDivider &&
                            settings.messageLayout == CinnyMessageLayout.Modern &&
                            CinnyViewModel.shouldGroupWithPrevious(prevMsg, msg)

                        if (showDateDivider) {
                            DateDividerRow(
                                label = CinnyViewModel.formatDateDivider(msg.timestampMs, settings.dateFormat)
                            )
                        }

                        val attach = msg.attachment
                        if (settings.autoLoadMedia &&
                            attach != null &&
                            attach.mimeType.startsWith("image/") &&
                            downloadedMedia[msg.eventId] == null
                        ) {
                            LaunchedEffect(msg.eventId) {
                                repository.downloadMedia(msg.eventId, attach.mediaSourceJson, attach.filename)
                            }
                        }

                        val onOpenAttachment: (uniffi.cinny_core.MediaAttachmentInfo) -> Unit = { media ->
                            repository.downloadMedia(
                                eventId = msg.eventId,
                                mediaSourceJson = media.mediaSourceJson,
                                filename = media.filename
                            ) { localPath ->
                                val file = File(localPath)
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    file
                                )
                                val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(uri, media.mimeType)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                runCatching { context.startActivity(viewIntent) }
                            }
                        }

                        CinnyTimelineMessageItem(
                            message = msg,
                            layout = settings.messageLayout,
                            bubbleOwnRight = settings.bubbleOwnRight,
                            groupedWithPrevious = groupedWithPrevious,
                            timeLabel = CinnyViewModel.formatTimeLabel(msg.timestampMs, settings.use24HourClock),
                            mxcCachePaths = mxcCachePaths,
                            cachedMediaPath = downloadedMedia[msg.eventId],
                            onRequestMxc = { repository.requestMxcThumbnail(it, 96) },
                            onOpenAttachment = onOpenAttachment,
                            onOpenMenu = { if (!msg.isRedacted) actionSheetTarget = msg },
                            onReact = { emoji -> repository.toggleReaction(msg.eventId, emoji) },
                            onOpenThread = { repository.openThread(msg) },
                            onUnlockKeys = onOpenVerify
                        )
                    }
                }

                if (showJumpToLatest) {
                    Surface(
                        color = colors.surface,
                        shape = CircleShape,
                        border = BorderStroke(1.dp, colors.containerLine),
                        shadowElevation = 4.dp,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp)
                            .clickable {
                                coroutineScope.launch {
                                    if (visibleMessages.isNotEmpty()) {
                                        listState.animateScrollToItem(visibleMessages.lastIndex)
                                    }
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ArrowDownward,
                                contentDescription = stringResource(R.string.jump_to_latest),
                                tint = colors.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = stringResource(R.string.jump_to_latest),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Cinny Message Action Bottom Sheet
        val target = actionSheetTarget
        if (target != null) {
            ModalBottomSheet(
                onDismissRequest = { actionSheetTarget = null },
                containerColor = colors.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        QUICK_EMOJIS.forEach { emoji ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = colors.surfaceVariant,
                                modifier = Modifier.clickable {
                                    repository.toggleReaction(target.eventId, emoji)
                                    actionSheetTarget = null
                                }
                            ) {
                                Text(
                                    text = emoji,
                                    style = MaterialTheme.typography.headlineSmall,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = colors.surfaceLine)

                    if (target.canReply) {
                        CinnySheetAction(Icons.Outlined.Reply, stringResource(R.string.action_reply)) {
                            editingMessage = null
                            replyingTo = target
                            actionSheetTarget = null
                        }
                    }
                    CinnySheetAction(Icons.Outlined.Forum, stringResource(R.string.action_open_thread)) {
                        repository.openThread(target)
                        actionSheetTarget = null
                    }
                    CinnySheetAction(Icons.Outlined.ContentCopy, stringResource(R.string.action_copy_text)) {
                        clipboard.setText(AnnotatedString(target.body))
                        actionSheetTarget = null
                    }
                    if (settings.developerTools && !target.rawJson.isNullOrBlank()) {
                        CinnySheetAction(Icons.Outlined.Code, stringResource(R.string.action_view_source)) {
                            sourceViewTarget = target
                            actionSheetTarget = null
                        }
                    }
                    if (target.isOwn) {
                        CinnySheetAction(Icons.Outlined.Edit, stringResource(R.string.action_edit)) {
                            replyingTo = null
                            editingMessage = target
                            draft = target.body
                            actionSheetTarget = null
                        }
                        CinnySheetAction(
                            icon = Icons.Outlined.Delete,
                            label = stringResource(R.string.action_delete),
                            isDestructive = true
                        ) {
                            repository.redactMessage(target.eventId)
                            actionSheetTarget = null
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        val srcTarget = sourceViewTarget
        if (srcTarget != null) {
            AlertDialog(
                onDismissRequest = { sourceViewTarget = null },
                title = { Text("Event Source JSON", fontWeight = FontWeight.SemiBold) },
                text = {
                    Text(
                        text = srcTarget.rawJson ?: "{}",
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .heightIn(max = 320.dp)
                            .verticalScroll(rememberScrollState())
                    )
                },
                confirmButton = {
                    TextButton(onClick = { sourceViewTarget = null }) {
                        Text(stringResource(R.string.action_close))
                    }
                }
            )
        }

        if (activeThreadRoot != null) {
            ThreadModalDialog(
                rootMessage = activeThreadRoot!!,
                threadMessages = threadMessages,
                onSendReply = { text -> repository.sendThreadReply(text) },
                onDismiss = { repository.closeThread() }
            )
        }

        if (showTopicDialog) {
            AlertDialog(
                onDismissRequest = { showTopicDialog = false },
                title = {
                    Column {
                        Text(room.name, fontWeight = FontWeight.Bold)
                        Text(
                            stringResource(R.string.room_topic_title),
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceMuted
                        )
                    }
                },
                text = {
                    Text(
                        text = room.topic?.takeIf { it.isNotBlank() } ?: "—",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurface,
                        modifier = Modifier
                            .heightIn(max = 280.dp)
                            .verticalScroll(rememberScrollState())
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        showTopicDialog = false
                        onOpenRoomSettings()
                    }) {
                        Text(stringResource(R.string.room_settings_title))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showTopicDialog = false }) {
                        Text(stringResource(R.string.action_close))
                    }
                }
            )
        }

        if (showMembersSheet) {
            RoomMembersBottomSheet(
                roomName = room.name,
                members = roomMembers,
                mxcCachePaths = mxcCachePaths,
                onRequestMxc = { repository.requestMxcThumbnail(it, 96) },
                onInviteUser = { userId -> repository.inviteUser(room.roomId, userId) },
                onDismiss = { showMembersSheet = false }
            )
        }
    }
}

@Composable
private fun DateDividerRow(label: String) {
    val colors = CinnyThemeColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = colors.surfaceLine)
        Surface(
            color = colors.surface,
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, colors.surfaceLine)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurfaceMuted,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
            )
        }
        HorizontalDivider(modifier = Modifier.weight(1f), color = colors.surfaceLine)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CinnyTimelineMessageItem(
    message: TimelineMessage,
    layout: CinnyMessageLayout,
    bubbleOwnRight: Boolean,
    groupedWithPrevious: Boolean,
    timeLabel: String,
    mxcCachePaths: Map<String, String>,
    cachedMediaPath: String?,
    onRequestMxc: (String?) -> Unit,
    onOpenAttachment: (uniffi.cinny_core.MediaAttachmentInfo) -> Unit,
    onOpenMenu: () -> Unit,
    onReact: (String) -> Unit,
    onOpenThread: () -> Unit,
    onUnlockKeys: () -> Unit
) {
    val colors = CinnyThemeColors.current
    val senderColor = colorMXID(message.senderId, colors.isDark)

    if (layout == CinnyMessageLayout.Compact) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .combinedClickable(onClick = onOpenMenu, onLongClick = onOpenMenu)
                .padding(horizontal = 6.dp, vertical = 3.dp)
        ) {
            message.replyTo?.let { reply ->
                ReplyBanner(reply)
            }
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = timeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceMuted
                )
                Text(
                    text = message.senderName,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = senderColor,
                    maxLines = 1
                )
                Box(modifier = Modifier.weight(1f)) {
                    MessageBodyContent(message, onUnlockKeys)
                }
            }
            AttachmentAndReactions(
                message = message,
                cachedMediaPath = cachedMediaPath,
                onOpenAttachment = onOpenAttachment,
                onReact = onReact,
                onOpenThread = onOpenThread
            )
        }
        return
    }

    if (layout == CinnyMessageLayout.Bubble) {
        val alignEnd = message.isOwn && bubbleOwnRight
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start
        ) {
            Surface(
                color = if (message.isOwn) colors.primaryContainer else colors.surface,
                shape = RoundedCornerShape(14.dp),
                border = if (message.isOwn) null else BorderStroke(1.dp, colors.surfaceLine),
                modifier = Modifier
                    .widthIn(max = 310.dp)
                    .combinedClickable(onClick = onOpenMenu, onLongClick = onOpenMenu)
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    message.replyTo?.let { reply ->
                        ReplyBanner(reply)
                    }
                    if (!alignEnd) {
                        Text(
                            text = message.senderName,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = senderColor
                        )
                    }
                    MessageBodyContent(message, onUnlockKeys)
                    AttachmentAndReactions(
                        message = message,
                        cachedMediaPath = cachedMediaPath,
                        onOpenAttachment = onOpenAttachment,
                        onReact = onReact,
                        onOpenThread = onOpenThread
                    )
                    Row(
                        modifier = Modifier.align(Alignment.End),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (message.isEdited && !message.isRedacted) {
                            Text("edited", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceMuted)
                        }
                        Text(timeLabel, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceMuted)
                    }
                }
            }
        }
        return
    }

    // Default: Cinny Modern Layout
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .combinedClickable(onClick = onOpenMenu, onLongClick = onOpenMenu)
            .padding(
                start = 6.dp,
                end = 6.dp,
                top = if (groupedWithPrevious) 2.dp else 6.dp,
                bottom = 2.dp
            ),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        if (groupedWithPrevious) {
            Box(
                modifier = Modifier.width(36.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    text = timeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceMuted.copy(alpha = 0.7f)
                )
            }
        } else {
            MxcAvatar(
                name = message.senderName,
                seedId = message.senderId,
                mxcUrl = message.senderAvatarMxc,
                mxcCachePaths = mxcCachePaths,
                onRequestMxc = onRequestMxc,
                size = 36.dp,
                cornerRadius = 18.dp
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            message.replyTo?.let { reply ->
                ReplyBanner(reply)
            }

            if (!groupedWithPrevious) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = message.senderName,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = senderColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = timeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceMuted
                        )
                        if (message.isEdited && !message.isRedacted) {
                            Text(
                                text = "(edited)",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.onSurfaceMuted
                            )
                        }
                        if (message.deliveryStatus is MessageDeliveryStatus.Sending) {
                            Text("sending…", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceMuted)
                        } else if (message.deliveryStatus is MessageDeliveryStatus.Failed) {
                            Text("failed", style = MaterialTheme.typography.labelSmall, color = colors.critical)
                        }
                    }

                    if (!message.isRedacted) {
                        IconButton(onClick = onOpenMenu, modifier = Modifier.size(26.dp)) {
                            Icon(
                                Icons.Outlined.MoreHoriz,
                                contentDescription = "Message actions",
                                tint = colors.onSurfaceMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            MessageBodyContent(message, onUnlockKeys)

            AttachmentAndReactions(
                message = message,
                cachedMediaPath = cachedMediaPath,
                onOpenAttachment = onOpenAttachment,
                onReact = onReact,
                onOpenThread = onOpenThread
            )
        }
    }
}

@Composable
private fun ReplyBanner(reply: uniffi.cinny_core.ReplyPreview) {
    val colors = CinnyThemeColors.current
    Surface(
        color = colors.surfaceVariant,
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(14.dp)
                    .background(colors.primary)
            )
            Text(
                text = "${reply.senderName}: ${reply.body}",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MessageBodyContent(
    message: TimelineMessage,
    onUnlockKeys: () -> Unit
) {
    val colors = CinnyThemeColors.current
    val formattedHtml = message.formattedHtml
    when {
        message.isRedacted -> {
            Text(
                text = message.body,
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
                color = colors.onSurfaceMuted
            )
        }
        message.isUnableToDecrypt -> {
            Surface(
                color = colors.warningContainer,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
                    .clickable(onClick = onUnlockKeys)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Outlined.Key, contentDescription = null, tint = colors.warning, modifier = Modifier.size(16.dp))
                    Text(
                        text = message.body,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.warning,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "Unlock",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.warning
                    )
                }
            }
        }
        !formattedHtml.isNullOrBlank() -> {
            Text(
                text = AnnotatedString.fromHtml(formattedHtml),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurface
            )
        }
        else -> {
            Text(
                text = message.body,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurface
            )
        }
    }
}

@Composable
private fun AttachmentAndReactions(
    message: TimelineMessage,
    cachedMediaPath: String?,
    onOpenAttachment: (uniffi.cinny_core.MediaAttachmentInfo) -> Unit,
    onReact: (String) -> Unit,
    onOpenThread: () -> Unit
) {
    val colors = CinnyThemeColors.current
    message.attachment?.let { attach ->
        Spacer(modifier = Modifier.height(6.dp))
        Surface(
            color = colors.surfaceVariant,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, colors.surfaceLine),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenAttachment(attach) }
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                if (cachedMediaPath != null && attach.mimeType.startsWith("image/")) {
                    val bmp = remember(cachedMediaPath) {
                        runCatching { BitmapFactory.decodeFile(cachedMediaPath) }.getOrNull()
                    }
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = attach.filename,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.surface)
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_image_broken),
                                contentDescription = stringResource(R.string.image_load_failed),
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = stringResource(R.string.image_load_failed),
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceMuted
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            attach.filename,
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${if (attach.isEncrypted) "E2EE AES-CTR" else "Plain"} · ${attach.mimeType}",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceMuted
                        )
                    }
                    Icon(Icons.Outlined.Download, contentDescription = "Open attachment", tint = colors.primary)
                }
            }
        }
    }

    if (message.reactions.isNotEmpty() || message.threadReplyCount > 0u) {
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            message.reactions.forEach { reaction ->
                Surface(
                    color = if (reaction.reactedByMe) colors.primaryContainer else colors.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(
                        1.dp,
                        if (reaction.reactedByMe) colors.primary else colors.surfaceLine
                    ),
                    modifier = Modifier.clickable { onReact(reaction.key) }
                ) {
                    Text(
                        text = "${reaction.key} ${reaction.count}",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (reaction.reactedByMe) colors.onPrimaryContainer else colors.onSurface,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            if (message.threadReplyCount > 0u) {
                Text(
                    text = "${message.threadReplyCount} replies in thread",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.primary,
                    modifier = Modifier.clickable(onClick = onOpenThread)
                )
            }
        }
    }
}

@Composable
private fun CinnySheetAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    val colors = CinnyThemeColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isDestructive) colors.critical else colors.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = if (isDestructive) colors.critical else colors.onSurface
        )
    }
}

@Composable
private fun ThreadModalDialog(
    rootMessage: TimelineMessage,
    threadMessages: List<TimelineMessage>,
    onSendReply: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var threadDraft by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Thread", fontWeight = FontWeight.SemiBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp)) {
                    Column(modifier = Modifier.padding(10.dp).fillMaxWidth()) {
                        Text(rootMessage.senderName, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Text(rootMessage.body, style = MaterialTheme.typography.bodySmall)
                    }
                }
                LazyColumn(modifier = Modifier.heightIn(max = 220.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(threadMessages, key = { it.eventId }) { reply ->
                        Column {
                            Text(reply.senderName, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                            Text(reply.body, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                OutlinedTextField(
                    value = threadDraft,
                    onValueChange = { threadDraft = it },
                    placeholder = { Text("Reply in thread…") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmed = threadDraft.trim()
                    if (trimmed.isNotEmpty()) {
                        onSendReply(trimmed)
                        threadDraft = ""
                    }
                },
                enabled = threadDraft.isNotBlank()
            ) {
                Text("Send Reply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoomMembersBottomSheet(
    roomName: String,
    members: List<RoomMemberSummary>,
    mxcCachePaths: Map<String, String>,
    onRequestMxc: (String?) -> Unit,
    onInviteUser: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = CinnyThemeColors.current
    var query by remember { mutableStateOf("") }
    var inviteInput by remember { mutableStateOf("") }

    val filteredMembers = remember(members, query) {
        val q = query.trim().lowercase()
        val sorted = members.sortedByDescending { it.powerLevel }
        if (q.isEmpty()) {
            sorted
        } else {
            sorted.filter {
                it.displayName.lowercase().contains(q) || it.userId.lowercase().contains(q)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.members_drawer_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface
                    )
                    Text(
                        text = "$roomName · ${members.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceMuted
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.action_close))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = inviteInput,
                    onValueChange = { inviteInput = it },
                    placeholder = {
                        Text(
                            stringResource(R.string.members_invite_hint),
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                )
                FilledTonalButton(
                    onClick = {
                        val target = inviteInput.trim()
                        if (target.isNotEmpty()) {
                            onInviteUser(target)
                            inviteInput = ""
                        }
                    },
                    enabled = inviteInput.isNotBlank(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Outlined.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.action_invite))
                }
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = {
                    Text(
                        stringResource(R.string.members_search_hint),
                        style = MaterialTheme.typography.bodySmall
                    )
                },
                leadingIcon = {
                    Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            )

            if (filteredMembers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.members_empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceMuted
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 340.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = PaddingValues(bottom = 20.dp)
                ) {
                    items(filteredMembers, key = { it.userId }) { member ->
                        val roleLabel = when {
                            member.powerLevel >= 100L -> stringResource(R.string.role_admin)
                            member.powerLevel >= 50L -> stringResource(R.string.role_moderator)
                            else -> stringResource(R.string.role_member)
                        }
                        val isElevated = member.powerLevel >= 50L
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            MxcAvatar(
                                name = member.displayName,
                                seedId = member.userId,
                                mxcUrl = member.avatarMxc,
                                mxcCachePaths = mxcCachePaths,
                                onRequestMxc = onRequestMxc,
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
                            Surface(
                                color = if (isElevated) colors.primaryContainer else colors.surfaceVariant,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = roleLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isElevated) colors.onPrimaryContainer else colors.onSurfaceMuted,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
