package com.auralyx.ui.search

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.auralyx.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onNavigateToPlayer: () -> Unit,
    vm: SearchViewModel = hiltViewModel()
) {
    val query   by vm.query.collectAsState()
    val results by vm.results.collectAsState()
    val player  by vm.playerState.collectAsState()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        try { focusRequester.requestFocus() } catch (_: Exception) {}
    }

    GradientBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        OutlinedTextField(
                            value            = query,
                            onValueChange    = vm::onQueryChange,
                            modifier         = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                            placeholder      = { Text("Songs, artists, albums…") },
                            leadingIcon      = { Icon(Icons.Default.Search, null) },
                            trailingIcon = if (query.isNotEmpty()) {
                                { IconButton(onClick = { vm.onQueryChange("") }) {
                                    Icon(Icons.Default.Close, "Clear")
                                }}
                            } else null,
                            singleLine       = true,
                            shape            = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                            colors           = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor   = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                when {
                    query.isBlank() -> {
                        Column(
                            modifier              = Modifier.align(Alignment.Center),
                            horizontalAlignment   = Alignment.CenterHorizontally,
                            verticalArrangement   = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Search, null, modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                            Text("Search your library", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    results.isEmpty() -> {
                        Column(
                            modifier              = Modifier.align(Alignment.Center),
                            horizontalAlignment   = Alignment.CenterHorizontally,
                            verticalArrangement   = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.SearchOff, null, modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                            Text("No results for "$query"", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    else -> {
                        LazyColumn {
                            item {
                                Text(
                                    "${results.size} results",
                                    style    = MaterialTheme.typography.labelMedium,
                                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                            items(results, key = { it.id }) { item ->
                                MediaListItem(
                                    item      = item,
                                    isPlaying = player.currentItem?.id == item.id && player.isPlaying,
                                    onClick   = {
                                        vm.play(item, results)
                                        onNavigateToPlayer()
                                    }
                                )
                                HorizontalDivider(modifier = Modifier.padding(start = 72.dp), thickness = 0.5.dp)
                            }
                        }
                    }
                }
            }
        }
    }
}
