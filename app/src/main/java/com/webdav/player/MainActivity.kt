package com.webdav.player

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.webdav.player.data.preferences.AppPreferences
import com.webdav.player.ui.navigation.AppNavigation
import com.webdav.player.ui.theme.ThemeMode
import com.webdav.player.ui.theme.WebDavPlayerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var appPreferences: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val themeFlow = combine(
            appPreferences.themeMode,
            appPreferences.dynamicColor
        ) { mode, dynamic -> mode to dynamic }
            .stateIn(lifecycleScope, SharingStarted.Eagerly, ThemeMode.SYSTEM to true)

        setContent {
            val (themeMode, dynamicColor) by themeFlow.collectAsState()

            WebDavPlayerTheme(
                themeMode = themeMode,
                dynamicColor = dynamicColor
            ) {
                AppNavigation()
            }
        }
    }
}
