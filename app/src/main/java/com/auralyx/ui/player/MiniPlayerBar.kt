package com.auralyx.ui.player

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.auralyx.ui.components.AlbumArt

/**
 * Compact player bar shown above the bottom navigation when music is playing.
 * Tapping it navigates to the full PlayerScreen.
 */
@Composable
fun MiniPlayerBar(
    onTap: () -> Unit,
    vm: PlayerViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    val item = state.currentItem ?: return  // hidden when nothing is playing

    AnimatedVisibility(
        visible       = true,
        enter         = slideInVertically { it } + fadeIn(),
        exit          = slideOutVertically { it } + fadeOut()
    ) {
        Surface(
            modifier      = Modifier
                .fillMaxWidth()
                .clickable(onClick = onTap),
            color         = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 8.dp,
            shadowElevation = 8.dp,
            shape         = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        ) {
            Column {
                // Progress line at top
                LinearProgressIndicator(
                    progress       = state.progressFraction,
                    modifier       = Modifier.fillMaxWidth().height(2.dp),
                    color          = MaterialTheme.colorScheme.primary,
                    trackColor     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                )

                Row(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Album art thumbnail
                    Box(modifier = Modifier.size(44.dp)) {
                        AlbumArt(
                            uri      = item.albumArtUri,
                            isVideo  = item.isAD17,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(8.dp))
                        )
                    }

                    // Title + artist
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text     = item.title,
                            style    = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
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
                    IconButton(
                        onClick  = { vm.skipToPrev() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.SkipPrevious, "Prev", modifier = Modifier.size(22.dp))
                    }

                    // Play / Pause
                    IconButton(
                        onClick  = { vm.togglePlayPause() },
                        modifier = Modifier.size(44.dp)
                    ) {
                        AnimatedContent(targetState = state.isPlaying, label = "mini_pp",
                            transitionSpec = { scaleIn() togetherWith scaleOut() }) { playing ->
                            if (state.isBuffering) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(
                                    imageVector = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }

                    // Next
                    IconButton(
                        onClick  = { vm.skipToNext() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.SkipNext, "Next", modifier = Modifier.size(22.dp))
                    }
                }
            }
        }
    }
}
