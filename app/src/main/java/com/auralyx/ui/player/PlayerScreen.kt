package com.auralyx.ui.player

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.os.PowerManager
import android.view.WindowManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.auralyx.domain.model.MediaItem
import com.auralyx.domain.model.RepeatMode
import com.auralyx.ui.components.AD17ThumbnailImage
import com.auralyx.ui.components.AlbumArt
import com.auralyx.ui.components.PlayingBars
import com.auralyx.ui.theme.PurpleAccent
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    onBack: () -> Unit,
    vm: PlayerViewModel = hiltViewModel()
) {
    val state   by vm.state.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity

    // ── Wake lock: keep screen on while video is playing ──────────────────
    DisposableEffect(state.isVideoEnabled && state.isPlaying) {
        if (state.isVideoEnabled && state.isPlaying) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // ── Decide layout based on mode ───────────────────────────────────────
    val isVideo = state.isVideoEnabled && state.currentItem?.isAD17 == true

    if (isVideo) {
        VideoPlayerLayout(state, vm, activity, onBack)
    } else {
        AudioPlayerLayout(state, vm, onBack)
    }
}

// ════════════════════════════════════════════════════════════════════════════
// VIDEO LAYOUT — fullscreen with overlaid controls, tap to show/hide
// ════════════════════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VideoPlayerLayout(
    state: com.auralyx.domain.model.PlayerState,
    vm: PlayerViewModel,
    activity: Activity?,
    onBack: () -> Unit
) {
    var controlsVisible by remember { mutableStateOf(true) }
    var isFullscreen    by remember { mutableStateOf(false) }

    // Auto-hide controls after 3 s of no interaction
    LaunchedEffect(controlsVisible, state.isPlaying) {
        if (controlsVisible && state.isPlaying) {
            delay(3000)
            controlsVisible = false
        }
    }

    // Lock/unlock orientation for fullscreen
    DisposableEffect(isFullscreen) {
        activity?.requestedOrientation = if (isFullscreen)
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        else
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures { controlsVisible = !controlsVisible }
            }
    ) {
        // ── ExoPlayer surface fills entire screen ─────────────────────────
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player               = vm.player.exoPlayer
                    useController        = false
                    resizeMode           = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    setBackgroundColor(android.graphics.Color.BLACK)
                }
            },
            modifier = Modifier.fillMaxSize(),
            update   = { pv ->
                pv.player = vm.player.exoPlayer
            }
        )

        // ── Overlay controls (animated fade in/out) ───────────────────────
        AnimatedVisibility(
            visible       = controlsVisible,
            enter         = fadeIn(tween(220)),
            exit          = fadeOut(tween(220))
        ) {
            VideoControlsOverlay(
                state        = state,
                vm           = vm,
                isFullscreen = isFullscreen,
                onBack       = onBack,
                onToggleFullscreen = { isFullscreen = !isFullscreen }
            )
        }
    }
}

@Composable
private fun VideoControlsOverlay(
    state: com.auralyx.domain.model.PlayerState,
    vm: PlayerViewModel,
    isFullscreen: Boolean,
    onBack: () -> Unit,
    onToggleFullscreen: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f   to Color.Black.copy(alpha = 0.65f),
                    0.3f to Color.Transparent,
                    0.7f to Color.Transparent,
                    1f   to Color.Black.copy(alpha = 0.8f)
                )
            )
    ) {
        // ── TOP BAR ───────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBackIosNew, "Back", tint = Color.White)
            }
            Column(Modifier.weight(1f).padding(horizontal = 4.dp)) {
                Text(
                    text       = state.currentItem?.title ?: "",
                    style      = MaterialTheme.typography.titleMedium,
                    color      = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis
                )
                Text(
                    text  = state.currentItem?.displayArtist ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            // Audio mode toggle
            IconButton(onClick = { vm.toggleVideo() }) {
                Icon(Icons.Outlined.MusicNote, "Switch to audio", tint = Color.White)
            }
            // Fullscreen toggle
            IconButton(onClick = onToggleFullscreen) {
                Icon(
                    imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                    contentDescription = "Fullscreen",
                    tint = Color.White
                )
            }
        }

        // ── CENTER PLAY/PAUSE + SKIP ──────────────────────────────────────
        Row(
            modifier              = Modifier.align(Alignment.Center),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            // Skip -10s
            VideoSeekButton(
                icon     = Icons.Rounded.Replay10,
                label    = "-10s",
                onClick  = { vm.seekTo((state.progress - 10_000).coerceAtLeast(0)) }
            )
            // Prev
            VideoControlButton(Icons.Default.SkipPrevious, "Prev", 48.dp) { vm.skipToPrev() }
            // Play / Pause
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(0.2f))
                    .clickable { vm.togglePlayPause() },
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState   = state.isPlaying,
                    transitionSpec = { scaleIn(tween(180)) togetherWith scaleOut(tween(180)) },
                    label         = "vpp"
                ) { playing ->
                    if (state.isBuffering) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(32.dp), strokeWidth = 3.dp)
                    } else {
                        Icon(
                            imageVector = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }
            }
            // Next
            VideoControlButton(Icons.Default.SkipNext, "Next", 48.dp) { vm.skipToNext() }
            // Forward +10s
            VideoSeekButton(
                icon    = Icons.Rounded.Forward10,
                label   = "+10s",
                onClick = { vm.seekTo((state.progress + 10_000).coerceAtMost(state.duration)) }
            )
        }

        // ── BOTTOM — seekbar + time ───────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Time labels
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text(formatMs(state.progress), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.85f))
                Text(state.currentItem?.durationFormatted ?: "--:--", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.85f))
            }
            Slider(
                value         = state.progressFraction,
                onValueChange = { vm.seekToFraction(it) },
                colors        = SliderDefaults.colors(
                    thumbColor         = Color.White,
                    activeTrackColor   = PurpleAccent,
                    inactiveTrackColor = Color.White.copy(0.3f)
                ),
                modifier = Modifier.fillMaxWidth()
            )
            // Bottom row: shuffle, repeat
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                IconButton(onClick = { vm.toggleShuffle() }) {
                    Icon(Icons.Default.Shuffle, null, tint = if (state.shuffleEnabled) PurpleAccent else Color.White.copy(0.6f), modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = { vm.cycleRepeatMode() }) {
                    Icon(
                        imageVector = when (state.repeatMode) { RepeatMode.ONE -> Icons.Default.RepeatOne; else -> Icons.Default.Repeat },
                        contentDescription = null,
                        tint = if (state.repeatMode != RepeatMode.OFF) PurpleAccent else Color.White.copy(0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun VideoControlButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, size: androidx.compose.ui.unit.Dp, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(size)) {
        Icon(icon, label, tint = Color.White, modifier = Modifier.fillMaxSize(0.7f))
    }
}

@Composable
private fun VideoSeekButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = onClick, modifier = Modifier.size(44.dp)) {
            Icon(icon, label, tint = Color.White, modifier = Modifier.size(30.dp))
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
// AUDIO LAYOUT — album art fades as user scrolls to Up Next queue
// ════════════════════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AudioPlayerLayout(
    state: com.auralyx.domain.model.PlayerState,
    vm: PlayerViewModel,
    onBack: () -> Unit
) {
    val scrollState = rememberLazyListState()

    // Album art alpha fades only when user scrolls UP to see the queue
    val artAlpha by remember {
        derivedStateOf {
            val first = scrollState.layoutInfo.visibleItemsInfo.firstOrNull()
                ?: return@derivedStateOf 1f
            val scrollPx = -first.offset.toFloat()
            // Start fading after 80dp scroll, fully gone at 320dp
            (1f - ((scrollPx - 80f) / 240f)).coerceIn(0f, 1f)
        }
    }

    val artAlphaAnimated by animateFloatAsState(artAlpha, tween(80), label = "aa")

    LaunchedEffect(state.currentItem?.id) {
        state.currentItem?.id?.let { vm.updateLastPlayed(it) }
    }

    Box(Modifier.fillMaxSize()) {
        // ── Blurred background pulled from album art ──────────────────────
        val artUri = state.currentItem?.albumArtUri
        if (artUri != null) {
            AsyncImage(
                model            = artUri,
                contentDescription = null,
                modifier         = Modifier.fillMaxSize().blur(70.dp).alpha(0.28f),
                contentScale     = ContentScale.Crop
            )
        } else if (state.currentItem?.isAD17 == true) {
            // For aD17 without URI, try bitmap-based blur background
            AD17ThumbnailImage(
                path     = state.currentItem!!.path,
                modifier = Modifier.fillMaxSize().blur(70.dp).alpha(0.28f)
            )
        }
        // Gradient overlay keeps readability
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(
                    MaterialTheme.colorScheme.background.copy(0.72f),
                    MaterialTheme.colorScheme.background.copy(0.92f),
                    MaterialTheme.colorScheme.background
                ))
            )
        )

        Column(Modifier.fillMaxSize()) {
            // ── App bar ───────────────────────────────────────────────────
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.KeyboardArrowDown, "Close", modifier = Modifier.size(34.dp))
                    }
                },
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text("NOW PLAYING", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 2.sp)
                    }
                },
                actions = {
                    if (state.currentItem?.isAD17 == true) {
                        IconButton(onClick = { vm.toggleVideo() }) {
                            Icon(Icons.Outlined.Videocam, "Video mode",
                                tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    IconButton(onClick = { /* TODO options */ }) {
                        Icon(Icons.Default.MoreVert, "More")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )

            LazyColumn(
                state          = scrollState,
                modifier       = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                // ── 1. Album art ──────────────────────────────────────────
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 28.dp, vertical = 8.dp)
                            .alpha(artAlphaAnimated),
                        contentAlignment = Alignment.Center
                    ) {
                        // Outer glow ring
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(24.dp))
                                .background(
                                    Brush.radialGradient(listOf(
                                        MaterialTheme.colorScheme.primary.copy(0.15f),
                                        Color.Transparent
                                    ))
                                )
                        )
                        Card(
                            modifier  = Modifier.fillMaxWidth().aspectRatio(1f),
                            shape     = RoundedCornerShape(22.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 28.dp)
                        ) {
                            if (state.currentItem?.isAD17 == true) {
                                AD17ThumbnailImage(
                                    path     = state.currentItem!!.path,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                AlbumArt(
                                    uri      = state.currentItem?.albumArtUri,
                                    isVideo  = false,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }

                // ── 2. Track info + heart ─────────────────────────────────
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text       = state.currentItem?.title ?: "Nothing playing",
                                style      = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines   = 1,
                                overflow   = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text     = state.currentItem?.displayArtist ?: "Unknown Artist",
                                style    = MaterialTheme.typography.bodyLarge,
                                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        if (state.currentItem?.isAD17 == true) {
                            Surface(color = PurpleAccent.copy(0.18f), shape = RoundedCornerShape(10.dp)) {
                                Row(Modifier.padding(horizontal = 9.dp, vertical = 5.dp), Arrangement.spacedBy(4.dp), Alignment.CenterVertically) {
                                    Icon(Icons.Default.Videocam, null, tint = PurpleAccent, modifier = Modifier.size(13.dp))
                                    Text("aD17", style = MaterialTheme.typography.labelSmall, color = PurpleAccent, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // ── 3. Progress seekbar ───────────────────────────────────
                item {
                    Column(Modifier.padding(horizontal = 24.dp)) {
                        Slider(
                            value         = state.progressFraction,
                            onValueChange = { vm.seekToFraction(it) },
                            colors        = SliderDefaults.colors(
                                thumbColor         = MaterialTheme.colorScheme.primary,
                                activeTrackColor   = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(0.18f)
                            )
                        )
                        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), Arrangement.SpaceBetween) {
                            Text(formatMs(state.progress), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(state.currentItem?.durationFormatted ?: "--:--", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // ── 4. Controls ───────────────────────────────────────────
                item {
                    AudioControls(state = state, vm = vm)
                }

                // ── 5. Volume slider ──────────────────────────────────────
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.VolumeDown, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                        Slider(
                            value         = 0.75f,
                            onValueChange = { /* volume handled by AudioManager if desired */ },
                            modifier      = Modifier.weight(1f),
                            colors        = SliderDefaults.colors(
                                thumbColor         = MaterialTheme.colorScheme.onSurfaceVariant,
                                activeTrackColor   = MaterialTheme.colorScheme.onSurfaceVariant,
                                inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(0.15f)
                            )
                        )
                        Icon(Icons.Default.VolumeUp, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    }
                }

                // ── 6. Up Next divider ────────────────────────────────────
                item {
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(0.4f), modifier = Modifier.padding(horizontal = 28.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Up Next", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "${state.queue.size - state.queueIndex - 1} songs",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // ── 7. Queue items ────────────────────────────────────────
                val upNext = state.queue.drop(state.queueIndex + 1)
                itemsIndexed(upNext, key = { _, it -> it.id }) { i, item ->
                    QueueRow(item = item, position = i + 1, onClick = {})
                }

                if (upNext.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(32.dp), Alignment.Center) {
                            Text("Queue is empty", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AudioControls(
    state: com.auralyx.domain.model.PlayerState,
    vm: PlayerViewModel
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        // Main transport row
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            // Shuffle
            ControlIconButton(
                icon    = Icons.Default.Shuffle,
                active  = state.shuffleEnabled,
                size    = 44.dp,
                iconSize = 22.dp,
                onClick = { vm.toggleShuffle() }
            )
            // Skip prev
            ControlIconButton(
                icon     = Icons.Default.SkipPrevious,
                active   = false,
                size     = 56.dp,
                iconSize = 34.dp,
                onClick  = { vm.skipToPrev() },
                tint     = MaterialTheme.colorScheme.onSurface
            )
            // Play/Pause FAB
            val playScale by animateFloatAsState(
                targetValue   = if (state.isPlaying) 1f else 0.92f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label         = "playScale"
            )
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .scale(playScale)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                        )
                    )
                    .clickable { vm.togglePlayPause() },
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState   = state.isPlaying,
                    transitionSpec = { scaleIn(tween(160)) togetherWith scaleOut(tween(160)) },
                    label         = "pp"
                ) { playing ->
                    if (state.isBuffering) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                    } else {
                        Icon(
                            imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint     = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }
            // Skip next
            ControlIconButton(
                icon     = Icons.Default.SkipNext,
                active   = false,
                size     = 56.dp,
                iconSize = 34.dp,
                onClick  = { vm.skipToNext() },
                tint     = MaterialTheme.colorScheme.onSurface
            )
            // Repeat
            ControlIconButton(
                icon     = when (state.repeatMode) { RepeatMode.ONE -> Icons.Default.RepeatOne; else -> Icons.Default.Repeat },
                active   = state.repeatMode != RepeatMode.OFF,
                size     = 44.dp,
                iconSize = 22.dp,
                onClick  = { vm.cycleRepeatMode() }
            )
        }
    }
}

@Composable
private fun ControlIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    active: Boolean,
    size: androidx.compose.ui.unit.Dp,
    iconSize: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
    tint: Color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
) {
    val scale by animateFloatAsState(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "cs")
    IconButton(onClick = onClick, modifier = Modifier.size(size)) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(iconSize))
    }
}

@Composable
private fun QueueRow(item: MediaItem, position: Int, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        overlineContent = {
            Text("$position", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        headlineContent = {
            Text(item.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = {
            Text("${item.displayArtist} \u2022 ${item.durationFormatted}", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        leadingContent = {
            Box(Modifier.size(46.dp)) {
                if (item.isAD17) {
                    AD17ThumbnailImage(item.path, Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)))
                } else {
                    AlbumArt(item.albumArtUri, false, Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)))
                }
            }
        }
    )
    HorizontalDivider(Modifier.padding(start = 72.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(0.3f))
}

private fun formatMs(ms: Long): String {
    val s = ms / 1000
    return if (s >= 3600) "%d:%02d:%02d".format(s/3600, (s%3600)/60, s%60)
    else "%d:%02d".format(s/60, s%60)
}
