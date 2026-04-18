package com.auralyx.ui.home

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.auralyx.domain.model.MediaItem
import com.auralyx.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToPlayer: () -> Unit,
    onNavigateToSettings: () -> Unit,
    vm: HomeViewModel = hiltViewModel()
) {
    val state   by vm.uiState.collectAsState()
    val player  by vm.playerState.collectAsState()

    GradientBackground {
        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text       = "Auralyx",
                            style      = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color      = MaterialTheme.colorScheme.primary
                        )
                    },
                    actions = {
                        if (state.isScanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp).padding(end = 8.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            IconButton(onClick = { vm.scanStorage() }) {
                                Icon(Icons.Default.Refresh, "Scan")
                            }
                        }
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Outlined.Settings, "Settings")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = androidx.compose.ui.graphics.Color.Transparent
                    )
                )
            }
        ) { padding ->
            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            } else {
                LazyColumn(
                    modifier            = Modifier.fillMaxSize().padding(padding),
                    contentPadding      = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // ── Greeting ─────────────────────────────────────────
                    item {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            Text("Good ${timeOfDayGreeting()}", style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("What's playing today?", style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold)
                        }
                    }

                    // ── Quick Shuffle Banner ──────────────────────────────
                    if (state.allSongs.isNotEmpty()) {
                        item {
                            ShuffleBanner(
                                songCount = state.allSongs.size,
                                onShuffle = {
                                    vm.play(
                                        state.allSongs.random(),
                                        state.allSongs.shuffled()
                                    )
                                    onNavigateToPlayer()
                                }
                            )
                        }
                    }

                    // ── Recently Played ───────────────────────────────────
                    if (state.recentlyPlayed.isNotEmpty()) {
                        item {
                            SectionHeader(title = "Recently Played", actionLabel = "See All")
                        }
                        item {
                            HorizontalCarousel(
                                items     = state.recentlyPlayed,
                                onItemClick = { item ->
                                    vm.play(item, state.recentlyPlayed)
                                    onNavigateToPlayer()
                                }
                            )
                        }
                    }

                    // ── Music Videos (.aD17) ──────────────────────────────
                    if (state.musicVideos.isNotEmpty()) {
                        item {
                            SectionHeader(title = "Music Videos", actionLabel = "See All")
                        }
                        item {
                            HorizontalCarousel(
                                items     = state.musicVideos,
                                onItemClick = { item ->
                                    vm.play(item, state.musicVideos, videoEnabled = true)
                                    onNavigateToPlayer()
                                }
                            )
                        }
                    }

                    // ── All Songs ─────────────────────────────────────────
                    if (state.allSongs.isNotEmpty()) {
                        item { SectionHeader(title = "Songs") }
                        items(state.allSongs.take(8), key = { it.id }) { song ->
                            MediaListItem(
                                item      = song,
                                isPlaying = player.currentItem?.id == song.id && player.isPlaying,
                                onClick   = {
                                    vm.play(song, state.allSongs)
                                    onNavigateToPlayer()
                                }
                            )
                        }
                    }

                    // ── Empty state ───────────────────────────────────────
                    if (state.allSongs.isEmpty() && state.musicVideos.isEmpty()) {
                        item {
                            EmptyState(onScan = { vm.scanStorage() })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShuffleBanner(songCount: Int, onShuffle: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onShuffle),
        shape  = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.tertiary
                        )
                    )
                )
                .padding(20.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Shuffle, null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Shuffle Play", style = MaterialTheme.typography.titleMedium, color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.Bold)
                    Text("$songCount songs", style = MaterialTheme.typography.bodySmall, color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.8f))
                }
            }
        }
    }
}

@Composable
private fun HorizontalCarousel(items: List<MediaItem>, onItemClick: (MediaItem) -> Unit) {
    LazyRow(
        contentPadding      = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items, key = { it.id }) { item ->
            MediaCard(item = item, onClick = { onItemClick(item) })
        }
    }
}

@Composable
private fun EmptyState(onScan: () -> Unit) {
    Column(
        modifier              = Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment   = Alignment.CenterHorizontally,
        verticalArrangement   = Arrangement.spacedBy(16.dp)
    ) {
        Icon(Icons.Default.MusicNote, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
        Text("No music found", style = MaterialTheme.typography.headlineSmall)
        Text("Tap below to scan your device storage", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(onClick = onScan) {
            Icon(Icons.Default.Search, null)
            Spacer(Modifier.width(8.dp))
            Text("Scan Storage")
        }
    }
}

private fun timeOfDayGreeting(): String {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when {
        hour < 12 -> "morning,"
        hour < 17 -> "afternoon,"
        else      -> "evening,"
    }
}
