package `in`.cinny.android.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import `in`.cinny.android.ui.theme.CinnyThemeColors
import `in`.cinny.android.ui.theme.colorMXID

@Composable
fun MxcAvatar(
    name: String,
    seedId: String,
    mxcUrl: String?,
    mxcCachePaths: Map<String, String>,
    onRequestMxc: (String?) -> Unit,
    size: Dp = 36.dp,
    cornerRadius: Dp = 18.dp,
    modifier: Modifier = Modifier
) {
    val colors = CinnyThemeColors.current
    val cleanMxc = mxcUrl?.trim()
    LaunchedEffect(cleanMxc) {
        if (!cleanMxc.isNullOrBlank() && cleanMxc.startsWith("mxc://") && !mxcCachePaths.containsKey(cleanMxc)) {
            onRequestMxc(cleanMxc)
        }
    }

    val cachedPath = if (!cleanMxc.isNullOrBlank()) mxcCachePaths[cleanMxc] else null
    val bitmap = remember(cachedPath) {
        if (!cachedPath.isNullOrBlank()) {
            runCatching { BitmapFactory.decodeFile(cachedPath) }.getOrNull()
        } else {
            null
        }
    }

    val accent = colorMXID(seedId, colors.isDark)
    val initials = remember(name) {
        val cleaned = name.trim().removePrefix("#").removePrefix("@")
        val parts = cleaned.split(" ", "-", "_").filter { it.isNotBlank() }
        when {
            parts.size >= 2 -> "${parts[0].first()}${parts[1].first()}".uppercase()
            cleaned.length >= 2 -> cleaned.take(2).uppercase()
            cleaned.isNotEmpty() -> cleaned.take(1).uppercase()
            else -> "?"
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(accent.copy(alpha = if (colors.isDark) 0.22f else 0.14f))
            .border(
                width = 1.dp,
                color = accent.copy(alpha = 0.35f),
                shape = RoundedCornerShape(cornerRadius)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = initials,
                style = if (size <= 28.dp) {
                    MaterialTheme.typography.labelSmall
                } else {
                    MaterialTheme.typography.labelMedium
                },
                fontWeight = FontWeight.Bold,
                color = accent,
                maxLines = 1
            )
        }
    }
}

@Composable
fun CinnySequenceCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = CinnyThemeColors.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = colors.surface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, colors.surfaceLine)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            content = content
        )
    }
}

@Composable
fun CinnySectionTitle(
    title: String,
    modifier: Modifier = Modifier
) {
    val colors = CinnyThemeColors.current
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = colors.onSurfaceMuted,
        modifier = modifier.padding(horizontal = 4.dp, vertical = 6.dp)
    )
}

@Composable
fun CinnySettingSwitchRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = CinnyThemeColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = colors.onSurface
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceMuted
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun CinnyNavTile(
    title: String,
    subtitle: String? = null,
    leading: @Composable (() -> Unit)? = null,
    trailingBadge: String? = null,
    onClick: () -> Unit
) {
    val colors = CinnyThemeColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (leading != null) {
            leading()
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (!trailingBadge.isNullOrBlank()) {
            Surface(
                color = colors.primaryContainer,
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = trailingBadge,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = colors.onSurfaceMuted,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun CinnyCountBadge(
    count: UInt,
    highlight: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (count == 0u) return
    val colors = CinnyThemeColors.current
    Surface(
        modifier = modifier,
        color = if (highlight) colors.critical else colors.primary,
        shape = CircleShape
    ) {
        Text(
            text = if (count > 99u) "99+" else count.toString(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (highlight) Color.White else colors.onPrimary,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
