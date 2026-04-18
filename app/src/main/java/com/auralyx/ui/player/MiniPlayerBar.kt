package com.auralyx.ui.player

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.scale
import androidx.hilt.navigation.compose.hiltViewModel
import com.auralyx.ui.components.AD17ThumbnailImage
import com.auralyx.ui.components.AlbumArt
import com.auralyx.ui.components.PlayingBars
import com.auralyx.ui.theme.PurpleAccent

/**
 * Professional mini-player bar:
 * - Slides up from bottom when playback starts
 * - Shows album art / aD17 thumbnail, title, artist
 * - Animated progress bar along the top edge
 * - Prev / Play / Next buttons
 * - Tap anywhere to open full PlayerScreen
 */
@Composable
fun MiniPlayerBar(
    onTap: () -> Unit,
    vm: PlayerViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    val item  = state.currentItem ?: return

    AnimatedVisibility(
        visible       = true,
        enter         = slideInVertically { it } + fadeIn(tween(280)),
        exit          = slideOutVertically { it } + fadeOut(tween(200))
    ) {
        Surface(
            modifier        = Modifier.fillMaxWidth().clickable(onClick = onTap),
            color           = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation  = 12.dp,
            shadowElevation = 12.dp,
            shape           = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)
        ) {
            Column {
                // ── Thin progress strip at the very top ───────────────────
                val progressAnim by animateFloatAsState(state.progressFraction, tween(500), label = "prog")
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(MaterialTheme.colorScheme.onSurface.copy(0.08f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progressAnim)
                            .height(3.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(MaterialTheme.colorScheme.primary, PurpleAccent)
                                )
                            )
                    )
                }

                // ── Main row ──────────────────────────────────────────────
                Row(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Artwork
                    Box(Modifier.size(46.dp)) {
                        if (item.isAD17) {
                            AD17ThumbnailImage(item.path, Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)))
                        } else {
                            AlbumArt(item.albumArtUri, false, Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)))
                        }
                        if (state.isPlaying) {
                            Box(
                                Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp))
                                    .background(Color.Black.copy(0.35f)),
                                Alignment.Center
                            ) { PlayingBars() }
                        }
                    }

                    // Title + artist
                    Column(Modifier.weight(1f)) {
                        Text(
                            text       = item.title,
                            style      = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines   = 1,
                            overflow   = TextOverflow.Ellipsis
                        )
                        Text(
                            text     = item.displayArtist,
                            style    = MaterialTheme.typography.bodySmall,
                            color    = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Prev
                    IconButton(onClick = { vm.skipToPrev() }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.SkipPrevious, "Prev", modifier = Modifier.size(22.dp))
                    }

                    // Play / Pause with scale animation
                    val btnScale by animateFloatAsState(
                        if (state.isPlaying) 1f else 0.88f,
                        spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "s"
                    )
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .scale(btnScale)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable { vm.togglePlayPause() },
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedContent(
                            targetState   = state.isPlaying,
                            transitionSpec = { scaleIn(tween(160)) togetherWith scaleOut(tween(160)) },
                            label         = "mini_pp"
                        ) { playing ->
                            if (state.isBuffering) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(
                                    imageVector = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    // Next
                    IconButton(onClick = { vm.skipToNext() }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.SkipNext, "Next", modifier = Modifier.size(22.dp))
                    }
                }
            }
        }
    }
}
