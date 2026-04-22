package com.pocketphp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pocketphp.server.ServerController
import com.pocketphp.tunnel.TunnelManager
import com.pocketphp.ui.screens.*
import com.pocketphp.ui.theme.PocketPHPTheme

class MainActivity : ComponentActivity() {

    private lateinit var serverController: ServerController
    private lateinit var tunnelManager: TunnelManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        serverController = ServerController(this)
        tunnelManager = TunnelManager(this)

        setContent {
            PocketPHPTheme {
                MainContent(serverController, tunnelManager)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serverController.stop()
        tunnelManager.destroy()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContent(
    serverController: ServerController,
    tunnelManager: TunnelManager
) {
    val navController = rememberNavController()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                val items = listOf(
                    Triple("server", "Server", Icons.Default.Dns),
                    Triple("tunnels", "Tunnels", Icons.Default.Public),
                    Triple("files", "Files", Icons.Default.Folder),
                    Triple("browser", "Browser", Icons.Default.Language),
                    Triple("settings", "Settings", Icons.Default.Settings)
                )

                items.forEach { (route, label, icon) ->
                    NavigationBarItem(
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label) },
                        selected = currentRoute == route,
                        onClick = {
                            navController.navigate(route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "server",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("server") {
                ServerScreen(
                    serverController = serverController,
                    onOpenBrowser = { port ->
                        navController.navigate("browser/$port")
                    }
                )
            }
            composable("tunnels") {
                TunnelScreen(
                    tunnelManager = tunnelManager,
                    getServerPort = { serverController.getPort() }
                )
            }
            composable("files") {
                FileBrowserScreen(serverController = serverController)
            }
            composable(
                "browser/{port}",
                arguments = listOf(navArgument("port") { type = NavType.IntType })
            ) { backStackEntry ->
                val port = backStackEntry.arguments?.getInt("port") ?: 8080
                BrowserScreen(port = port, tunnelUrl = tunnelManager.getPublicUrl())
            }
            composable("settings") {
                SettingsScreen(serverController = serverController, tunnelManager = tunnelManager)
            }
        }
    }
}
