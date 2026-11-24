package `in`.cinny.android.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.cinny.android.R
import `in`.cinny.android.data.CinnyDateFormat
import `in`.cinny.android.data.CinnyMessageLayout
import `in`.cinny.android.data.CinnyMessageSpacing
import `in`.cinny.android.data.CinnyRepository
import `in`.cinny.android.data.CinnyThemeMode
import `in`.cinny.android.ui.theme.CinnyThemeColors
import uniffi.cinny_core.SyncConnectionState

enum class SettingsSection {
    Overview,
    General,
    Account,
    Notifications,
    Devices,
    Developer,
    About
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    repository: CinnyRepository,
    initialSection: SettingsSection = SettingsSection.Overview,
    onOpenVerifyDialog: () -> Unit,
    onBack: () -> Unit
) {
    val colors = CinnyThemeColors.current
    val settings by repository.settings.collectAsState()
    val ownProfile by repository.ownProfile.collectAsState()
    val devices by repository.devices.collectAsState()
    val backupStatus by repository.secretBackupStatus.collectAsState()
    val syncState by repository.syncState.collectAsState()
    val mxcCachePaths by repository.mxcCachePaths.collectAsState()
    val pushEndpoint by repository.pushEndpoint.collectAsState()
    val pushGateway by repository.pushGatewayUrl.collectAsState()
    val pushStatus by repository.pushRegistrationStatus.collectAsState()
    val rooms by repository.rooms.collectAsState()
    val spaces by repository.spaces.collectAsState()
    val actionNotice by repository.actionNotice.collectAsState()

    var section by rememberSaveable { mutableStateOf(initialSection) }
    var showSignOutConfirm by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        repository.refreshOwnProfile()
        repository.loadOwnDevices()
    }

    val unverifiedCount = remember(devices) {
        devices.count { !it.isVerified }
    }

    val titleText = when (section) {
        SettingsSection.Overview -> stringResource(R.string.settings_title)
        SettingsSection.General -> stringResource(R.string.settings_general)
        SettingsSection.Account -> stringResource(R.string.settings_account)
        SettingsSection.Notifications -> stringResource(R.string.settings_notifications)
        SettingsSection.Devices -> stringResource(R.string.settings_devices)
        SettingsSection.Developer -> stringResource(R.string.settings_developer)
        SettingsSection.About -> stringResource(R.string.settings_about)
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
                    IconButton(onClick = {
                        if (section != SettingsSection.Overview && initialSection == SettingsSection.Overview) {
                            section = SettingsSection.Overview
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                title = {
                    Text(titleText, fontWeight = FontWeight.SemiBold)
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
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

            when (section) {
                SettingsSection.Overview -> {
                    item {
                        CinnySequenceCard {
                            val displayName = ownProfile?.displayName ?: ownProfile?.userId ?: "Matrix User"
                            val userId = ownProfile?.userId ?: ""
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { section = SettingsSection.Account }
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                MxcAvatar(
                                    name = displayName,
                                    seedId = userId,
                                    mxcUrl = ownProfile?.avatarMxc,
                                    mxcCachePaths = mxcCachePaths,
                                    onRequestMxc = { repository.requestMxcThumbnail(it, 128) },
                                    size = 52.dp,
                                    cornerRadius = 26.dp
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = displayName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.onSurface
                                    )
                                    Text(
                                        text = userId,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.onSurfaceMuted
                                    )
                                }
                                Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = colors.onSurfaceMuted)
                            }
                        }
                    }

                    item {
                        CinnySequenceCard {
                            CinnyNavTile(
                                title = stringResource(R.string.settings_general),
                                subtitle = "${settings.themeMode.name} · ${settings.messageLayout.name}",
                                leading = { Icon(Icons.Outlined.Tune, contentDescription = null, tint = colors.primary) },
                                onClick = { section = SettingsSection.General }
                            )
                            HorizontalDivider(color = colors.surfaceLine)
                            CinnyNavTile(
                                title = stringResource(R.string.settings_account),
                                subtitle = ownProfile?.homeserverUrl,
                                leading = { Icon(Icons.Outlined.PersonOutline, contentDescription = null, tint = colors.primary) },
                                onClick = { section = SettingsSection.Account }
                            )
                            HorizontalDivider(color = colors.surfaceLine)
                            CinnyNavTile(
                                title = stringResource(R.string.settings_notifications),
                                subtitle = if (pushEndpoint.isNullOrBlank()) "UnifiedPush not configured" else "Push gateway configured",
                                leading = { Icon(Icons.Outlined.Notifications, contentDescription = null, tint = colors.primary) },
                                onClick = { section = SettingsSection.Notifications }
                            )
                            HorizontalDivider(color = colors.surfaceLine)
                            CinnyNavTile(
                                title = stringResource(R.string.settings_devices),
                                subtitle = "${devices.size} sessions · ${if (backupStatus?.backupEnabled == true) "4S active" else "4S locked"}",
                                leading = { Icon(Icons.Outlined.Devices, contentDescription = null, tint = colors.primary) },
                                trailingBadge = if (unverifiedCount > 0) "$unverifiedCount unverified" else null,
                                onClick = { section = SettingsSection.Devices }
                            )
                            HorizontalDivider(color = colors.surfaceLine)
                            CinnyNavTile(
                                title = stringResource(R.string.settings_developer),
                                subtitle = if (settings.developerTools) "Enabled" else "Event source & room IDs",
                                leading = { Icon(Icons.Outlined.Code, contentDescription = null, tint = colors.primary) },
                                onClick = { section = SettingsSection.Developer }
                            )
                            HorizontalDivider(color = colors.surfaceLine)
                            CinnyNavTile(
                                title = stringResource(R.string.settings_about),
                                subtitle = "Cinny Android · Rust matrix-sdk 0.16",
                                leading = { Icon(Icons.Outlined.Info, contentDescription = null, tint = colors.primary) },
                                onClick = { section = SettingsSection.About }
                            )
                        }
                    }

                    item {
                        CinnySequenceCard {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showSignOutConfirm = true }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Outlined.Logout,
                                    contentDescription = null,
                                    tint = colors.critical
                                )
                                Text(
                                    text = stringResource(R.string.action_sign_out),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.critical
                                )
                            }
                        }
                    }
                }

                SettingsSection.General -> {
                    item {
                        CinnySectionTitle(stringResource(R.string.settings_appearance))
                        CinnySequenceCard {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.settings_theme_label),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(CinnyThemeMode.entries) { mode ->
                                        val swatch = when (mode) {
                                            CinnyThemeMode.System -> colors.primary
                                            CinnyThemeMode.Light -> Color(0xFFF5F5F5)
                                            CinnyThemeMode.Silver -> Color(0xFFDEDEDE)
                                            CinnyThemeMode.Dark -> Color(0xFF1A1A1A)
                                            CinnyThemeMode.Butter -> Color(0xFF262621)
                                        }
                                        val selected = settings.themeMode == mode
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (selected) colors.primaryContainer else colors.surfaceVariant,
                                            border = BorderStroke(
                                                1.dp,
                                                if (selected) colors.primary else colors.surfaceLine
                                            ),
                                            modifier = Modifier.clickable {
                                                repository.updateSettings { it.copy(themeMode = mode) }
                                            }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(14.dp)
                                                        .clip(CircleShape)
                                                        .background(swatch)
                                                        .border(1.dp, colors.containerLine, CircleShape)
                                                )
                                                Text(
                                                    text = mode.name,
                                                    style = MaterialTheme.typography.labelLarge,
                                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (selected) colors.onPrimaryContainer else colors.onSurface
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        CinnySectionTitle(stringResource(R.string.settings_messages_section))
                        CinnySequenceCard {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.settings_message_layout),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    CinnyMessageLayout.entries.forEach { layout ->
                                        FilterChip(
                                            selected = settings.messageLayout == layout,
                                            onClick = { repository.updateSettings { it.copy(messageLayout = layout) } },
                                            label = { Text(layout.name) }
                                        )
                                    }
                                }

                                Text(
                                    text = stringResource(R.string.settings_message_spacing),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    CinnyMessageSpacing.entries.forEach { spacing ->
                                        FilterChip(
                                            selected = settings.messageSpacing == spacing,
                                            onClick = { repository.updateSettings { it.copy(messageSpacing = spacing) } },
                                            label = { Text(spacing.name) }
                                        )
                                    }
                                }
                            }
                            HorizontalDivider(color = colors.surfaceLine)
                            if (settings.messageLayout == CinnyMessageLayout.Bubble) {
                                CinnySettingSwitchRow(
                                    title = stringResource(R.string.settings_bubble_own_right),
                                    subtitle = stringResource(R.string.settings_bubble_own_right_desc),
                                    checked = settings.bubbleOwnRight,
                                    onCheckedChange = { v -> repository.updateSettings { it.copy(bubbleOwnRight = v) } }
                                )
                                HorizontalDivider(color = colors.surfaceLine)
                            }
                            CinnySettingSwitchRow(
                                title = stringResource(R.string.settings_auto_load_media),
                                subtitle = stringResource(R.string.settings_auto_load_media_desc),
                                checked = settings.autoLoadMedia,
                                onCheckedChange = { v -> repository.updateSettings { it.copy(autoLoadMedia = v) } }
                            )
                        }
                    }

                    item {
                        CinnySectionTitle(stringResource(R.string.settings_room_list_section))
                        CinnySequenceCard {
                            CinnySettingSwitchRow(
                                title = stringResource(R.string.settings_show_room_avatars),
                                checked = settings.showRoomAvatars,
                                onCheckedChange = { v -> repository.updateSettings { it.copy(showRoomAvatars = v) } }
                            )
                            HorizontalDivider(color = colors.surfaceLine)
                            CinnySettingSwitchRow(
                                title = stringResource(R.string.settings_sort_by_activity),
                                subtitle = stringResource(R.string.settings_sort_by_activity_desc),
                                checked = settings.sortRoomsByActivity,
                                onCheckedChange = { v -> repository.updateSettings { it.copy(sortRoomsByActivity = v) } }
                            )
                            HorizontalDivider(color = colors.surfaceLine)
                            CinnySettingSwitchRow(
                                title = stringResource(R.string.settings_hide_activity),
                                subtitle = stringResource(R.string.settings_hide_activity_desc),
                                checked = settings.hideActivity,
                                onCheckedChange = { v -> repository.updateSettings { it.copy(hideActivity = v) } }
                            )
                        }
                    }

                    item {
                        CinnySectionTitle(stringResource(R.string.settings_datetime_editor_section))
                        CinnySequenceCard {
                            CinnySettingSwitchRow(
                                title = stringResource(R.string.settings_24h_clock),
                                checked = settings.use24HourClock,
                                onCheckedChange = { v -> repository.updateSettings { it.copy(use24HourClock = v) } }
                            )
                            HorizontalDivider(color = colors.surfaceLine)
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(stringResource(R.string.settings_date_format), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    CinnyDateFormat.entries.forEach { fmt ->
                                        FilterChip(
                                            selected = settings.dateFormat == fmt,
                                            onClick = { repository.updateSettings { it.copy(dateFormat = fmt) } },
                                            label = { Text(fmt.name) }
                                        )
                                    }
                                }
                            }
                            HorizontalDivider(color = colors.surfaceLine)
                            CinnySettingSwitchRow(
                                title = stringResource(R.string.settings_markdown_toolbar),
                                subtitle = stringResource(R.string.settings_markdown_toolbar_desc),
                                checked = settings.markdownEnabled,
                                onCheckedChange = { v -> repository.updateSettings { it.copy(markdownEnabled = v) } }
                            )
                            HorizontalDivider(color = colors.surfaceLine)
                            CinnySettingSwitchRow(
                                title = stringResource(R.string.settings_enter_newline),
                                checked = settings.enterForNewline,
                                onCheckedChange = { v -> repository.updateSettings { it.copy(enterForNewline = v) } }
                            )
                        }
                    }
                }

                SettingsSection.Account -> {
                    item {
                        var displayNameDraft by remember(ownProfile?.displayName) {
                            mutableStateOf(ownProfile?.displayName ?: "")
                        }
                        CinnySectionTitle(stringResource(R.string.settings_profile_section))
                        CinnySequenceCard {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    MxcAvatar(
                                        name = ownProfile?.displayName ?: ownProfile?.userId ?: "U",
                                        seedId = ownProfile?.userId ?: "",
                                        mxcUrl = ownProfile?.avatarMxc,
                                        mxcCachePaths = mxcCachePaths,
                                        onRequestMxc = { repository.requestMxcThumbnail(it, 128) },
                                        size = 56.dp,
                                        cornerRadius = 28.dp
                                    )
                                    Column {
                                        Text(
                                            text = ownProfile?.userId ?: "",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = ownProfile?.homeserverUrl ?: "",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.onSurfaceMuted
                                        )
                                        val devId = ownProfile?.deviceId
                                        if (!devId.isNullOrBlank()) {
                                            Text(
                                                text = "Device: $devId",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = colors.onSurfaceMuted
                                            )
                                        }
                                    }
                                }
                                OutlinedTextField(
                                    value = displayNameDraft,
                                    onValueChange = { displayNameDraft = it },
                                    label = { Text(stringResource(R.string.settings_display_name_label)) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Button(
                                    shape = RoundedCornerShape(8.dp),
                                    onClick = { repository.updateDisplayName(displayNameDraft) }
                                ) {
                                    Text(stringResource(R.string.action_save))
                                }
                            }
                        }
                    }
                }

                SettingsSection.Notifications -> {
                    item {
                        var gatewayInput by remember(pushGateway) { mutableStateOf(pushGateway) }
                        var pushKeyInput by remember(pushEndpoint) { mutableStateOf(pushEndpoint ?: "") }
                        CinnySectionTitle(stringResource(R.string.settings_notifications))
                        CinnySequenceCard {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Configure UnifiedPush or Sygnal HTTP Matrix Pusher so encrypted messages decrypt in background WorkManager.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.onSurfaceMuted
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
                                    label = { Text("UnifiedPush Endpoint / Token") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                if (!pushStatus.isNullOrBlank()) {
                                    Text(
                                        text = pushStatus!!,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.primary
                                    )
                                }
                                Button(
                                    shape = RoundedCornerShape(8.dp),
                                    enabled = gatewayInput.isNotBlank() && pushKeyInput.isNotBlank(),
                                    onClick = { repository.registerHttpPusher(pushKeyInput.trim(), gatewayInput.trim()) }
                                ) {
                                    Text("Register HTTP Pusher")
                                }
                            }
                        }
                    }
                }

                SettingsSection.Devices -> {
                    item {
                        CinnySectionTitle("4S Recovery & Cross-Signing")
                        CinnySequenceCard {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Master Key: ${if (backupStatus?.hasMasterKey == true) "Active" else "Missing"} · Backup: ${if (backupStatus?.backupEnabled == true) "Unlocked" else "Locked"}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Button(
                                    shape = RoundedCornerShape(8.dp),
                                    onClick = onOpenVerifyDialog
                                ) {
                                    Text(stringResource(R.string.settings_open_security_center))
                                }
                            }
                        }
                    }

                    item {
                        CinnySectionTitle("Active Sessions (${devices.size})")
                    }

                    items(devices, key = { it.deviceId }) { dev ->
                        CinnySequenceCard {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = if (dev.isVerified) Icons.Outlined.VerifiedUser else Icons.Outlined.GppMaybe,
                                    contentDescription = null,
                                    tint = if (dev.isVerified) colors.success else colors.warning
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = dev.displayName ?: dev.deviceId,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "${dev.deviceId}${if (dev.isCurrentDevice) " · Current device" else ""}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.onSurfaceMuted
                                    )
                                }
                                if (!dev.isVerified && !dev.isCurrentDevice) {
                                    FilledTonalButton(
                                        shape = RoundedCornerShape(8.dp),
                                        onClick = {
                                            repository.startSasVerification(dev.userId, dev.deviceId)
                                            onOpenVerifyDialog()
                                        }
                                    ) {
                                        Text("Verify")
                                    }
                                } else {
                                    Text(
                                        text = if (dev.isVerified) "Verified" else "Unverified",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (dev.isVerified) colors.success else colors.warning,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }

                SettingsSection.Developer -> {
                    item {
                        val clipboard = LocalClipboardManager.current
                        CinnySectionTitle(stringResource(R.string.settings_developer))
                        CinnySequenceCard {
                            CinnySettingSwitchRow(
                                title = stringResource(R.string.settings_enable_devtools),
                                subtitle = stringResource(R.string.settings_enable_devtools_desc),
                                checked = settings.developerTools,
                                onCheckedChange = { v -> repository.updateSettings { it.copy(developerTools = v) } }
                            )
                            HorizontalDivider(color = colors.surfaceLine)
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Sync State: ${syncState::class.simpleName} · Spaces: ${spaces.size} · Rooms: ${rooms.size}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace
                                )
                                val uid = ownProfile?.userId
                                if (!uid.isNullOrBlank()) {
                                    OutlinedButton(
                                        shape = RoundedCornerShape(8.dp),
                                        onClick = { clipboard.setText(AnnotatedString(uid)) }
                                    ) {
                                        Text("Copy Matrix ID ($uid)")
                                    }
                                }
                            }
                        }
                    }
                }

                SettingsSection.About -> {
                    item {
                        CinnySequenceCard {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Cinny for Android",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Inspired by upstream cinnyapp/cinny WebUI, powered natively by Rust matrix-sdk 0.16, UniFFI 0.29, and Jetpack Compose Material 3.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.onSurfaceMuted
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSignOutConfirm) {
        AlertDialog(
            onDismissRequest = { showSignOutConfirm = false },
            title = { Text(stringResource(R.string.action_sign_out)) },
            text = { Text(stringResource(R.string.sign_out_confirm_desc)) },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = colors.critical),
                    onClick = {
                        showSignOutConfirm = false
                        repository.logout()
                    }
                ) {
                    Text(stringResource(R.string.action_sign_out))
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutConfirm = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}
