package com.phpserver.android.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phpserver.android.server.ServerState
import com.phpserver.android.ui.theme.*
import com.phpserver.android.viewmodel.ServerViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: ServerViewModel,
    onNavigateToFiles: () -> Unit,
    onNavigateToLogs: () -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    val serverStatus by viewModel.serverStatus.collectAsState()
    val port by viewModel.port.collectAsState()
    val isStarting by viewModel.isServerStarting.collectAsState()
    val context = LocalContext.current

    // Auto-refresh status
    LaunchedEffect(serverStatus.isRunning) {
        while (true) {
            viewModel.refreshServerStatus()
            delay(2000)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "PHP Local Server",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Deploy PHP on Android",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Dashboard") },
                    label = { Text("Dashboard") },
                    selected = true,
                    onClick = { }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Folder, contentDescription = "Files") },
                    label = { Text("Files") },
                    selected = false,
                    onClick = onNavigateToFiles
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Article, contentDescription = "Logs") },
                    label = { Text("Logs") },
                    selected = false,
                    onClick = onNavigateToLogs
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Server Status Card
            item {
                ServerStatusCard(
                    isRunning = serverStatus.isRunning,
                    isStarting = isStarting,
                    port = port,
                    uptime = serverStatus.uptime,
                    requestCount = serverStatus.requestCount,
                    onStart = { viewModel.startServer(port) },
                    onStop = { viewModel.stopServer() },
                    onPortChange = { viewModel.setPort(it) },
                )
            }

            // Quick Access Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            "Quick Access",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        if (serverStatus.isRunning) {
                            QuickAccessButton(
                                icon = Icons.Default.Language,
                                title = "Open in Browser",
                                subtitle = "http://localhost:$port",
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("http://localhost:$port"))
                                    context.startActivity(intent)
                                }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            QuickAccessButton(
                                icon = Icons.Default.Code,
                                title = "PHP Info",
                                subtitle = "http://localhost:$port/phpinfo.php",
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("http://localhost:$port/phpinfo.php"))
                                    context.startActivity(intent)
                                }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            QuickAccessButton(
                                icon = Icons.Default.Science,
                                title = "Run PHP Test",
                                subtitle = "http://localhost:$port/test.php",
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("http://localhost:$port/test.php"))
                                    context.startActivity(intent)
                                }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            QuickAccessButton(
                                icon = Icons.Default.Api,
                                title = "API Status",
                                subtitle = "http://localhost:$port/api/status",
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("http://localhost:$port/api/status"))
                                    context.startActivity(intent)
                                }
                            )
                        } else {
                            Text(
                                "Start the server to access quick links",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            // PHP Info Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "🐘",
                                fontSize = 28.sp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "PHP ${ServerState.phpVersion}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        InfoRow("Engine", ServerState.phpVersion)
                        InfoRow("Architecture", System.getProperty("os.arch") ?: "Unknown")
                        InfoRow("Document Root", ServerState.documentRoot)
                        InfoRow("Android", "${android.os.Build.VERSION.RELEASE} (${android.os.Build.MODEL})")
                    }
                }
            }

            // File Manager Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onNavigateToFiles,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                "File Manager",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Manage your PHP projects",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Server Logs Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onNavigateToLogs,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Terminal,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                "Server Logs",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "${ServerState.logs.size} log entries",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Footer
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "PHP Local Server v1.0.0",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Text(
                    "Run PHP applications on Android without a VPS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun ServerStatusCard(
    isRunning: Boolean,
    isStarting: Boolean,
    port: Int,
    uptime: Long,
    requestCount: Int,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onPortChange: (Int) -> Unit,
) {
    var portInput by remember { mutableStateOf(port.toString()) }
    var showPortDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isStarting -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                isRunning -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            // Status Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status Indicator
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .padding(1.dp)
                        ) {
                            Surface(
                                modifier = Modifier.size(12.dp),
                                shape = MaterialTheme.shapes.extraSmall,
                                color = when {
                                    isStarting -> ServerStarting
                                    isRunning -> ServerOnline
                                    else -> ServerOffline
                                }
                            ) {}
                            if (isStarting || isRunning) {
                                Surface(
                                    modifier = Modifier.size(12.dp),
                                    shape = MaterialTheme.shapes.extraSmall,
                                    color = when {
                                        isStarting -> ServerStarting
                                        isRunning -> ServerOnline
                                        else -> ServerOffline
                                }
                                ) {}
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            when {
                                isStarting -> "Starting..."
                                isRunning -> "Server Online"
                                else -> "Server Offline"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                isStarting -> ServerStarting
                                isRunning -> ServerOnline
                                else -> ServerOffline
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Port Display
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        ":$port",
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stats Row
            if (isRunning) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem("Uptime", formatUptime(uptime))
                    StatItem("Requests", requestCount.toString())
                    StatItem("Port", port.toString())
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!isRunning && !isStarting) {
                    Button(
                        onClick = onStart,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ServerOnline
                        ),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start Server", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { showPortDialog = true },
                        shape = MaterialTheme.shapes.large
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Port")
                    }
                } else if (isRunning) {
                    Button(
                        onClick = onStop,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ServerOffline
                        ),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Stop Server", fontWeight = FontWeight.Bold)
                    }

                    FilledTonalButton(
                        onClick = { showPortDialog = true },
                        shape = MaterialTheme.shapes.large,
                        enabled = false
                    ) {
                        Text("Port: $port")
                    }
                } else {
                    Button(
                        onClick = {},
                        modifier = Modifier.weight(1f),
                        enabled = false,
                        shape = MaterialTheme.shapes.large
                    ) {
                        Icon(Icons.Default.Loading, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Starting...", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Port Dialog
    if (showPortDialog) {
        AlertDialog(
            onDismissRequest = { showPortDialog = false },
            title = { Text("Change Port") },
            text = {
                OutlinedTextField(
                    value = portInput,
                    onValueChange = {
                        if (it.all { char -> char.isDigit() } && it.length <= 5) {
                            portInput = it
                        }
                    },
                    label = { Text("Port Number") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.foundation.text.KeyboardType.Number
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val newPort = portInput.toIntOrNull()
                        if (newPort != null && newPort in 1024..65535) {
                            onPortChange(newPort)
                        }
                        showPortDialog = false
                    }
                ) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPortDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun QuickAccessButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onTertiaryContainer
        )
    }
}

private fun formatUptime(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m ${secs}s"
        else -> "${secs}s"
    }
}
