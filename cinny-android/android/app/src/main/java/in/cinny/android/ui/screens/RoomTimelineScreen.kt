package `in`.cinny.android.ui.screens

import android.content.Intent
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.AddReaction
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Reply
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import `in`.cinny.android.R
import `in`.cinny.android.data.CinnyRepository
import uniffi.cinny_core.MessageDeliveryStatus
import uniffi.cinny_core.RoomSummary
import uniffi.cinny_core.TimelineMessage
import java.io.File

private val QUICK_EMOJIS = listOf("👍", "❤️", "🦀", "🚀", "😂", "👀")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomTimelineScreen(
    room: RoomSummary,
    repository: CinnyRepository,
    onBack: () -> Unit,
    onOpenVerify: () -> Unit
) {
    val context = LocalContext.current
    val messages by repository.messages.collectAsState()
    val searchResults by repository.roomSearchResults.collectAsState()
    val activeThreadRoot by repository.activeThreadRoot.collectAsState()
    val threadMessages by repository.threadMessages.collectAsState()
    val roomMembers by repository.roomMembers.collectAsState()
    val downloadedMedia by repository.downloadedMediaPaths.collectAsState()

    val listState = rememberLazyListState()
    var draft by remember { mutableStateOf("") }
    var replyingTo by remember { mutableStateOf<TimelineMessage?>(null) }
    var editingMessage by remember { mutableStateOf<TimelineMessage?>(null) }
    var reactionTarget by remember { mutableStateOf<TimelineMessage?>(null) }
    var showSearchBar by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showMembersDialog by remember { mutableStateOf(false) }

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

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (room.isEncrypted) {
                            Icon(
                                Icons.Outlined.Lock,
                                contentDescription = "Encrypted",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(room.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        showSearchBar = !showSearchBar
                        if (!showSearchBar) searchQuery = ""
                    }) {
                        Icon(Icons.Outlined.Search, contentDescription = "Search Messages")
                    }
                    IconButton(onClick = {
                        repository.loadRoomMembers(room.roomId)
                        showMembersDialog = true
                    }) {
                        Icon(Icons.Outlined.Group, contentDescription = "Members & Room Settings")
                    }
                    IconButton(onClick = onOpenVerify) {
                        Icon(Icons.Outlined.VerifiedUser, contentDescription = "Security Center")
                    }
                }
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                if (replyingTo != null || editingMessage != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (editingMessage != null) {
                                "Editing message"
                            } else {
                                "Replying to ${replyingTo?.senderName}"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        TextButton(onClick = {
                            replyingTo = null
                            editingMessage = null
                            draft = ""
                        }) {
                            Text("Cancel")
                        }
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(onClick = { mediaPicker.launch("*/*") }) {
                        Icon(Icons.Outlined.AttachFile, contentDescription = "Attach encrypted image or file")
                    }
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        placeholder = { Text(stringResource(R.string.message_hint, room.name)) },
                        modifier = Modifier.weight(1f),
                        maxLines = 4
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
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = "Send")
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
                    placeholder = { Text("Search decrypted room messages…") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(visibleMessages, key = { it.eventId }) { msg ->
                    TimelineRow(
                        message = msg,
                        cachedMediaPath = downloadedMedia[msg.eventId],
                        onDownloadMedia = { attach ->
                            repository.downloadMedia(
                                eventId = msg.eventId,
                                mediaSourceJson = attach.mediaSourceJson,
                                filename = attach.filename
                            ) { localPath ->
                                val file = File(localPath)
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    file
                                )
                                val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(uri, attach.mimeType)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                runCatching { context.startActivity(viewIntent) }
                            }
                        },
                        onReply = {
                            if (msg.canReply) {
                                editingMessage = null
                                replyingTo = msg
                            }
                        },
                        onOpenThread = { repository.openThread(msg) },
                        onOpenReactionPicker = { reactionTarget = msg },
                        onReact = { emoji -> repository.toggleReaction(msg.eventId, emoji) },
                        onEdit = {
                            replyingTo = null
                            editingMessage = msg
                            draft = msg.body
                        },
                        onRedact = { repository.redactMessage(msg.eventId) }
                    )
                }
            }
        }

        if (reactionTarget != null) {
            AlertDialog(
                onDismissRequest = { reactionTarget = null },
                title = { Text("Add reaction") },
                text = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        QUICK_EMOJIS.forEach { emoji ->
                            TextButton(onClick = {
                                repository.toggleReaction(reactionTarget!!.eventId, emoji)
                                reactionTarget = null
                            }) {
                                Text(emoji, style = MaterialTheme.typography.headlineSmall)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { reactionTarget = null }) { Text("Close") }
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

        if (showMembersDialog) {
            RoomInfoMembersDialog(
                room = room,
                members = roomMembers,
                onInvite = { userId -> repository.inviteUser(room.roomId, userId) },
                onLeave = {
                    showMembersDialog = false
                    repository.leaveRoom(room.roomId)
                },
                onDismiss = { showMembersDialog = false }
            )
        }
    }
}

@Composable
private fun TimelineRow(
    message: TimelineMessage,
    cachedMediaPath: String?,
    onDownloadMedia: (uniffi.cinny_core.MediaAttachmentInfo) -> Unit,
    onReply: () -> Unit,
    onOpenThread: () -> Unit,
    onOpenReactionPicker: () -> Unit,
    onReact: (String) -> Unit,
    onEdit: () -> Unit,
    onRedact: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = message.canReply && !message.isRedacted, onClick = onReply)
            .padding(vertical = 6.dp, horizontal = 8.dp)
    ) {
        message.replyTo?.let { reply ->
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            ) {
                Text(
                    text = "${reply.senderName}: ${reply.body}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Baseline
            ) {
                Text(
                    text = message.senderName,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (message.isEdited && !message.isRedacted) {
                    Text(
                        text = "edited",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (message.deliveryStatus is MessageDeliveryStatus.Sending) {
                    Text("sending…", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else if (message.deliveryStatus is MessageDeliveryStatus.Failed) {
                    Text("failed", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                }
            }

            if (!message.isRedacted) {
                Row {
                    IconButton(onClick = onOpenReactionPicker, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Outlined.AddReaction, contentDescription = "React", modifier = Modifier.size(16.dp))
                    }
                    if (message.canReply) {
                        IconButton(onClick = onReply, modifier = Modifier.size(30.dp)) {
                            Icon(Icons.Outlined.Reply, contentDescription = "Reply", modifier = Modifier.size(16.dp))
                        }
                    }
                    IconButton(onClick = onOpenThread, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Outlined.Forum, contentDescription = "Thread", modifier = Modifier.size(16.dp))
                    }
                    if (message.isOwn) {
                        IconButton(onClick = onEdit, modifier = Modifier.size(30.dp)) {
                            Icon(Icons.Outlined.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = onRedact, modifier = Modifier.size(30.dp)) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(2.dp))
        if (message.isRedacted) {
            Text(
                text = message.body,
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else if (!message.formattedHtml.isNullOrBlank()) {
            Text(
                text = AnnotatedString.fromHtml(message.formattedHtml),
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            Text(
                text = message.body,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        message.attachment?.let { attach ->
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onDownloadMedia(attach) }
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    if (cachedMediaPath != null && attach.mimeType.startsWith("image/")) {
                        val bmp = remember(cachedMediaPath) { BitmapFactory.decodeFile(cachedMediaPath) }
                        if (bmp != null) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = attach.filename,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(attach.filename, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                text = "${if (attach.isEncrypted) "E2EE AES-CTR" else "Plain"} · ${attach.mimeType}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(Icons.Outlined.Download, contentDescription = "Decrypt & open attachment")
                    }
                }
            }
        }

        if (message.reactions.isNotEmpty() || message.threadReplyCount > 0u) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                message.reactions.forEach { reaction ->
                    val bg = if (reaction.reactedByMe) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                    Text(
                        text = "${reaction.key} ${reaction.count}",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(bg)
                            .clickable { onReact(reaction.key) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                if (message.threadReplyCount > 0u) {
                    Text(
                        text = "${message.threadReplyCount} replies in thread",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable(onClick = onOpenThread)
                    )
                }
            }
        }
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

@Composable
private fun RoomInfoMembersDialog(
    room: RoomSummary,
    members: List<uniffi.cinny_core.RoomMemberSummary>,
    onInvite: (String) -> Unit,
    onLeave: () -> Unit,
    onDismiss: () -> Unit
) {
    var inviteUserId by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(room.name, fontWeight = FontWeight.SemiBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!room.topic.isNullOrBlank()) {
                    Text(room.topic!!, style = MaterialTheme.typography.bodySmall)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = inviteUserId,
                        onValueChange = { inviteUserId = it },
                        placeholder = { Text("@user:matrix.org") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            if (inviteUserId.isNotBlank()) {
                                onInvite(inviteUserId.trim())
                                inviteUserId = ""
                            }
                        }
                    ) { Text("Invite") }
                }
                Text("Active Members (${members.size})", style = MaterialTheme.typography.labelLarge)
                LazyColumn(modifier = Modifier.heightIn(max = 180.dp)) {
                    items(members, key = { it.userId }) { m ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(m.displayName, style = MaterialTheme.typography.bodySmall)
                            Text("PL ${m.powerLevel}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onLeave, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                Text("Leave Room")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
