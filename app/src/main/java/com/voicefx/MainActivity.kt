package com.voicefx

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.voicefx.navigation.Routes
import com.voicefx.navigation.VoiceFXNavGraph
import com.voicefx.ui.theme.VoiceFXTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val navRoute = intent?.getStringExtra("navigation_route")
        enableEdgeToEdge()
        setContent {
            VoiceFXTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    LaunchedEffect(navRoute) {
                        navRoute?.let { route ->
                            val destination = when {
                                route.startsWith("recorder/") -> route
                                route.startsWith("picker/") -> route
                                route == "history" -> Routes.HISTORY
                                route == "settings" -> Routes.SETTINGS
                                route == "camera_tab" -> Routes.CAMERA_TAB
                                route == "location_tab" -> Routes.LOCATION_TAB
                                else -> null
                            }
                            destination?.let { navController.navigate(it) }
                        }
                    }
                    VoiceFXNavGraph(navController = navController)
                }
            }
        }
    }
}
