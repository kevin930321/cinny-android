package `in`.cinny.android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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

@Composable
fun LoginScreen(repository: CinnyRepository) {
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
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(12.dp))
                Text("Restoring encrypted Matrix session…")
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 48.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.login_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = homeserver,
            onValueChange = { homeserver = it },
            label = { Text(stringResource(R.string.homeserver_label)) },
            singleLine = true,
            enabled = !isLoggingIn,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text(stringResource(R.string.username_label)) },
            placeholder = { Text("@user:matrix.org") },
            singleLine = true,
            enabled = !isLoggingIn,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))

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
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = authError ?: "",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = { repository.login(homeserver.trim(), username.trim(), password) },
            enabled = !isLoggingIn && homeserver.isNotBlank() && username.isNotBlank() && password.isNotBlank(),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            if (isLoggingIn) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(stringResource(R.string.login_cta))
            }
        }

        if (capabilities?.supportsSso != false) {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = { repository.startSsoLogin(homeserver.trim()) },
                enabled = !isLoggingIn && homeserver.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(Icons.Outlined.OpenInBrowser, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.login_sso_cta))
            }

            val providers = capabilities?.ssoProviders.orEmpty()
            if (providers.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    providers.take(3).forEach { idp ->
                        FilledTonalButton(
                            onClick = { repository.startSsoLogin(homeserver.trim(), idp.id) },
                            modifier = Modifier.weight(1f),
                            enabled = !isLoggingIn
                        ) {
                            Text(idp.name, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}
