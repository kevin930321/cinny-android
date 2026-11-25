package `in`.cinny.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import `in`.cinny.android.R
import `in`.cinny.android.data.CinnyRepository
import `in`.cinny.android.ui.theme.CinnyThemeColors

private val PRESET_HOMESERVERS = listOf(
    "https://matrix.org",
    "https://mozilla.org",
    "https://fedora.im"
)

@Composable
fun LoginScreen(repository: CinnyRepository) {
    val colors = CinnyThemeColors.current
    var homeserver by rememberSaveable { mutableStateOf("https://matrix.org") }
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    val authError by repository.authError.collectAsState()
    val isRestoring by repository.isRestoringSession.collectAsState()
    val isLoggingIn by repository.isLoggingIn.collectAsState()
    val capabilities by repository.loginCapabilities.collectAsState()

    LaunchedEffect(homeserver) {
        if (homeserver.startsWith("https://") && homeserver.length > 10) {
            repository.probeHomeserverCapabilities(homeserver)
        }
    }

    if (isRestoring) {
        CinnyDotPatternBackground {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CinnyBrandLogo(size = 56.dp)
                    Spacer(modifier = Modifier.height(16.dp))
                    CircularProgressIndicator(color = colors.primary)
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = stringResource(R.string.restoring_session),
                        color = colors.onSurfaceMuted
                    )
                }
            }
        }
        return
    }

    CinnyDotPatternBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CinnySequenceCard(modifier = Modifier.widthIn(max = 440.dp)) {
                // Upstream Cinny AuthHeader Strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.surfaceVariant.copy(alpha = 0.45f))
                        .padding(horizontal = 22.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    CinnyBrandLogo(size = 48.dp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.onSurface
                        )
                        Text(
                            text = stringResource(R.string.login_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceMuted
                        )
                    }
                }
                HorizontalDivider(color = colors.surfaceLine)

                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(PRESET_HOMESERVERS) { preset ->
                            FilterChip(
                                selected = homeserver.trim() == preset,
                                onClick = { homeserver = preset },
                                label = { Text(preset.removePrefix("https://")) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = homeserver,
                        onValueChange = { homeserver = it },
                        label = { Text(stringResource(R.string.homeserver_label)) },
                        singleLine = true,
                        enabled = !isLoggingIn,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text(stringResource(R.string.username_label)) },
                        placeholder = { Text("@user:matrix.org") },
                        singleLine = true,
                        enabled = !isLoggingIn,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(stringResource(R.string.password_label)) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        enabled = !isLoggingIn,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (authError != null) {
                        Surface(
                            color = colors.criticalContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = authError ?: "",
                                color = colors.critical,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    Button(
                        onClick = { repository.login(homeserver.trim(), username.trim(), password) },
                        enabled = !isLoggingIn && homeserver.isNotBlank() && username.isNotBlank() && password.isNotBlank(),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        if (isLoggingIn) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = colors.onPrimary
                            )
                        } else {
                            Text(stringResource(R.string.login_cta), fontWeight = FontWeight.SemiBold)
                        }
                    }

                    if (capabilities?.supportsSso != false) {
                        HorizontalDivider(color = colors.surfaceLine)
                        OutlinedButton(
                            onClick = { repository.startSsoLogin(homeserver.trim()) },
                            enabled = !isLoggingIn && homeserver.isNotBlank(),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            Icon(Icons.Outlined.OpenInBrowser, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.login_sso_cta))
                        }

                        val providers = capabilities?.ssoProviders.orEmpty()
                        if (providers.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                providers.take(3).forEach { idp ->
                                    FilledTonalButton(
                                        onClick = { repository.startSsoLogin(homeserver.trim(), idp.id) },
                                        modifier = Modifier.weight(1f),
                                        enabled = !isLoggingIn,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(idp.name, maxLines = 1)
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
