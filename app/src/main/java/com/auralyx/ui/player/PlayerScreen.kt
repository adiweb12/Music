package com.auralyx.ui.player

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.WindowManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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
import com.auralyx.ui.theme.PurpleAccent
import kotlinx.coroutines.delay

// ════════════════════════════════════════════════════════════════════════
// ENTRY POINT
// ════════════════════════════════════════════════════════════════════════
@Composable
fun PlayerScreen(onBack: () -> Unit, vm: PlayerViewModel = hiltViewModel()) {
    val state   by vm.state.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity

    // Keep screen ON while video is actively playing
    DisposableEffect(state.isVideoEnabled && state.isPlaying) {
        if (state.isVideoEnabled && state.isPlaying) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Track last played
    LaunchedEffect(state.currentItem?.id) {
        state.currentItem?.id?.let { vm.updateLastPlayed(it) }
    }

    val isVideoActive = state.isVideoEnabled && state.currentItem?.isAD17 == true

    if (isVideoActive) {
        VideoLayout(state = state, vm = vm, activity = activity, onBack = onBack)
    } else {
        AudioLayout(state = state, vm = vm, onBack = onBack)
    }
}

// ════════════════════════════════════════════════════════════════════════
// VIDEO LAYOUT  — full-screen video + tap-to-reveal overlay controls
// ════════════════════════════════════════════════════════════════════════
@Composable
private fun VideoLayout(
    state: com.auralyx.domain.model.PlayerState,
    vm: PlayerViewModel,
    activity: Activity?,
    onBack: () -> Unit
) {
    var showControls by remember { mutableStateOf(true) }
    var isFullscreen by remember { mutableStateOf(false) }

    // Auto-hide controls after 3 s when playing
    LaunchedEffect(showControls, state.isPlaying) {
        if (showControls && state.isPlaying) {
            delay(3_000)
            showControls = false
        }
    }

    // Lock orientation when fullscreen
    DisposableEffect(isFullscreen) {
        activity?.requestedOrientation = if (isFullscreen)
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        else
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        onDispose { activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) { detectTapGestures { showControls = !showControls } }
    ) {
        // ── ExoPlayer surface — always visible, never fades ───────────────
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player        = vm.player.exoPlayer
                    useController = false
                    resizeMode    = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    setBackgroundColor(android.graphics.Color.BLACK)
                }
            },
            modifier = Modifier.fillMaxSize(),
            update   = { it.player = vm.player.exoPlayer }
        )

        // ── Controls overlay — tap to show/hide ───────────────────────────
        AnimatedVisibility(
            visible      = showControls,
            enter        = fadeIn(tween(200)),
            exit         = fadeOut(tween(200))
        ) {
            VideoOverlay(
                state           = state,
                vm              = vm,
                isFullscreen    = isFullscreen,
                onBack          = onBack,
                onToggleFS      = { isFullscreen = !isFullscreen }
            )
        }
    }
}

// ── Overlay: top-bar + center transport + bottom seekbar ─────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VideoOverlay(
    state: com.auralyx.domain.model.PlayerState,
    vm: PlayerViewModel,
    isFullscreen: Boolean,
    onBack: () -> Unit,
    onToggleFS: () -> Unit
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0.00f to Color.Black.copy(alpha = 0.70f),
                    0.25f to Color.Transparent,
                    0.75f to Color.Transparent,
                    1.00f to Color.Black.copy(alpha = 0.85f)
                )
            )
    ) {
        // ── TOP BAR ───────────────────────────────────────────────────────
        Row(
            Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBackIosNew, "Back", tint = Color.White, modifier = Modifier.size(22.dp))
            }
            Column(Modifier.weight(1f).padding(horizontal = 4.dp)) {
                Text(
                    state.currentItem?.title ?: "", style = MaterialTheme.typography.titleMedium,
                    color = Color.White, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text(
                    state.currentItem?.displayArtist ?: "", style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(0.75f), maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = { vm.toggleVideo() }) {
                Icon(Icons.Outlined.MusicNote, "Audio mode", tint = Color.White, modifier = Modifier.size(22.dp))
            }
            IconButton(onClick = onToggleFS) {
                Icon(
                    if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                    "Fullscreen", tint = Color.White, modifier = Modifier.size(22.dp)
                )
            }
        }

        // ── CENTER TRANSPORT ──────────────────────────────────────────────
        Row(
            Modifier.align(Alignment.Center),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            // -10s
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(Modifier.size(48.dp), onClick = { vm.seekTo((state.progress - 10_000).coerceAtLeast(0)) }) {
                    Icon(Icons.Rounded.Replay10, null, tint = Color.White, modifier = Modifier.size(32.dp))
                }
            }
            // Prev
            IconButton(Modifier.size(52.dp), onClick = { vm.skipToPrev() }) {
                Icon(Icons.Default.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(36.dp))
            }
            // Play/Pause — large circle
            Box(
                Modifier.size(74.dp).clip(CircleShape).background(Color.White.copy(0.18f)).clickable { vm.togglePlayPause() },
                Alignment.Center
            ) {
                AnimatedContent(state.isPlaying, label = "vpp", transitionSpec = { scaleIn(tween(160)) togetherWith scaleOut(tween(160)) }) { p ->
                    if (state.isBuffering)
                        CircularProgressIndicator(Modifier.size(30.dp), color = Color.White, strokeWidth = 3.dp)
                    else
                        Icon(if (p) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(40.dp))
                }
            }
            // Next
            IconButton(Modifier.size(52.dp), onClick = { vm.skipToNext() }) {
                Icon(Icons.Default.SkipNext, null, tint = Color.White, modifier = Modifier.size(36.dp))
            }
            // +10s
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(Modifier.size(48.dp), onClick = { vm.seekTo((state.progress + 10_000).coerceAtMost(state.duration)) }) {
                    Icon(Icons.Rounded.Forward10, null, tint = Color.White, modifier = Modifier.size(32.dp))
                }
            }
        }

        // ── BOTTOM — time + seekbar + extras ─────────────────────────────
        Column(
            Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text(fmtMs(state.progress), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.85f))
                Text(state.currentItem?.durationFormatted ?: "--:--", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.85f))
            }
            Slider(
                value         = state.progressFraction,
                onValueChange = { vm.seekToFraction(it) },
                colors        = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = PurpleAccent, inactiveTrackColor = Color.White.copy(0.3f))
            )
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                IconButton(onClick = { vm.toggleShuffle() }) {
                    Icon(Icons.Default.Shuffle, null, tint = if (state.shuffleEnabled) PurpleAccent else Color.White.copy(0.6f), modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = { vm.cycleRepeatMode() }) {
                    Icon(if (state.repeatMode == RepeatMode.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat, null,
                        tint = if (state.repeatMode != RepeatMode.OFF) PurpleAccent else Color.White.copy(0.6f), modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════
// AUDIO LAYOUT — album art FADES ONLY when user scrolls up to Up Next
//               artwork stays fixed, only opacity changes with scroll
// ════════════════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AudioLayout(
    state: com.auralyx.domain.model.PlayerState,
    vm: PlayerViewModel,
    onBack: () -> Unit
) {
    val scrollState = rememberLazyListState()

    // Compute art alpha from scroll offset of the list
    // Art starts fading after 60px scroll, fully gone by 300px
    val rawAlpha by remember {
        derivedStateOf {
            val first  = scrollState.layoutInfo.visibleItemsInfo.firstOrNull() ?: return@derivedStateOf 1f
            val offset = -first.offset.toFloat()
            ((1f - (offset - 60f) / 240f)).coerceIn(0f, 1f)
        }
    }
    val artAlpha by animateFloatAsState(rawAlpha, tween(80), label = "artAlpha")

    Box(Modifier.fillMaxSize()) {
        // ── Blurred background from album art / aD17 thumbnail ────────────
        val artUri = state.currentItem?.albumArtUri
        if (!artUri.isNullOrBlank()) {
            AsyncImage(
                model            = artUri,
                contentDescription = null,
                modifier         = Modifier.fillMaxSize().blur(72.dp).alpha(0.30f),
                contentScale     = ContentScale.Crop
            )
        } else if (state.currentItem?.isAD17 == true) {
            AD17ThumbnailImage(
                path     = state.currentItem!!.path,
                modifier = Modifier.fillMaxSize().blur(72.dp).alpha(0.30f)
            )
        }

        // Gradient overlay
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(
                    MaterialTheme.colorScheme.background.copy(0.75f),
                    MaterialTheme.colorScheme.background.copy(0.93f),
                    MaterialTheme.colorScheme.background
                ))
            )
        )

        Column(Modifier.fillMaxSize()) {
            // ── TOP BAR ───────────────────────────────────────────────────
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.KeyboardArrowDown, "Back", modifier = Modifier.size(34.dp))
                    }
                },
                title = {
                    Box(Modifier.fillMaxWidth(), Alignment.Center) {
                        Text("NOW PLAYING", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 2.sp)
                    }
                },
                actions = {
                    // Show video toggle only for aD17 files
                    if (state.currentItem?.isAD17 == true) {
                        IconButton(onClick = { vm.toggleVideo() }) {
                            Icon(Icons.Default.Videocam, "Watch video", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    IconButton(onClick = {}) { Icon(Icons.Default.MoreVert, "More") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )

            LazyColumn(
                state          = scrollState,
                modifier       = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 48.dp)
            ) {
                // ── 1. ALBUM ART — fades as user scrolls toward queue ─────
                item {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 28.dp, vertical = 4.dp)
                            .alpha(artAlpha),   // <-- ONLY the art fades
                        Alignment.Center
                    ) {
                        // Subtle glow behind art
                        Box(
                            Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(26.dp))
                                .background(Brush.radialGradient(listOf(MaterialTheme.colorScheme.primary.copy(0.20f), Color.Transparent)))
                        )
                        Card(
                            Modifier.fillMaxWidth().aspectRatio(1f),
                            shape     = RoundedCornerShape(24.dp),
                            elevation = CardDefaults.cardElevation(32.dp)
                        ) {
                            if (state.currentItem?.isAD17 == true) {
                                AD17ThumbnailImage(state.currentItem!!.path, Modifier.fillMaxSize())
                            } else {
                                AlbumArt(state.currentItem?.albumArtUri, false, Modifier.fillMaxSize())
                            }
                        }
                    }
                }

                // ── 2. TRACK INFO ─────────────────────────────────────────
                item {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 10.dp),
                        Arrangement.SpaceBetween, Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                state.currentItem?.title ?: "Nothing playing",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                state.currentItem?.displayArtist ?: "Unknown Artist",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (state.currentItem?.isAD17 == true) {
                            Spacer(Modifier.width(10.dp))
                            Surface(color = PurpleAccent.copy(0.18f), shape = RoundedCornerShape(10.dp)) {
                                Row(Modifier.padding(horizontal = 9.dp, vertical = 5.dp), Arrangement.spacedBy(4.dp), Alignment.CenterVertically) {
                                    Icon(Icons.Default.Videocam, null, tint = PurpleAccent, modifier = Modifier.size(13.dp))
                                    Text("aD17", style = MaterialTheme.typography.labelSmall, color = PurpleAccent, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // ── 3. SEEKBAR ────────────────────────────────────────────
                item {
                    Column(Modifier.padding(horizontal = 24.dp)) {
                        Slider(
                            value         = state.progressFraction,
                            onValueChange = { vm.seekToFraction(it) },
                            colors        = SliderDefaults.colors(
                                thumbColor         = MaterialTheme.colorScheme.primary,
                                activeTrackColor   = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(0.15f)
                            )
                        )
                        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), Arrangement.SpaceBetween) {
                            Text(fmtMs(state.progress), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(state.currentItem?.durationFormatted ?: "--:--", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // ── 4. TRANSPORT CONTROLS ─────────────────────────────────
                item { AudioControls(state, vm) }

                // ── 5. VOLUME ROW ─────────────────────────────────────────
                item {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.VolumeDown, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                        Slider(
                            value = 0.75f, onValueChange = {},
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.onSurfaceVariant, activeTrackColor = MaterialTheme.colorScheme.onSurfaceVariant, inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(0.12f))
                        )
                        Icon(Icons.Default.VolumeUp, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    }
                }

                // ── 6. UP NEXT HEADER ─────────────────────────────────────
                item {
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(Modifier.padding(horizontal = 28.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(0.4f))
                    Row(Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 14.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Text("Up Next", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        val remaining = state.queue.size - state.queueIndex - 1
                        Text("$remaining songs", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // ── 7. QUEUE ITEMS ────────────────────────────────────────
                val upNext = state.queue.drop(state.queueIndex + 1)
                if (upNext.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(32.dp), Alignment.Center) {
                            Text("Queue is empty", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    itemsIndexed(upNext, key = { _, it -> it.id }) { i, item ->
                        QueueRow(item, i + 1)
                    }
                }
            }
        }
    }
}

// ─── Audio transport controls row ────────────────────────────────────────────
@Composable
private fun AudioControls(state: com.auralyx.domain.model.PlayerState, vm: PlayerViewModel) {
    val playScale by animateFloatAsState(if (state.isPlaying) 1f else 0.90f, spring(Spring.DampingRatioMediumBouncy), label = "ps")

    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        Arrangement.SpaceEvenly, Alignment.CenterVertically
    ) {
        // Shuffle
        CtrlBtn(Icons.Default.Shuffle, 44.dp, 22.dp, active = state.shuffleEnabled) { vm.toggleShuffle() }
        // Prev
        CtrlBtn(Icons.Default.SkipPrevious, 56.dp, 34.dp) { vm.skipToPrev() }

        // Play/Pause — gradient filled circle
        Box(
            Modifier.size(72.dp).scale(playScale).clip(CircleShape)
                .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)))
                .clickable { vm.togglePlayPause() },
            Alignment.Center
        ) {
            AnimatedContent(state.isPlaying, label = "pp", transitionSpec = { scaleIn(tween(150)) togetherWith scaleOut(tween(150)) }) { p ->
                if (state.isBuffering)
                    CircularProgressIndicator(Modifier.size(26.dp), color = Color.White, strokeWidth = 3.dp)
                else
                    Icon(if (p) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(36.dp))
            }
        }

        // Next
        CtrlBtn(Icons.Default.SkipNext, 56.dp, 34.dp) { vm.skipToNext() }
        // Repeat
        CtrlBtn(if (state.repeatMode == RepeatMode.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat, 44.dp, 22.dp, active = state.repeatMode != RepeatMode.OFF) { vm.cycleRepeatMode() }
    }
}

@Composable
private fun CtrlBtn(
    icon: androidx.compose.ui.graphics.vector.ImageVector, size: Dp, iconSize: Dp,
    active: Boolean = false, onClick: () -> Unit
) {
    IconButton(onClick = onClick, modifier = Modifier.size(size)) {
        Icon(icon, null, modifier = Modifier.size(iconSize),
            tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ─── Queue row ────────────────────────────────────────────────────────────────
@Composable
private fun QueueRow(item: MediaItem, pos: Int) {
    ListItem(
        overlineContent = { Text("$pos", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        headlineContent = { Text(item.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = { Text("${item.displayArtist} \u2022 ${item.durationFormatted}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingContent = {
            Box(Modifier.size(46.dp)) {
                if (item.isAD17) AD17ThumbnailImage(item.path, Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)))
                else AlbumArt(item.albumArtUri, false, Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)))
            }
        }
    )
    HorizontalDivider(Modifier.padding(start = 72.dp), 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(0.3f))
}

private fun fmtMs(ms: Long): String {
    val s = ms / 1000
    return if (s >= 3600) "%d:%02d:%02d".format(s/3600, (s%3600)/60, s%60) else "%d:%02d".format(s/60, s%60)
}
