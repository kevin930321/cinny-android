package `in`.cinny.android

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import `in`.cinny.android.data.ThemePreference
import `in`.cinny.android.push.PushNotificationHelper
import `in`.cinny.android.ui.CinnyViewModel
import `in`.cinny.android.ui.screens.CryptoVerifyDialog
import `in`.cinny.android.ui.screens.HomeSpaceScreen
import `in`.cinny.android.ui.screens.LoginScreen
import `in`.cinny.android.ui.screens.PushSettingsDialog
import `in`.cinny.android.ui.screens.RoomTimelineScreen
import `in`.cinny.android.ui.theme.CinnyTheme
import uniffi.cinny_core.VerificationPhase

class MainActivity : ComponentActivity() {
    private val viewModel: CinnyViewModel by viewModels()

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIncomingIntent(intent)
        ensurePostNotificationsPermission()

        setContent {
            val repository = viewModel.repository
            val themePref by repository.themePreference.collectAsState()
            val isLoggedIn by repository.isAuthenticated.collectAsState()
            val activeRoom by repository.activeRoom.collectAsState()
            val verificationPhase by repository.verificationPhase.collectAsState()
            val pendingSsoUrl by repository.pendingSsoUrl.collectAsState()
            var showVerifyDialog by rememberSaveable { mutableStateOf(false) }
            var showPushDialog by rememberSaveable { mutableStateOf(false) }

            LaunchedEffect(pendingSsoUrl) {
                val url = pendingSsoUrl ?: return@LaunchedEffect
                repository.consumePendingSsoUrl()
                CustomTabsIntent.Builder().build().launchUrl(this@MainActivity, Uri.parse(url))
            }

            val isDark = when (themePref) {
                ThemePreference.FollowSystem -> isSystemInDarkTheme()
                ThemePreference.Dark -> true
                ThemePreference.Light -> false
            }

            val effectiveShowVerify = showVerifyDialog ||
                verificationPhase is VerificationPhase.IncomingRequest

            CinnyTheme(darkTheme = isDark) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    BackHandler(enabled = activeRoom != null) {
                        repository.closeRoom()
                    }

                    when {
                        !isLoggedIn -> LoginScreen(repository = repository)
                        activeRoom != null -> RoomTimelineScreen(
                            room = activeRoom!!,
                            repository = repository,
                            onBack = { repository.closeRoom() },
                            onOpenVerify = {
                                repository.loadOwnDevices()
                                showVerifyDialog = true
                            }
                        )
                        else -> HomeSpaceScreen(
                            repository = repository,
                            onOpenVerify = {
                                repository.loadOwnDevices()
                                showVerifyDialog = true
                            },
                            onOpenPushSettings = { showPushDialog = true }
                        )
                    }

                    if (effectiveShowVerify) {
                        CryptoVerifyDialog(
                            repository = repository,
                            phase = verificationPhase,
                            onDismiss = {
                                showVerifyDialog = false
                                repository.clearVerificationUi()
                            }
                        )
                    }

                    if (showPushDialog) {
                        PushSettingsDialog(
                            repository = repository,
                            onDismiss = { showPushDialog = false }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        val data = intent?.data
        if (data != null && data.scheme == "cinny" && data.host == "sso") {
            val token = data.getQueryParameter("loginToken")
            if (!token.isNullOrBlank()) {
                viewModel.repository.completeSsoCallback(token)
            }
        }
        val pushRoomId = intent?.getStringExtra(PushNotificationHelper.EXTRA_ROOM_ID)
        if (!pushRoomId.isNullOrBlank()) {
            viewModel.repository.openRoomById(pushRoomId)
        }
    }

    private fun ensurePostNotificationsPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val status = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            if (status != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
