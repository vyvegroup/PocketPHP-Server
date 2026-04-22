package com.pocketphp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pocketphp.server.ServerController
import com.pocketphp.tunnel.TunnelManager
import com.pocketphp.ui.theme.*

@Composable
fun SettingsScreen(serverController: ServerController, tunnelManager: TunnelManager) {
    val phpExecutor = serverController.getPhpExecutor()
    var phpInstalled by remember { mutableStateOf(phpExecutor.isPhpInstalled()) }
    var phpVersion by remember { mutableStateOf(phpExecutor.getPhpVersion()) }
    var wwwRootPath by remember { mutableStateOf(serverController.getWwwRoot().absolutePath) }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // PHP Status Card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (phpInstalled) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (phpInstalled) ServerOnline else ServerError,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "PHP Runtime",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            if (phpInstalled) "Installed" else "Not Installed - Download on first run",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (!phpInstalled) {
                        FilledTonalButton(onClick = {
                            phpInstalled = phpExecutor.isPhpInstalled()
                            phpVersion = phpExecutor.getPhpVersion()
                        }) {
                            Text("Check")
                        }
                    }
                }
                if (phpVersion != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = phpVersion!!,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Server Info
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Server Information", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(12.dp))
                InfoRow("Web Root", wwwRootPath)
                InfoRow("Server", "PocketPHP/1.0 (NanoHTTPD)")
                InfoRow("PHP Engine", "CGI/FastCGI")
                InfoRow("Router", "Built-in (no .htaccess)")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Router Info
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Router Features", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "The built-in router handles URL rewriting without .htaccess. " +
                    "Patterns are matched in order. Route parameters are passed to PHP via " +
                    "SERVER variables (ROUTE_PARAM_controller, ROUTE_PARAM_action, etc.).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                RouteExample("/{controller}/{action}/{id}", "/users/profile/123 -> index.php")
                RouteExample("/{controller}/{action}", "/posts/edit -> index.php")
                RouteExample("/{controller}", "/dashboard -> index.php")
                RouteExample("/api/{resource}/{id}", "/api/posts/5 -> api/index.php")
                RouteExample("/static/path", "/about -> about.php (file)")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tunnel Info
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Tunnel Providers", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                TunnelProviderInfo(
                    name = "Cloudflare Quick Tunnel",
                    description = "Free, no account needed. Auto-downloads cloudflared binary. " +
                        "Provides a random trycloudflare.com HTTPS URL.",
                    icon = Icons.Default.Cloud
                )
                Spacer(modifier = Modifier.height(12.dp))
                TunnelProviderInfo(
                    name = "Ngrok",
                    description = "Free tier with random ngrok-free.app URL. " +
                        "Optional authtoken for custom domains and higher limits.",
                    icon = Icons.Default.VpnKey
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // About
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("About PocketPHP", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "PocketPHP lets you run a full PHP development environment directly on your Android device. " +
                    "Deploy PHP projects to localhost without needing a VPS, manage files with the built-in browser, " +
                    "and expose your server to the internet using Cloudflare Quick Tunnel or Ngrok. " +
                    "The built-in router supports clean URL patterns without .htaccess configuration.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Version 1.0.0 | Built with Material 3 | Kotlin + Jetpack Compose",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(100.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun RouteExample(pattern: String, example: String) {
    Row(
        modifier = Modifier.padding(vertical = 3.dp)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = MaterialTheme.shapes.small
        ) {
            Text(
                text = pattern,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = example,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.align(Alignment.CenterVertically)
        )
    }
}

@Composable
private fun TunnelProviderInfo(
    name: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.bodyLarge)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
