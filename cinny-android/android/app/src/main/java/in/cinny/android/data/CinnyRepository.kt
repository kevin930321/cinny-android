package `in`.cinny.android.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import uniffi.cinny_core.CinnyClient
import uniffi.cinny_core.DeviceSummary
import uniffi.cinny_core.HomeserverLoginCapabilities
import uniffi.cinny_core.RoomMemberSummary
import uniffi.cinny_core.RoomSummary
import uniffi.cinny_core.SecretBackupStatus
import uniffi.cinny_core.SpaceHierarchyEntry
import uniffi.cinny_core.SpaceNode
import uniffi.cinny_core.SyncConnectionState
import uniffi.cinny_core.SyncObserver
import uniffi.cinny_core.TimelineListener
import uniffi.cinny_core.TimelineMessage
import uniffi.cinny_core.VerificationListener
import uniffi.cinny_core.VerificationPhase
import uniffi.cinny_core.buildSsoLoginUrl
import uniffi.cinny_core.discoverLoginCapabilities
import java.io.File

class CinnyRepository(private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val secureStore = SecureSessionStore(context)
    private var client: CinnyClient? = null
    private var syncRunning = false

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _isRestoringSession = MutableStateFlow(false)
    val isRestoringSession: StateFlow<Boolean> = _isRestoringSession.asStateFlow()

    private val _isLoggingIn = MutableStateFlow(false)
    val isLoggingIn: StateFlow<Boolean> = _isLoggingIn.asStateFlow()

    private val _loginCapabilities = MutableStateFlow<HomeserverLoginCapabilities?>(null)
    val loginCapabilities: StateFlow<HomeserverLoginCapabilities?> = _loginCapabilities.asStateFlow()

    private val _pendingSsoUrl = MutableStateFlow<String?>(null)
    val pendingSsoUrl: StateFlow<String?> = _pendingSsoUrl.asStateFlow()

    private val _themePreference = MutableStateFlow(secureStore.loadThemePreference())
    val themePreference: StateFlow<ThemePreference> = _themePreference.asStateFlow()

    private val _syncState = MutableStateFlow<SyncConnectionState>(SyncConnectionState.Offline)
    val syncState: StateFlow<SyncConnectionState> = _syncState.asStateFlow()

    private val _spaces = MutableStateFlow<List<SpaceNode>>(emptyList())
    val spaces: StateFlow<List<SpaceNode>> = _spaces.asStateFlow()

    private val _rooms = MutableStateFlow<List<RoomSummary>>(emptyList())
    val rooms: StateFlow<List<RoomSummary>> = _rooms.asStateFlow()

    private val _selectedSpaceId = MutableStateFlow<String?>(null)
    val selectedSpaceId: StateFlow<String?> = _selectedSpaceId.asStateFlow()

    private val _spaceHierarchy = MutableStateFlow<List<SpaceHierarchyEntry>>(emptyList())
    val spaceHierarchy: StateFlow<List<SpaceHierarchyEntry>> = _spaceHierarchy.asStateFlow()

    private val _activeRoom = MutableStateFlow<RoomSummary?>(null)
    val activeRoom: StateFlow<RoomSummary?> = _activeRoom.asStateFlow()

    private val _messages = MutableStateFlow<List<TimelineMessage>>(emptyList())
    val messages: StateFlow<List<TimelineMessage>> = _messages.asStateFlow()

    private val _roomSearchResults = MutableStateFlow<List<TimelineMessage>>(emptyList())
    val roomSearchResults: StateFlow<List<TimelineMessage>> = _roomSearchResults.asStateFlow()

    private val _activeThreadRoot = MutableStateFlow<TimelineMessage?>(null)
    val activeThreadRoot: StateFlow<TimelineMessage?> = _activeThreadRoot.asStateFlow()

    private val _threadMessages = MutableStateFlow<List<TimelineMessage>>(emptyList())
    val threadMessages: StateFlow<List<TimelineMessage>> = _threadMessages.asStateFlow()

    private val _roomMembers = MutableStateFlow<List<RoomMemberSummary>>(emptyList())
    val roomMembers: StateFlow<List<RoomMemberSummary>> = _roomMembers.asStateFlow()

    private val _devices = MutableStateFlow<List<DeviceSummary>>(emptyList())
    val devices: StateFlow<List<DeviceSummary>> = _devices.asStateFlow()

    private val _verificationPhase = MutableStateFlow<VerificationPhase?>(null)
    val verificationPhase: StateFlow<VerificationPhase?> = _verificationPhase.asStateFlow()

    private val _secretBackupStatus = MutableStateFlow<SecretBackupStatus?>(null)
    val secretBackupStatus: StateFlow<SecretBackupStatus?> = _secretBackupStatus.asStateFlow()

    private val _generatedRecoveryKey = MutableStateFlow<String?>(null)
    val generatedRecoveryKey: StateFlow<String?> = _generatedRecoveryKey.asStateFlow()

    private val _cryptoNotice = MutableStateFlow<String?>(null)
    val cryptoNotice: StateFlow<String?> = _cryptoNotice.asStateFlow()

    private val _downloadedMediaPaths = MutableStateFlow<Map<String, String>>(emptyMap())
    val downloadedMediaPaths: StateFlow<Map<String, String>> = _downloadedMediaPaths.asStateFlow()

    private val _pushEndpoint = MutableStateFlow<String?>(secureStore.loadPushEndpoint())
    val pushEndpoint: StateFlow<String?> = _pushEndpoint.asStateFlow()

    private val _pushGatewayUrl = MutableStateFlow(secureStore.loadPushGateway())
    val pushGatewayUrl: StateFlow<String> = _pushGatewayUrl.asStateFlow()

    private val _pushRegistrationStatus = MutableStateFlow<String?>(null)
    val pushRegistrationStatus: StateFlow<String?> = _pushRegistrationStatus.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError.asStateFlow()

    fun cycleThemePreference() {
        val next = when (_themePreference.value) {
            ThemePreference.FollowSystem -> ThemePreference.Dark
            ThemePreference.Dark -> ThemePreference.Light
            ThemePreference.Light -> ThemePreference.FollowSystem
        }
        _themePreference.value = next
        secureStore.saveThemePreference(next)
    }

    fun selectSpace(spaceId: String?) {
        _selectedSpaceId.value = spaceId
    }

    fun clearActionError() {
        _actionError.value = null
    }

    fun probeHomeserverCapabilities(homeserverUrl: String) {
        scope.launch {
            runCatching { discoverLoginCapabilities(homeserverUrl.trim()) }
                .onSuccess { caps -> _loginCapabilities.value = caps }
        }
    }

    fun startSsoLogin(homeserverUrl: String, idpId: String? = null) {
        scope.launch {
            _authError.value = null
            _isLoggingIn.value = true
            try {
                val cleanHs = homeserverUrl.trim()
                secureStore.savePendingSsoHomeserver(cleanHs)
                val url = buildSsoLoginUrl(
                    homeserverUrl = cleanHs,
                    redirectUrl = "cinny://sso/callback",
                    idpId = idpId
                )
                _pendingSsoUrl.value = url
            } catch (err: Exception) {
                _authError.value = err.message ?: "Unable to launch SSO flow"
            } finally {
                _isLoggingIn.value = false
            }
        }
    }

    fun consumePendingSsoUrl() {
        _pendingSsoUrl.value = null
    }

    fun completeSsoCallback(loginToken: String) {
        val homeserver = secureStore.loadPendingSsoHomeserver() ?: return
        scope.launch {
            _authError.value = null
            _isLoggingIn.value = true
            try {
                val sqliteRoot = File(context.filesDir, "matrix_stores").apply { mkdirs() }
                val passphrase = secureStore.getOrCreateSqlitePassphrase()
                val loggedIn = CinnyClient.loginWithSsoToken(
                    homeserverUrl = homeserver,
                    loginToken = loginToken,
                    sqliteRootDir = sqliteRoot.absolutePath,
                    sqlitePassphrase = passphrase
                )
                secureStore.savePendingSsoHomeserver(null)
                val exported = loggedIn.exportSession()
                secureStore.saveSession(exported)
                onClientReady(loggedIn)
            } catch (err: Exception) {
                _authError.value = err.message ?: "SSO authentication failed"
            } finally {
                _isLoggingIn.value = false
            }
        }
    }

    fun restoreSavedSessionIfAvailable() {
        val saved = secureStore.loadSession() ?: return
        scope.launch {
            _isRestoringSession.value = true
            try {
                val sqliteRoot = File(context.filesDir, "matrix_stores").apply { mkdirs() }
                val passphrase = secureStore.getOrCreateSqlitePassphrase()
                val restored = CinnyClient.restoreSession(
                    session = saved,
                    sqliteRootDir = sqliteRoot.absolutePath,
                    sqlitePassphrase = passphrase
                )
                onClientReady(restored)
            } catch (err: Exception) {
                secureStore.clearSession()
                _authError.value = err.message ?: "Saved session could not be restored"
            } finally {
                _isRestoringSession.value = false
            }
        }
    }

    fun login(homeserver: String, username: String, password: String) {
        scope.launch {
            _authError.value = null
            _isLoggingIn.value = true
            try {
                val sqliteRoot = File(context.filesDir, "matrix_stores").apply { mkdirs() }
                val passphrase = secureStore.getOrCreateSqlitePassphrase()
                val loggedIn = CinnyClient.loginWithPassword(
                    homeserverUrl = homeserver,
                    username = username,
                    password = password,
                    sqliteRootDir = sqliteRoot.absolutePath,
                    sqlitePassphrase = passphrase
                )
                val exported = loggedIn.exportSession()
                secureStore.saveSession(exported)
                onClientReady(loggedIn)
            } catch (err: Exception) {
                _authError.value = err.message ?: "Authentication failed"
            } finally {
                _isLoggingIn.value = false
            }
        }
    }

    private suspend fun onClientReady(activeClient: CinnyClient) {
        client = activeClient
        _isAuthenticated.value = true
        activeClient.registerVerificationListener(object : VerificationListener {
            override fun onPhaseChanged(phase: VerificationPhase) {
                _verificationPhase.value = phase
            }
        })
        refreshCryptoStatus()
        startSyncLoop(activeClient)
    }

    fun resumeSync() {
        val activeClient = client ?: return
        if (syncRunning) return
        scope.launch {
            startSyncLoop(activeClient)
        }
    }

    fun pauseSync() {
        val activeClient = client ?: return
        syncRunning = false
        scope.launch {
            activeClient.pauseSync()
        }
    }

    fun logout() {
        scope.launch {
            runCatching { client?.logout() }
            handleLocalSignOut()
        }
    }

    private fun handleLocalSignOut() {
        syncRunning = false
        client = null
        secureStore.clearSession()
        _isAuthenticated.value = false
        _activeRoom.value = null
        _activeThreadRoot.value = null
        _messages.value = emptyList()
        _threadMessages.value = emptyList()
        _spaces.value = emptyList()
        _rooms.value = emptyList()
        _syncState.value = SyncConnectionState.Offline
    }

    private suspend fun startSyncLoop(activeClient: CinnyClient) {
        syncRunning = true
        activeClient.startSync(object : SyncObserver {
            override fun onSyncState(state: SyncConnectionState) {
                _syncState.value = state
                if (state is SyncConnectionState.TokenInvalid) {
                    _authError.value = state.message
                    handleLocalSignOut()
                }
            }

            override fun onSpacesUpdated(spaces: List<SpaceNode>, rooms: List<RoomSummary>) {
                _spaces.value = spaces
                _rooms.value = rooms
            }
        })
    }

    fun openRoomById(roomId: String) {
        val found = _rooms.value.find { it.roomId == roomId } ?: return
        openRoom(found)
    }

    fun openRoom(room: RoomSummary) {
        _activeRoom.value = room
        _activeThreadRoot.value = null
        _threadMessages.value = emptyList()
        _roomSearchResults.value = emptyList()
        scope.launch {
            client?.subscribeRoomTimeline(
                roomId = room.roomId,
                listener = object : TimelineListener {
                    override fun onTimelineUpdated(roomId: String, messages: List<TimelineMessage>) {
                        if (_activeRoom.value?.roomId == roomId) {
                            _messages.value = messages
                        }
                    }
                }
            )
            runCatching { client?.markRoomAsRead(room.roomId) }
        }
    }

    fun closeRoom() {
        val roomId = _activeRoom.value?.roomId
        val rootId = _activeThreadRoot.value?.eventId
        if (roomId != null && rootId != null) {
            scope.launch { runCatching { client?.closeThreadTimeline(roomId, rootId) } }
        }
        _activeRoom.value = null
        _activeThreadRoot.value = null
        _messages.value = emptyList()
        _threadMessages.value = emptyList()
        _roomSearchResults.value = emptyList()
    }

    fun openThread(rootMessage: TimelineMessage) {
        val roomId = _activeRoom.value?.roomId ?: return
        _activeThreadRoot.value = rootMessage
        scope.launch {
            client?.subscribeThreadTimeline(
                roomId = roomId,
                rootEventId = rootMessage.eventId,
                listener = object : TimelineListener {
                    override fun onTimelineUpdated(roomId: String, messages: List<TimelineMessage>) {
                        if (_activeRoom.value?.roomId == roomId &&
                            _activeThreadRoot.value?.eventId == rootMessage.eventId
                        ) {
                            _threadMessages.value = messages
                        }
                    }
                }
            )
        }
    }

    fun closeThread() {
        val roomId = _activeRoom.value?.roomId
        val rootId = _activeThreadRoot.value?.eventId
        _activeThreadRoot.value = null
        _threadMessages.value = emptyList()
        if (roomId != null && rootId != null) {
            scope.launch { runCatching { client?.closeThreadTimeline(roomId, rootId) } }
        }
    }

    fun sendThreadReply(markdown: String) {
        val roomId = _activeRoom.value?.roomId ?: return
        val rootId = _activeThreadRoot.value?.eventId ?: return
        scope.launch {
            runCatching { client?.sendThreadReply(roomId, rootId, markdown) }
                .onFailure { _actionError.value = it.message }
        }
    }

    fun paginateOlderMessages(limit: UShort = 30u) {
        val roomId = _activeRoom.value?.roomId ?: return
        scope.launch {
            runCatching { client?.paginateBackwards(roomId, limit) }
        }
    }

    fun sendMessage(markdown: String, replyToEventId: String? = null) {
        val roomId = _activeRoom.value?.roomId ?: return
        scope.launch {
            runCatching { client?.sendMarkdownMessage(roomId, markdown, replyToEventId) }
                .onFailure { _actionError.value = it.message }
            runCatching { client?.markRoomAsRead(roomId) }
        }
    }

    fun sendAttachmentUri(uri: Uri) {
        val roomId = _activeRoom.value?.roomId ?: return
        scope.launch {
            try {
                val resolver = context.contentResolver
                val mime = resolver.getType(uri) ?: "application/octet-stream"
                var filename = "attachment.bin"
                resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) filename = cursor.getString(idx) ?: filename
                    }
                }
                val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: throw IllegalStateException("Cannot read selected file")
                client?.sendMediaAttachment(roomId, filename, mime, bytes)
            } catch (err: Exception) {
                _actionError.value = err.message ?: "Attachment upload failed"
            }
        }
    }

    fun downloadMedia(eventId: String, mediaSourceJson: String, filename: String, onReady: ((String) -> Unit)? = null) {
        scope.launch {
            runCatching {
                client?.downloadMediaToCache(
                    mediaSourceJson = mediaSourceJson,
                    filename = filename,
                    cacheDir = context.cacheDir.absolutePath
                )
            }.onSuccess { localPath ->
                if (localPath != null) {
                    _downloadedMediaPaths.value = _downloadedMediaPaths.value + (eventId to localPath)
                    onReady?.invoke(localPath)
                }
            }.onFailure {
                _actionError.value = it.message ?: "Failed to decrypt/download media"
            }
        }
    }

    fun searchRoomMessages(query: String) {
        val roomId = _activeRoom.value?.roomId ?: return
        if (query.isBlank()) {
            _roomSearchResults.value = emptyList()
            return
        }
        scope.launch {
            runCatching { client?.searchRoomMessages(roomId, query) }
                .onSuccess { results -> _roomSearchResults.value = results.orEmpty() }
        }
    }

    fun editMessage(eventId: String, newMarkdown: String) {
        val roomId = _activeRoom.value?.roomId ?: return
        scope.launch {
            runCatching { client?.editMarkdownMessage(roomId, eventId, newMarkdown) }
        }
    }

    fun redactMessage(eventId: String) {
        val roomId = _activeRoom.value?.roomId ?: return
        scope.launch {
            runCatching { client?.redactMessage(roomId, eventId, null) }
        }
    }

    fun toggleReaction(eventId: String, emoji: String) {
        val roomId = _activeRoom.value?.roomId ?: return
        scope.launch {
            runCatching { client?.toggleReaction(roomId, eventId, emoji) }
        }
    }

    // --- Spaces & Room Management ---

    fun loadSpaceHierarchy(spaceId: String) {
        scope.launch {
            runCatching { client?.browseSpaceHierarchy(spaceId) }
                .onSuccess { list -> _spaceHierarchy.value = list.orEmpty() }
                .onFailure { _actionError.value = it.message }
        }
    }

    fun createRoom(name: String, topic: String?, isDirect: Boolean, isEncrypted: Boolean, inviteIds: List<String>) {
        scope.launch {
            runCatching {
                client?.createRoom(
                    name = name,
                    topic = topic,
                    isDirect = isDirect,
                    isEncrypted = isEncrypted,
                    inviteUserIds = inviteIds
                )
            }.onFailure { _actionError.value = it.message }
        }
    }

    fun joinRoom(roomIdOrAlias: String) {
        scope.launch {
            runCatching { client?.joinRoom(roomIdOrAlias) }
                .onFailure { _actionError.value = it.message }
        }
    }

    fun acceptInvite(roomId: String) {
        scope.launch {
            runCatching { client?.acceptInvite(roomId) }
                .onFailure { _actionError.value = it.message }
        }
    }

    fun leaveRoom(roomId: String) {
        scope.launch {
            runCatching { client?.leaveRoom(roomId) }
                .onSuccess {
                    if (_activeRoom.value?.roomId == roomId) {
                        closeRoom()
                    }
                }
                .onFailure { _actionError.value = it.message }
        }
    }

    fun inviteUser(roomId: String, userId: String) {
        scope.launch {
            runCatching { client?.inviteUser(roomId, userId) }
                .onFailure { _actionError.value = it.message }
        }
    }

    fun loadRoomMembers(roomId: String) {
        scope.launch {
            runCatching { client?.listRoomMembers(roomId) }
                .onSuccess { members -> _roomMembers.value = members.orEmpty() }
        }
    }

    // --- E2EE Devices, SAS & 4S Backup ---

    fun loadOwnDevices() {
        scope.launch {
            runCatching { client?.listOwnDevices() }
                .onSuccess { list -> if (list != null) _devices.value = list }
            refreshCryptoStatus()
        }
    }

    fun refreshCryptoStatus() {
        scope.launch {
            val status = client?.getSecretBackupStatus()
            _secretBackupStatus.value = status
        }
    }

    fun recoverWithSecurityKey(recoveryKey: String) {
        scope.launch {
            _cryptoNotice.value = null
            runCatching { client?.recoverWithSecurityKey(recoveryKey) }
                .onSuccess { status ->
                    _secretBackupStatus.value = status
                    _cryptoNotice.value = "4S cross-signing and room key backup restored."
                }
                .onFailure { err ->
                    _cryptoNotice.value = err.message ?: "Recovery failed"
                }
        }
    }

    fun enableRecoveryAndBackup() {
        scope.launch {
            _cryptoNotice.value = null
            runCatching { client?.enableRecoveryAndBackup() }
                .onSuccess { key ->
                    _generatedRecoveryKey.value = key
                    refreshCryptoStatus()
                }
                .onFailure { err ->
                    _cryptoNotice.value = err.message ?: "Backup setup failed"
                }
        }
    }

    fun startSasVerification(userId: String, deviceId: String) {
        scope.launch {
            client?.verifyDeviceSas(
                userId = userId,
                deviceId = deviceId,
                listener = object : VerificationListener {
                    override fun onPhaseChanged(phase: VerificationPhase) {
                        _verificationPhase.value = phase
                    }
                }
            )
        }
    }

    fun acceptIncomingVerification(flowId: String) {
        scope.launch {
            runCatching { client?.acceptVerificationRequest(flowId) }
        }
    }

    fun confirmSas(flowId: String) {
        scope.launch {
            runCatching { client?.confirmSasVerification(flowId) }
        }
    }

    fun cancelSas(flowId: String) {
        scope.launch {
            runCatching { client?.cancelSasVerification(flowId) }
            _verificationPhase.value = null
        }
    }

    fun clearVerificationUi() {
        _verificationPhase.value = null
        _cryptoNotice.value = null
    }

    // --- UnifiedPush Registration ---

    fun onUnifiedPushEndpointUpdated(endpoint: String?) {
        _pushEndpoint.value = endpoint
        secureStore.savePushConfig(endpoint, _pushGatewayUrl.value)
        if (!endpoint.isNullOrBlank()) {
            registerHttpPusher(endpoint, _pushGatewayUrl.value)
        }
    }

    fun registerHttpPusher(pushKey: String, gatewayUrl: String) {
        scope.launch {
            secureStore.savePushConfig(pushKey, gatewayUrl)
            _pushGatewayUrl.value = gatewayUrl
            runCatching {
                client?.registerHttpPusher(
                    pushKey = pushKey,
                    gatewayUrl = gatewayUrl,
                    appId = "in.cinny.android",
                    appDisplayName = "Cinny Android",
                    deviceDisplayName = android.os.Build.MODEL ?: "Android"
                )
            }.onSuccess {
                _pushRegistrationStatus.value = "Push gateway active"
            }.onFailure { err ->
                _pushRegistrationStatus.value = err.message ?: "Push registration failed"
            }
        }
    }
}
