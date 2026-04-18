package com.auralyx.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.auralyx.ui.home.HomeScreen
import com.auralyx.ui.library.LibraryScreen
import com.auralyx.ui.player.MiniPlayerBar
import com.auralyx.ui.player.PlayerScreen
import com.auralyx.ui.search.SearchScreen
import com.auralyx.ui.settings.SettingsScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuralyxNavGraph() {
    val navController = rememberNavController()
    val entry         by navController.currentBackStackEntryAsState()
    val route         = entry?.destination?.route

    val isPlayerRoute  = route == Routes.PLAYER
    val showBottomBar  = route in NavDestination.bottomNavItems.map { it.route }
    val showMiniPlayer = !isPlayerRoute && route != null

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            Column {
                if (showMiniPlayer) {
                    MiniPlayerBar(onTap = { navController.navigate(Routes.PLAYER) })
                }
                if (showBottomBar) {
                    AuralyxBottomNav(navController, route)
                }
            }
        }
    ) { padding ->
        NavHost(
            navController    = navController,
            startDestination = NavDestination.Home.route,
            modifier         = Modifier.padding(padding)
        ) {
            composable(
                NavDestination.Home.route,
                enterTransition = { fadeIn(tween(220)) },
                exitTransition  = { fadeOut(tween(180)) }
            ) {
                HomeScreen(
                    onNavigateToPlayer   = { navController.navigate(Routes.PLAYER) },
                    onNavigateToSettings = { navController.navigate(Routes.SETTINGS) }
                )
            }

            composable(
                NavDestination.Library.route,
                enterTransition = { fadeIn(tween(220)) },
                exitTransition  = { fadeOut(tween(180)) }
            ) {
                LibraryScreen(onNavigateToPlayer = { navController.navigate(Routes.PLAYER) })
            }

            composable(
                NavDestination.Search.route,
                enterTransition = { fadeIn(tween(220)) },
                exitTransition  = { fadeOut(tween(180)) }
            ) {
                SearchScreen(onNavigateToPlayer = { navController.navigate(Routes.PLAYER) })
            }

            composable(
                Routes.PLAYER,
                enterTransition = {
                    slideInVertically(tween(380, easing = FastOutSlowInEasing)) { it } +
                    fadeIn(tween(280))
                },
                exitTransition = {
                    slideOutVertically(tween(300)) { it } + fadeOut(tween(260))
                }
            ) {
                PlayerScreen(onBack = { navController.popBackStack() })
            }

            composable(
                Routes.SETTINGS,
                enterTransition = {
                    slideInHorizontally(tween(280)) { it } + fadeIn(tween(220))
                },
                exitTransition = {
                    slideOutHorizontally(tween(240)) { it } + fadeOut(tween(200))
                }
            ) {
                SettingsScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

@Composable
private fun AuralyxBottomNav(navController: NavHostController, currentRoute: String?) {
    NavigationBar(tonalElevation = 0.dp) {
        NavDestination.bottomNavItems.forEach { dest ->
            val selected = currentRoute == dest.route
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(dest.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState    = true
                    }
                },
                icon  = { Icon(if (selected) dest.selectedIcon else dest.unselectedIcon, dest.label) },
                label = { Text(dest.label) }
            )
        }
    }
}
