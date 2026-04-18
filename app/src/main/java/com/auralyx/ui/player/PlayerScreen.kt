package com.auralyx.ui.player

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.auralyx.domain.model.MediaItem
import com.auralyx.domain.model.RepeatMode
import com.auralyx.ui.components.AlbumArt
import com.auralyx.ui.theme.PurpleAccent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    onBack: () -> Unit,
    vm: PlayerViewModel = hiltViewModel()
) {
    val state   by vm.state.collectAsState()
    val scrollState = rememberLazyListState()

    // Album-art fade alpha computed from scroll offset
    val scrollOffset by remember {
        derivedStateOf {
            val first = scrollState.layoutInfo.visibleItemsInfo.firstOrNull() ?: return@derivedStateOf 0f
            (-first.offset.toFloat() / 800f).coerceIn(0f, 1f)
        }
    }
    val artAlpha by animateFloatAsState(
        targetValue   = (1f - scrollOffset * 1.8f).coerceIn(0f, 1f),
        animationSpec = tween(150),
        label         = "artAlpha"
    )

    LaunchedEffect(state.currentItem?.id) {
        state.currentItem?.id?.let { vm.updateLastPlayed(it) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Blurred background
        if (state.currentItem?.albumArtUri != null) {
            AsyncImage(
                model = state.currentItem!!.albumArtUri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize().blur(80.dp).alpha(0.3f),
                contentScale = ContentScale.Crop
            )
        }
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(
                    MaterialTheme.colorScheme.background.copy(alpha = 0.75f),
                    MaterialTheme.colorScheme.background.copy(alpha = 0.97f)
                ))
            )
        )

        Column(modifier = Modifier.fillMaxSize()) {
            // ── Top bar
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.KeyboardArrowDown, "Close", modifier = Modifier.size(32.dp))
                    }
                },
                title = {
                    Text("NOW PLAYING", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 2.sp)
                },
                actions = {
                    if (state.currentItem?.isAD17 == true) {
                        IconButton(onClick = { vm.toggleVideo() }) {
                            Icon(
                                imageVector = if (state.isVideoEnabled) Icons.Filled.Videocam else Icons.Outlined.VideocamOff,
                                contentDescription = "Video toggle",
                                tint = if (state.isVideoEnabled) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )

            LazyColumn(state = scrollState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 48.dp)) {

                // ── Album art / video
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp)
                            .padding(horizontal = 24.dp, vertical = 8.dp)
                            .alpha(artAlpha),
                        contentAlignment = Alignment.Center
                    ) {
                        if (state.isVideoEnabled && state.currentItem?.isAD17 == true) {
                            AndroidView(
                                factory = { ctx ->
                                    PlayerView(ctx).apply {
                                        player        = vm.player.exoPlayer
                                        useController = false
                                    }
                                },
                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp))
                            )
                        } else {
                            Card(
                                modifier  = Modifier.fillMaxSize(),
                                shape     = RoundedCornerShape(20.dp),
                                elevation = CardDefaults.cardElevation(24.dp)
                            ) {
                                AlbumArt(
                                    uri      = state.currentItem?.albumArtUri,
                                    isVideo  = state.currentItem?.isAD17 ?: false,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }

                // ── Track info
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text       = state.currentItem?.title ?: "Nothing playing",
                                style      = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines   = 1,
                                overflow   = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text     = state.currentItem?.displayArtist ?: "Unknown Artist",
                                style    = MaterialTheme.typography.bodyMedium,
                                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (state.currentItem?.isAD17 == true) {
                            Spacer(Modifier.width(8.dp))
                            Surface(color = PurpleAccent.copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp)) {
                                Text(".aD17", style = MaterialTheme.typography.labelSmall, color = PurpleAccent,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                            }
                        }
                    }
                }

                // ── Seekbar
                item {
                    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                        Slider(
                            value = state.progressFraction,
                            onValueChange = { vm.seekToFraction(it) },
                            colors = SliderDefaults.colors(
                                thumbColor         = MaterialTheme.colorScheme.primary,
                                activeTrackColor   = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                            )
                        )
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text(formatMs(state.progress), style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(state.currentItem?.durationFormatted ?: "--:--",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // ── Controls
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { vm.toggleShuffle() }) {
                            Icon(Icons.Default.Shuffle, "Shuffle",
                                tint = if (state.shuffleEnabled) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp))
                        }
                        IconButton(onClick = { vm.skipToPrev() }, modifier = Modifier.size(56.dp)) {
                            Icon(Icons.Default.SkipPrevious, "Prev", modifier = Modifier.size(36.dp))
                        }
                        FloatingActionButton(
                            onClick        = { vm.togglePlayPause() },
                            shape          = CircleShape,
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor   = Color.White,
                            modifier       = Modifier.size(72.dp)
                        ) {
                            AnimatedContent(targetState = state.isPlaying, label = "pp",
                                transitionSpec = { scaleIn() togetherWith scaleOut() }) { playing ->
                                if (state.isBuffering) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                                } else {
                                    Icon(
                                        imageVector = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = null, modifier = Modifier.size(36.dp)
                                    )
                                }
                            }
                        }
                        IconButton(onClick = { vm.skipToNext() }, modifier = Modifier.size(56.dp)) {
                            Icon(Icons.Default.SkipNext, "Next", modifier = Modifier.size(36.dp))
                        }
                        IconButton(onClick = { vm.cycleRepeatMode() }) {
                            Icon(
                                imageVector = when (state.repeatMode) {
                                    RepeatMode.ONE -> Icons.Default.RepeatOne
                                    else           -> Icons.Default.Repeat
                                },
                                contentDescription = "Repeat",
                                tint = if (state.repeatMode != RepeatMode.OFF) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                // ── Up Next header
                item {
                    Spacer(Modifier.height(16.dp))
                    Text("Up Next", style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
                }

                // ── Queue items
                val upNext = state.queue.drop(state.queueIndex + 1)
                itemsIndexed(upNext) { _, item ->
                    QueueItem(item = item, onClick = {})
                }
            }
        }
    }
}

@Composable
private fun QueueItem(item: MediaItem, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = {
            Text(item.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = {
            Text("${item.displayArtist} • ${item.durationFormatted}", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        leadingContent = {
            Box(Modifier.size(44.dp)) {
                AlbumArt(uri = item.albumArtUri, isVideo = item.isAD17,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)))
            }
        }
    )
    HorizontalDivider(modifier = Modifier.padding(start = 72.dp), thickness = 0.5.dp)
}

private fun formatMs(ms: Long): String {
    val s = ms / 1000
    return if (s >= 3600) "%d:%02d:%02d".format(s/3600, (s%3600)/60, s%60)
    else "%d:%02d".format(s/60, s%60)
}
