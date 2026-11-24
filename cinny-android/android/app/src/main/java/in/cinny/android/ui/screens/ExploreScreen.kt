package `in`.cinny.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Search
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

private val DIRECTORY_SERVERS = listOf("", "matrix.org", "mozilla.org", "gnome.org")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    repository: CinnyRepository,
    onBack: () -> Unit,
    onOpenRoom: (String) -> Unit
) {
    val colors = CinnyThemeColors.current
    val publicRooms by repository.publicRooms.collectAsState()
    val isSearching by repository.isSearchingPublicRooms.collectAsState()
    val mxcCachePaths by repository.mxcCachePaths.collectAsState()

    var searchTerm by rememberSaveable { mutableStateOf("") }
    var serverHost by rememberSaveable { mutableStateOf("matrix.org") }
    var directAlias by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(Unit) {
        if (publicRooms.isEmpty()) {
            repository.searchPublicRooms(serverHost.ifBlank { null }, null)
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Outlined.Explore, contentDescription = null, tint = colors.primary)
                        Text(stringResource(R.string.explore_title), fontWeight = FontWeight.SemiBold)
                    }
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
            item {
                CinnySequenceCard {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.explore_directory_heading),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.onSurface
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = searchTerm,
                                onValueChange = { searchTerm = it },
                                placeholder = { Text(stringResource(R.string.explore_search_hint)) },
                                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                shape = RoundedCornerShape(8.dp),
                                onClick = {
                                    repository.searchPublicRooms(
                                        server = serverHost.ifBlank { null },
                                        searchTerm = searchTerm.ifBlank { null }
                                    )
                                }
                            ) {
                                Text(stringResource(R.string.action_search))
                            }
                        }

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(DIRECTORY_SERVERS) { srv ->
                                val label = if (srv.isEmpty()) stringResource(R.string.explore_home_server) else srv
                                FilterChip(
                                    selected = serverHost == srv,
                                    onClick = {
                                        serverHost = srv
                                        repository.searchPublicRooms(
                                            server = srv.ifBlank { null },
                                            searchTerm = searchTerm.ifBlank { null }
                                        )
                                    },
                                    label = { Text(label) }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = serverHost,
                            onValueChange = { serverHost = it },
                            label = { Text(stringResource(R.string.explore_custom_server_label)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            item {
                CinnySequenceCard {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = directAlias,
                            onValueChange = { directAlias = it },
                            label = { Text("#room:matrix.org") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        FilledTonalButton(
                            enabled = directAlias.isNotBlank(),
                            shape = RoundedCornerShape(8.dp),
                            onClick = {
                                repository.joinRoom(directAlias.trim())
                                directAlias = ""
                            }
                        ) {
                            Text(stringResource(R.string.action_join))
                        }
                    }
                }
            }

            if (isSearching) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else if (publicRooms.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.explore_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceMuted
                        )
                    }
                }
            } else {
                items(publicRooms, key = { it.roomId }) { entry ->
                    CinnySequenceCard {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (entry.isJoined) {
                                        onOpenRoom(entry.roomId)
                                    } else {
                                        repository.joinRoom(entry.canonicalAlias ?: entry.roomId)
                                    }
                                }
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MxcAvatar(
                                name = entry.name,
                                seedId = entry.roomId,
                                mxcUrl = entry.avatarMxc,
                                mxcCachePaths = mxcCachePaths,
                                onRequestMxc = { repository.requestMxcThumbnail(it, 96) },
                                size = 44.dp,
                                cornerRadius = 12.dp
                            )
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = entry.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val alias = entry.canonicalAlias
                                if (!alias.isNullOrBlank()) {
                                    Text(
                                        text = alias,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                val topic = entry.topic
                                if (!topic.isNullOrBlank()) {
                                    Text(
                                        text = topic,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.onSurfaceMuted,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.Groups,
                                        contentDescription = null,
                                        tint = colors.onSurfaceMuted,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "${entry.numJoinedMembers} members",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.onSurfaceMuted
                                    )
                                }
                            }
                            if (entry.isJoined) {
                                OutlinedButton(
                                    shape = RoundedCornerShape(8.dp),
                                    onClick = { onOpenRoom(entry.roomId) }
                                ) {
                                    Text(stringResource(R.string.action_open))
                                }
                            } else {
                                Button(
                                    shape = RoundedCornerShape(8.dp),
                                    onClick = { repository.joinRoom(entry.canonicalAlias ?: entry.roomId) }
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
