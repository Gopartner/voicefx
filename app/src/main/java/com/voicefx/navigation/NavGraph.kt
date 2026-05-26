package com.voicefx.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.voicefx.ui.home.HomeScreen
import com.voicefx.ui.picker.FilePickerScreen
import com.voicefx.ui.history.HistoryScreen
import com.voicefx.ui.recorder.RecorderScreen
import com.voicefx.ui.preview.PreviewScreen
import com.voicefx.ui.upload.UploadScreen

object Routes {
    const val HOME = "home"
    const val PICKER = "picker/{source}"
    const val HISTORY = "history"
    const val RECORDER = "recorder/{preset}"
    const val UPLOAD = "upload/{audioUri}/{preset}"
    const val PREVIEW = "preview/{audioUri}"

    fun picker(source: String) = "picker/$source"
    fun recorder(preset: String) = "recorder/$preset"
    fun upload(audioUri: String, preset: String) = "upload/$audioUri/$preset"
    fun preview(audioUri: String) = "preview/$audioUri"
}

@Composable
fun VoiceFXNavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onNavigateToPicker = { source -> navController.navigate(Routes.picker(source)) },
                onNavigateToHistory = { navController.navigate(Routes.HISTORY) },
                onNavigateToRecorder = { preset -> navController.navigate(Routes.recorder(preset)) },
                onNavigateToUpload = { uri, preset -> navController.navigate(Routes.upload(uri, preset)) }
            )
        }
        composable(
            route = Routes.PICKER,
            arguments = listOf(navArgument("source") { type = NavType.StringType })
        ) { backStackEntry ->
            val source = backStackEntry.arguments?.getString("source") ?: "internal"
            FilePickerScreen(
                source = source,
                onNavigateBack = { navController.popBackStack() },
                onFileSelected = { uri ->
                    navController.navigate(Routes.preview(uri.toString())) {
                        popUpTo(Routes.HOME)
                    }
                }
            )
        }
        composable(Routes.HISTORY) {
            HistoryScreen(
                onNavigateBack = { navController.popBackStack() },
                onVoiceNoteSelected = { uri ->
                    navController.navigate(Routes.preview(uri.toString())) {
                        popUpTo(Routes.HOME)
                    }
                }
            )
        }
        composable(
            route = Routes.RECORDER,
            arguments = listOf(navArgument("preset") { type = NavType.StringType })
        ) { backStackEntry ->
            val preset = backStackEntry.arguments?.getString("preset") ?: "ORIGINAL"
            RecorderScreen(
                presetName = preset,
                onNavigateBack = { navController.popBackStack() },
                onRecordingComplete = { uri ->
                    navController.navigate(Routes.preview(uri.toString())) {
                        popUpTo(Routes.HOME)
                    }
                }
            )
        }
        composable(
            route = Routes.UPLOAD,
            arguments = listOf(
                navArgument("audioUri") { type = NavType.StringType },
                navArgument("preset") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val audioUri = backStackEntry.arguments?.getString("audioUri") ?: return@composable
            val preset = backStackEntry.arguments?.getString("preset") ?: "ORIGINAL"
            UploadScreen(
                audioUri = audioUri,
                presetName = preset,
                onNavigateBack = { navController.popBackStack() },
                onProcessingComplete = { resultUri ->
                    navController.navigate(Routes.preview(resultUri)) {
                        popUpTo(Routes.HOME)
                    }
                }
            )
        }
        composable(
            route = Routes.PREVIEW,
            arguments = listOf(navArgument("audioUri") { type = NavType.StringType })
        ) { backStackEntry ->
            val audioUri = backStackEntry.arguments?.getString("audioUri") ?: return@composable
            PreviewScreen(
                audioUri = audioUri,
                onNavigateBack = { navController.popBackStack() },
                onNavigateHome = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                }
            )
        }
    }
}
