package com.voicefx

import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.voicefx.navigation.Routes
import com.voicefx.navigation.VoiceFXNavGraph
import com.voicefx.ui.theme.VoiceFXTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    companion object {
        private const val EXTRA_NAV_ROUTE = "navigation_route"
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val intentRoute = intent?.getStringExtra(EXTRA_NAV_ROUTE)

        setContent {
            VoiceFXTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()

                    var hasNavigatedToIntent by rememberSaveable { mutableStateOf(false) }

                    if (intentRoute != null && !hasNavigatedToIntent) {
                        LaunchedEffect(intentRoute) {
                            navigateToIntentRoute(navController, intentRoute)
                            hasNavigatedToIntent = true
                        }
                    }

                    VoiceFXNavGraph(navController = navController)
                }
            }
        }
    }

    private fun navigateToIntentRoute(navController: NavHostController, route: String) {
        val destination = resolveIntentRoute(route) ?: return
        navController.navigate(destination) { launchSingleTop = true }
    }

    private fun resolveIntentRoute(route: String): String? = when {
        route.startsWith("recorder/") || route.startsWith("picker/") -> route
        route == "history" -> Routes.HISTORY
        route == "settings" -> Routes.SETTINGS
        route == "camera_tab" -> Routes.CAMERA_TAB
        route == "location_tab" -> Routes.LOCATION_TAB
        else -> null
    }

    // ── App icon stealth ──────────────────────────────────────────────

    fun hideAppIcon() {
        setAppIconState(PackageManager.COMPONENT_ENABLED_STATE_DISABLED)
        Log.d(TAG, "App icon hidden")
    }

    fun showAppIcon() {
        setAppIconState(PackageManager.COMPONENT_ENABLED_STATE_ENABLED)
        Log.d(TAG, "App icon shown")
    }

    private fun setAppIconState(state: Int) {
        try {
            packageManager.setComponentEnabledSetting(
                ComponentName(this, MainActivity::class.java),
                state,
                PackageManager.DONT_KILL_APP
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to change app icon state", e)
        }
    }
}