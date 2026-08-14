package com.sheshabiz.quickquote.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sheshabiz.quickquote.R

/**
 * Solid, full-bleed header used at the top of each main tab (Dashboard, Quotes, Invoices,
 * Customers, Settings). Uses the surface color so it reads as a distinct band above the
 * background-colored scrollable content, and reserves its own status-bar clearance so the
 * title never sits flush against the system bar.
 *
 * When [subtitle] (a greeting) is present, an extra utility row can be shown above it —
 * menu / app name / search / theme toggle / notifications — mirroring a typical app home
 * screen's top bar. Each action is optional; omitting a callback hides that icon.
 */
@Composable
fun ScreenTitleHeader(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    onMenuClick: (() -> Unit)? = null,
    onSearchClick: (() -> Unit)? = null,
    isDarkTheme: Boolean? = null,
    onThemeToggle: (() -> Unit)? = null,
    onNotificationsClick: (() -> Unit)? = null,
    notificationCount: Int = 0,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
    ) {
        val hasUtilityRow = subtitle != null &&
            (onMenuClick != null || onSearchClick != null || onThemeToggle != null || onNotificationsClick != null)
        if (hasUtilityRow) {
            UtilityRow(
                onMenuClick = onMenuClick,
                onSearchClick = onSearchClick,
                isDarkTheme = isDarkTheme,
                onThemeToggle = onThemeToggle,
                onNotificationsClick = onNotificationsClick,
                notificationCount = notificationCount
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = if (hasUtilityRow) 12.dp else 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
            if (subtitle != null) {
                Image(
                    painter = painterResource(R.drawable.logo_mark),
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.background)
                        .padding(6.dp)
                )
                Spacer(Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(2.dp))
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            HeaderLogo(modifier = Modifier.padding(start = 12.dp))
        }
    }
}

@Composable
private fun UtilityRow(
    onMenuClick: (() -> Unit)?,
    onSearchClick: (() -> Unit)?,
    isDarkTheme: Boolean?,
    onThemeToggle: (() -> Unit)?,
    onNotificationsClick: (() -> Unit)?,
    notificationCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onMenuClick != null) {
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Filled.Menu, contentDescription = "Menu")
            }
        }
        Column(modifier = Modifier.weight(1f).padding(start = if (onMenuClick != null) 0.dp else 8.dp)) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Business Suite",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (onSearchClick != null) {
            IconButton(onClick = onSearchClick) {
                Icon(Icons.Filled.Search, contentDescription = "Search")
            }
        }
        if (onThemeToggle != null) {
            IconButton(onClick = onThemeToggle) {
                Icon(
                    if (isDarkTheme == true) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                    contentDescription = "Toggle theme"
                )
            }
        }
        if (onNotificationsClick != null) {
            Box {
                IconButton(onClick = onNotificationsClick) {
                    Icon(Icons.Filled.Notifications, contentDescription = "Notifications")
                }
                if (notificationCount > 0) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .align(Alignment.TopEnd)
                            .offset(x = (-4).dp, y = 4.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (notificationCount > 9) "9+" else notificationCount.toString(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        )
                    }
                }
            }
        }
    }
}
