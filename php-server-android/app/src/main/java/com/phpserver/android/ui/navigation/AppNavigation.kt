package com.phpserver.android.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.phpserver.android.ui.screens.DashboardScreen
import com.phpserver.android.ui.screens.FileEditorScreen
import com.phpserver.android.ui.screens.FileManagerScreen
import com.phpserver.android.ui.screens.ServerLogScreen
import com.phpserver.android.ui.screens.SettingsScreen
import com.phpserver.android.viewmodel.ServerViewModel

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object FileManager : Screen("files")
    object FileEditor : Screen("editor/{filename}") {
        fun createRoute(filename: String) = "editor/$filename"
    }
    object ServerLog : Screen("logs")
    object Settings : Screen("settings")
}

@Composable
fun AppNavigation(viewModel: ServerViewModel = viewModel()) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.Dashboard.route) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                viewModel = viewModel,
                onNavigateToFiles = { navController.navigate(Screen.FileManager.route) },
                onNavigateToLogs = { navController.navigate(Screen.ServerLog.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
            )
        }

        composable(Screen.FileManager.route) {
            FileManagerScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEditor = { filename ->
                    navController.navigate(Screen.FileEditor.createRoute(filename))
                },
                onNavigateToEditorNew = { filename ->
                    navController.navigate(Screen.FileEditor.createRoute(filename))
                },
            )
        }

        composable(
            route = Screen.FileEditor.route,
            arguments = listOf(navArgument("filename") { type = NavType.StringType })
        ) { backStackEntry ->
            val filename = backStackEntry.arguments?.getString("filename") ?: ""
            FileEditorScreen(
                viewModel = viewModel,
                filename = filename,
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(Screen.ServerLog.route) {
            ServerLogScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
            )
        }
    }
}
