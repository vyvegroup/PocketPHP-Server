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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.pocketphp.server.PhpHttpServer
import com.pocketphp.server.ServerController
import com.pocketphp.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerScreen(
    serverController: ServerController,
    onOpenBrowser: (Int) -> Unit
) {
    var serverPort by remember { mutableIntStateOf(8080) }
    var serverStatus by remember { mutableStateOf(PhpHttpServer.ServerStatus.STOPPED) }
    var logs by remember { mutableStateOf(listOf<String>()) }
    var showPortDialog by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    // PHP state
    var phpInstalled by remember { mutableStateOf(serverController.isPhpInstalled()) }
    var phpVersion by remember { mutableStateOf(serverController.getPhpVersion()) }
    var isInstalling by remember { mutableStateOf(false) }
    var installProgress by remember { mutableIntStateOf(0) }
    var installMessage by remember { mutableStateOf("") }
    var installError by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Poll server status
    LaunchedEffect(Unit) {
        while (true) {
            serverStatus = serverController.getStatus()
            kotlinx.coroutines.delay(1000)
        }
    }

    // Check PHP status periodically
    LaunchedEffect(Unit) {
        while (true) {
            phpInstalled = serverController.isPhpInstalled()
            if (phpInstalled) {
                phpVersion = serverController.getPhpVersion()
            }
            kotlinx.coroutines.delay(3000)
        }
    }

    // Capture logs
    LaunchedEffect(serverController) {
        serverController.setLogCallback { log ->
            logs = logs + log
            if (logs.size > 200) logs = logs.takeLast(200)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        // Header
        Text(
            text = "PHP Server",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // PHP Installation Card (shown when PHP not installed or installing)
        if (!phpInstalled || isInstalling) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (installError)
                        ServerError.copy(alpha = 0.1f)
                    else if (isInstalling)
                        ServerStarting.copy(alpha = 0.1f)
                    else
                        MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isInstalling) {
                        Icon(
                            imageVector = Icons.Default.Downloading,
                            contentDescription = null,
                            tint = ServerStarting,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Installing PHP 8.3...",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = installProgress / 100f,
                            modifier = Modifier.fillMaxWidth(),
                            color = ServerStarting
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "$installProgress% - $installMessage",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else if (installError) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = ServerError,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "PHP Installation Failed",
                            style = MaterialTheme.typography.titleMedium,
                            color = ServerError
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = installMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                installError = false
                                installMessage = ""
                                scope.launch {
                                    isInstalling = true
                                    installProgress = 0
                                    val success = serverController.getPhpInstaller().install { progress, msg ->
                                        installProgress = if (progress < 0) 0 else progress
                                        installMessage = msg
                                    }
                                    isInstalling = false
                                    if (success) {
                                        phpInstalled = true
                                        phpVersion = serverController.getPhpVersion()
                                        installMessage = ""
                                    } else {
                                        installError = true
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ServerError)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Retry Installation")
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "PHP 8.3 Runtime Required",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "The PHP runtime needs to be installed before starting the server. This will extract PHP 8.3 and all required libraries.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                scope.launch {
                                    isInstalling = true
                                    installProgress = 0
                                    installError = false
                                    installMessage = ""
                                    val success = serverController.getPhpInstaller().install { progress, msg ->
                                        installProgress = if (progress < 0) 0 else progress
                                        installMessage = msg
                                    }
                                    isInstalling = false
                                    if (success) {
                                        phpInstalled = true
                                        phpVersion = serverController.getPhpVersion()
                                        installMessage = ""
                                    } else {
                                        installError = true
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Install PHP 8.3")
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        } else {
            // PHP Info Card (shown when PHP is installed)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = ServerOnline.copy(alpha = 0.1f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = ServerOnline,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "PHP 8.3 Ready",
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (phpVersion != null) {
                            Text(
                                text = phpVersion!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Server Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = when (serverStatus) {
                    PhpHttpServer.ServerStatus.RUNNING -> ServerOnline.copy(alpha = 0.1f)
                    PhpHttpServer.ServerStatus.STARTING -> ServerStarting.copy(alpha = 0.1f)
                    PhpHttpServer.ServerStatus.ERROR -> ServerError.copy(alpha = 0.1f)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when (serverStatus) {
                        PhpHttpServer.ServerStatus.RUNNING -> Icons.Default.CheckCircle
                        PhpHttpServer.ServerStatus.STARTING -> Icons.Default.Refresh
                        PhpHttpServer.ServerStatus.ERROR -> Icons.Default.Error
                        else -> Icons.Default.StopCircle
                    },
                    contentDescription = "Status",
                    tint = when (serverStatus) {
                        PhpHttpServer.ServerStatus.RUNNING -> ServerOnline
                        PhpHttpServer.ServerStatus.STARTING -> ServerStarting
                        PhpHttpServer.ServerStatus.ERROR -> ServerError
                        else -> ServerOffline
                    },
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = when (serverStatus) {
                            PhpHttpServer.ServerStatus.RUNNING -> "Server Running"
                            PhpHttpServer.ServerStatus.STARTING -> "Starting..."
                            PhpHttpServer.ServerStatus.ERROR -> "Error"
                            else -> "Server Stopped"
                        },
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Port: $serverPort",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (serverStatus == PhpHttpServer.ServerStatus.RUNNING) {
                        Text(
                            text = "http://localhost:$serverPort",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Port Configuration
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Port Configuration", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Port: $serverPort", style = MaterialTheme.typography.bodyLarge)
                    FilledTonalButton(onClick = { showPortDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Change")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    serverController.setPort(serverPort)
                    serverController.setLogCallback { log ->
                        logs = logs + log
                        if (logs.size > 200) logs = logs.takeLast(200)
                    }
                    serverController.start()
                },
                modifier = Modifier.weight(1f),
                enabled = phpInstalled && !isInstalling &&
                    (serverStatus == PhpHttpServer.ServerStatus.STOPPED || serverStatus == PhpHttpServer.ServerStatus.ERROR),
                colors = ButtonDefaults.buttonColors(containerColor = ServerOnline)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start")
            }

            OutlinedButton(
                onClick = {
                    serverController.stop()
                    serverStatus = PhpHttpServer.ServerStatus.STOPPED
                    logs = emptyList()
                },
                modifier = Modifier.weight(1f),
                enabled = serverStatus == PhpHttpServer.ServerStatus.RUNNING
            ) {
                Icon(Icons.Default.Stop, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Stop")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Open in Browser button
        FilledTonalButton(
            onClick = { onOpenBrowser(serverPort) },
            modifier = Modifier.fillMaxWidth(),
            enabled = serverStatus == PhpHttpServer.ServerStatus.RUNNING
        ) {
            Icon(Icons.Default.Language, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Open in Browser")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Server Logs
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Server Logs", style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = { logs = emptyList() }) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp, max = 300.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = if (logs.isEmpty()) "No logs yet. Start the server to see logs." else logs.joinToString("\n"),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace
                        ),
                        modifier = Modifier
                            .padding(8.dp)
                            .verticalScroll(rememberScrollState())
                    )
                }
            }
        }
    }

    // Port Dialog
    if (showPortDialog) {
        var portInput by remember { mutableStateOf(serverPort.toString()) }
        AlertDialog(
            onDismissRequest = { showPortDialog = false },
            title = { Text("Set Port") },
            text = {
                OutlinedTextField(
                    value = portInput,
                    onValueChange = { if (it.all { c -> c.isDigit() }) portInput = it },
                    label = { Text("Port (1024-65535)") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val port = portInput.toIntOrNull()
                    if (port != null && port in 1024..65535) {
                        serverPort = port
                    }
                    showPortDialog = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showPortDialog = false }) { Text("Cancel") }
            }
        )
    }
}
