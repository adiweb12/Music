package com.auralyx.ui.navigation

import androidx.compose.animation.*
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
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in NavDestination.bottomNavItems.map { it.route }
    val showMiniPlayer = currentRoute != Routes.PLAYER && currentRoute != null

    Scaffold(
        bottomBar = {
            Column {
                // Mini player sits above the bottom nav
                if (showMiniPlayer) {
                    MiniPlayerBar(
                        onTap = { navController.navigate(Routes.PLAYER) }
                    )
                }
                if (showBottomBar) {
                    AuralyxBottomNav(
                        navController = navController,
                        currentRoute  = currentRoute
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController    = navController,
            startDestination = NavDestination.Home.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(
                route = NavDestination.Home.route,
                enterTransition = { fadeIn(tween(200)) },
                exitTransition  = { fadeOut(tween(200)) }
            ) {
                HomeScreen(
                    onNavigateToPlayer   = { navController.navigate(Routes.PLAYER) },
                    onNavigateToSettings = { navController.navigate(Routes.SETTINGS) }
                )
            }

            composable(
                route = NavDestination.Library.route,
                enterTransition = { fadeIn(tween(200)) },
                exitTransition  = { fadeOut(tween(200)) }
            ) {
                LibraryScreen(
                    onNavigateToPlayer = { navController.navigate(Routes.PLAYER) }
                )
            }

            composable(
                route = NavDestination.Search.route,
                enterTransition = { fadeIn(tween(200)) },
                exitTransition  = { fadeOut(tween(200)) }
            ) {
                SearchScreen(
                    onNavigateToPlayer = { navController.navigate(Routes.PLAYER) }
                )
            }

            composable(
                route = Routes.PLAYER,
                enterTransition = {
                    slideInVertically(initialOffsetY = { it }, animationSpec = tween(350)) +
                    fadeIn(tween(350))
                },
                exitTransition = {
                    slideOutVertically(targetOffsetY = { it }, animationSpec = tween(300)) +
                    fadeOut(tween(300))
                }
            ) {
                PlayerScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.SETTINGS) {
                SettingsScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

@Composable
private fun AuralyxBottomNav(
    navController: NavHostController,
    currentRoute: String?
) {
    NavigationBar(tonalElevation = 0.dp) {
        NavDestination.bottomNavItems.forEach { dest ->
            val selected = currentRoute == dest.route
            NavigationBarItem(
                selected = selected,
                onClick  = {
                    navController.navigate(dest.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState    = true
                    }
                },
                icon  = {
                    Icon(
                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                        contentDescription = dest.label
                    )
                },
                label = { Text(dest.label) }
            )
        }
    }
}
