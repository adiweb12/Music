package com.auralyx.ui.library

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.auralyx.domain.model.*
import com.auralyx.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onNavigateToPlayer: () -> Unit,
    vm: LibraryViewModel = hiltViewModel()
) {
    val state  by vm.state.collectAsState()
    val player by vm.playerState.collectAsState()
    val tabs   = LibraryTab.values().toList()
    val labels = listOf("Songs","Albums","Artists","Folders","Videos")

    GradientBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Column {
                    TopAppBar(
                        title  = { Text("Library", style = MaterialTheme.typography.headlineLarge) },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                    )
                    ScrollableTabRow(
                        selectedTabIndex = tabs.indexOf(state.tab),
                        edgePadding      = 16.dp,
                        containerColor   = Color.Transparent,
                        indicator = { positions ->
                            val idx = tabs.indexOf(state.tab)
                            if (idx < positions.size) {
                                TabRowDefaults.SecondaryIndicator(
                                    modifier = Modifier.tabIndicatorOffset(positions[idx]),
                                    color    = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    ) {
                        tabs.forEachIndexed { i, tab ->
                            Tab(selected = state.tab == tab, onClick = { vm.selectTab(tab) },
                                text = { Text(labels[i]) })
                        }
                    }
                }
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else when (state.tab) {
                    LibraryTab.SONGS   -> SongsTab(state.songs, player.currentItem?.id, player.isPlaying) {
                        vm.play(it, state.songs); onNavigateToPlayer()
                    }
                    LibraryTab.ALBUMS  -> AlbumsTab(state.albums)
                    LibraryTab.ARTISTS -> ArtistsTab(state.artists)
                    LibraryTab.FOLDERS -> FoldersTab(state.folders)
                    LibraryTab.VIDEOS  -> VideosTab(state.videos) {
                        vm.play(it, state.videos, videoEnabled = true); onNavigateToPlayer()
                    }
                }
            }
        }
    }
}

@Composable private fun SongsTab(
    songs: List<MediaItem>, currentId: Long?, isPlaying: Boolean, onPlay: (MediaItem) -> Unit
) {
    LazyColumn {
        items(songs, key = { it.id }) { song ->
            MediaListItem(song, isPlaying = currentId == song.id && isPlaying, onClick = { onPlay(song) })
            HorizontalDivider(modifier = Modifier.padding(start = 72.dp), thickness = 0.5.dp)
        }
    }
}

@Composable private fun AlbumsTab(albums: List<Album>) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(160.dp),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement   = Arrangement.spacedBy(12.dp)
    ) {
        items(albums.size) { i ->
            val a = albums[i]
            Card(shape = RoundedCornerShape(12.dp)) {
                Column {
                    AlbumArt(a.artUri, modifier = Modifier.fillMaxWidth().aspectRatio(1f))
                    Column(Modifier.padding(8.dp)) {
                        Text(a.name, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${a.songCount} songs", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable private fun ArtistsTab(artists: List<Artist>) {
    LazyColumn {
        items(artists.size) { i ->
            val a = artists[i]
            ListItem(
                headlineContent   = { Text(a.name) },
                supportingContent = { Text("${a.albumCount} albums • ${a.songCount} songs", style = MaterialTheme.typography.bodySmall) },
                leadingContent    = {
                    Box(Modifier.size(48.dp)) {
                        AlbumArt(a.artUri, modifier = Modifier.fillMaxSize().clip(CircleShape))
                    }
                }
            )
            HorizontalDivider(modifier = Modifier.padding(start = 72.dp), thickness = 0.5.dp)
        }
    }
}

@Composable private fun FoldersTab(folders: List<Folder>) {
    LazyColumn {
        items(folders.size) { i ->
            val f = folders[i]
            ListItem(
                headlineContent   = { Text(f.name) },
                supportingContent = { Text("${f.songCount} songs", style = MaterialTheme.typography.bodySmall) },
                leadingContent    = { Icon(Icons.Default.Folder, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp)) }
            )
        }
    }
}

@Composable private fun VideosTab(videos: List<MediaItem>, onPlay: (MediaItem) -> Unit) {
    LazyColumn {
        items(videos, key = { it.id }) { v ->
            MediaListItem(v, onClick = { onPlay(v) })
            HorizontalDivider(modifier = Modifier.padding(start = 72.dp), thickness = 0.5.dp)
        }
    }
}
