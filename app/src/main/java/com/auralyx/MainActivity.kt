package com.auralyx

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.auralyx.service.AuralyxPlaybackService
import com.auralyx.ui.navigation.AuralyxNavGraph
import com.auralyx.ui.theme.AuralyxTheme
import com.auralyx.utils.PermissionUtils
import com.auralyx.utils.PreferencesManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var prefs: PreferencesManager

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* permissions handled in Compose via state */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Full edge-to-edge — content draws behind system bars
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Request storage permissions immediately on first launch
        permissionLauncher.launch(PermissionUtils.getRequiredPermissions())

        // Start playback service early so it's ready when user taps play
        startServiceCompat()

        setContent {
            val isDark by prefs.isDarkTheme.collectAsState(initial = true)
            AuralyxTheme(darkTheme = isDark) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AuralyxNavGraph()
                }
            }
        }
    }

    private fun startServiceCompat() {
        try {
            val intent = Intent(this, AuralyxPlaybackService::class.java)
            startService(intent)
        } catch (e: Exception) {
            // Service will start on first play anyway
        }
    }
}
