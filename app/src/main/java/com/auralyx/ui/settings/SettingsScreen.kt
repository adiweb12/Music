package com.auralyx.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.auralyx.ui.components.GradientBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, vm: SettingsViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()

    GradientBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Settings") },
                    navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                SectionLabel("Appearance")
                ToggleItem(Icons.Outlined.DarkMode, "Dark Theme", "Use dark colour scheme", state.darkTheme, vm::toggleDarkTheme)
                HorizontalDivider(Modifier.padding(vertical = 8.dp))

                SectionLabel("Playback")
                ToggleItem(Icons.Outlined.Videocam, "Default Video Mode", "Show video for .aD17 files by default", state.defaultVideoMode, vm::toggleDefaultVideoMode)
                ToggleItem(Icons.Outlined.TravelExplore, "Scan on Launch", "Auto-scan storage on start", state.scanOnLaunch, vm::toggleScanOnLaunch)
                HorizontalDivider(Modifier.padding(vertical = 8.dp))

                SectionLabel("Storage")
                ActionItem(
                    icon     = Icons.Default.Search,
                    title    = "Scan Storage",
                    subtitle = if (state.isScanning) "Scanning…" else (state.scanMessage ?: "Find music and .aD17 files"),
                    onClick  = { vm.scanStorage() },
                    loading  = state.isScanning
                )
                HorizontalDivider(Modifier.padding(vertical = 8.dp))

                SectionLabel("About")
                ListItem(
                    headlineContent   = { Text("Auralyx Player") },
                    supportingContent = { Text("Version 1.0.0") },
                    leadingContent    = { Icon(Icons.Outlined.Info, null, tint = MaterialTheme.colorScheme.primary) }
                )
                ListItem(
                    headlineContent   = { Text("Supported Formats") },
                    supportingContent = { Text("MP3 • MP4 • AAC • .aD17") },
                    leadingContent    = { Icon(Icons.Outlined.AudioFile, null, tint = MaterialTheme.colorScheme.primary) }
                )
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable private fun SectionLabel(title: String) {
    Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
}

@Composable private fun ToggleItem(icon: ImageVector, title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent   = { Text(title) },
        supportingContent = { Text(subtitle, style = MaterialTheme.typography.bodySmall) },
        leadingContent    = { Icon(icon, null, tint = MaterialTheme.colorScheme.primary) },
        trailingContent   = { Switch(checked = checked, onCheckedChange = onCheckedChange) }
    )
}

@Composable private fun ActionItem(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit, loading: Boolean = false) {
    ListItem(
        modifier          = if (!loading) Modifier.clickable(onClick = onClick) else Modifier,
        headlineContent   = { Text(title) },
        supportingContent = { Text(subtitle, style = MaterialTheme.typography.bodySmall) },
        leadingContent    = { Icon(icon, null, tint = MaterialTheme.colorScheme.primary) },
        trailingContent   = if (loading) {{ CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp) }} else null
    )
}
