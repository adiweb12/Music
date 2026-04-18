package com.auralyx

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.auralyx.ui.navigation.AuralyxNavGraph
import com.auralyx.ui.theme.AuralyxTheme
import com.auralyx.utils.PreferencesManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var preferencesManager: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val isDark by preferencesManager.isDarkTheme.collectAsState(initial = true)
            AuralyxTheme(darkTheme = isDark) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AuralyxNavGraph()
                }
            }
        }
    }
}
