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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.pocketphp.tunnel.TunnelManager
import com.pocketphp.tunnel.TunnelManager.TunnelState
import com.pocketphp.tunnel.TunnelManager.TunnelType
import com.pocketphp.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TunnelScreen(
    tunnelManager: TunnelManager,
    getServerPort: () -> Int
) {
    var tunnelState by remember { mutableStateOf(TunnelState.STOPPED) }
    var publicUrl by remember { mutableStateOf<String?>(null) }
    var selectedTunnel by remember { mutableStateOf(TunnelType.CLOUDFLARE) }
    var ngrokToken by remember { mutableStateOf("") }
    var showTokenDialog by remember { mutableStateOf(false) }
    var logs by remember { mutableStateOf(listOf<String>()) }
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        tunnelManager.stateListener = { tunnelState = it }
        tunnelManager.urlListener = { publicUrl = it }
        tunnelManager.logListener = { log ->
            logs = logs + log
            if (logs.size > 300) logs = logs.takeLast(300)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        Text(
            text = "Tunnels",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Tunnel Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = when (tunnelState) {
                    TunnelState.CONNECTED -> TunnelConnected.copy(alpha = 0.1f)
                    TunnelState.STARTING -> TunnelStarting.copy(alpha = 0.1f)
                    TunnelState.ERROR -> TunnelError.copy(alpha = 0.1f)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            )
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when (tunnelState) {
                            TunnelState.CONNECTED -> Icons.Default.CloudDone
                            TunnelState.STARTING -> Icons.Default.Refresh
                            TunnelState.ERROR -> Icons.Default.Error
                            else -> Icons.Default.CloudOff
                        },
                        contentDescription = "Tunnel Status",
                        tint = when (tunnelState) {
                            TunnelState.CONNECTED -> TunnelConnected
                            TunnelState.STARTING -> TunnelStarting
                            TunnelState.ERROR -> TunnelError
                            else -> TunnelDisconnected
                        },
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = when (tunnelState) {
                            TunnelState.CONNECTED -> "Tunnel Active"
                            TunnelState.STARTING -> "Connecting..."
                            TunnelState.ERROR -> "Connection Error"
                            else -> "Tunnel Inactive"
                        },
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                if (publicUrl != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Public URL:",
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                text = publicUrl!!,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = MaterialTheme.colorScheme.primary,
                                    textDecoration = TextDecoration.Underline
                                )
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tunnel Provider Selection
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Provider", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedTunnel == TunnelType.CLOUDFLARE,
                        onClick = { selectedTunnel = TunnelType.CLOUDFLARE },
                        label = { Text("Cloudflare") },
                        leadingIcon = {
                            Icon(Icons.Default.Cloud, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    )
                    FilterChip(
                        selected = selectedTunnel == TunnelType.NGROK,
                        onClick = { selectedTunnel = TunnelType.NGROK },
                        label = { Text("Ngrok") },
                        leadingIcon = {
                            Icon(Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Ngrok Token (only if ngrok selected)
        if (selectedTunnel == TunnelType.NGROK) {
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Ngrok Auth Token", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Optional: Add your ngrok authtoken for higher limits and custom domains.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = ngrokToken,
                            onValueChange = { ngrokToken = it },
                            label = { Text("Authtoken") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        FilledTonalButton(onClick = { showTokenDialog = true }) {
                            Text("Set")
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    val token = if (selectedTunnel == TunnelType.NGROK && ngrokToken.isNotBlank()) ngrokToken else null
                    tunnelManager.startTunnel(selectedTunnel, getServerPort(), token)
                },
                modifier = Modifier.weight(1f),
                enabled = tunnelState == TunnelState.STOPPED || tunnelState == TunnelState.ERROR || tunnelState == TunnelState.DISCONNECTED,
                colors = ButtonDefaults.buttonColors(containerColor = TunnelConnected)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start Tunnel")
            }

            OutlinedButton(
                onClick = { tunnelManager.stopTunnel() },
                modifier = Modifier.weight(1f),
                enabled = tunnelState != TunnelState.STOPPED
            ) {
                Icon(Icons.Default.Stop, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Stop")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tunnel Logs
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Tunnel Logs", style = MaterialTheme.typography.titleMedium)
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
                        text = if (logs.isEmpty()) "No tunnel logs yet" else logs.joinToString("\n"),
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
}
