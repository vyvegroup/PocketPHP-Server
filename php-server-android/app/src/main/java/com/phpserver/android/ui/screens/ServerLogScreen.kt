package com.phpserver.android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phpserver.android.ui.theme.*
import com.phpserver.android.viewmodel.ServerViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerLogScreen(
    viewModel: ServerViewModel,
    onNavigateBack: () -> Unit,
) {
    val logs by viewModel.logs.collectAsState()
    val listState = rememberLazyListState()
    var autoScroll by remember { mutableStateOf(true) }
    var filterText by remember { mutableStateOf("") }

    // Auto-refresh logs
    LaunchedEffect(Unit) {
        while (true) {
            viewModel.refreshServerStatus()
            if (autoScroll && logs.isNotEmpty()) {
                listState.animateScrollToItem(logs.size - 1)
            }
            delay(1500)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Server Logs",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            "${logs.size} entries",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { autoScroll = !autoScroll }) {
                        Icon(
                            if (autoScroll) Icons.Default.VerticalAlignBottom else Icons.Default.VerticalAlignCenter,
                            contentDescription = if (autoScroll) "Auto-scroll ON" else "Auto-scroll OFF",
                            tint = if (autoScroll) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = {
                        autoScroll = true
                        if (logs.isNotEmpty()) {
                            listState.animateScrollToItem(logs.size - 1)
                        }
                    }) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = "Scroll to Bottom")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Auto-scroll: ${if (autoScroll) "ON" else "OFF"}",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (autoScroll) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = { viewModel.clearLogs() }) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear Logs")
                    }
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Filter
            OutlinedTextField(
                value = filterText,
                onValueChange = { filterText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Filter logs...") },
                leadingIcon = { Icon(Icons.Default.FilterList, contentDescription = null) },
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            )

            // Log entries
            val filteredLogs = if (filterText.isEmpty()) logs
                else logs.filter { it.contains(filterText, ignoreCase = true) }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
            ) {
                if (filteredLogs.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Terminal,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "No logs yet",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                                if (filterText.isNotEmpty()) {
                                    Text(
                                        "No logs matching '$filterText'",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }
                }

                items(filteredLogs) { log ->
                    LogEntryItem(log)
                }
            }
        }
    }
}

@Composable
fun LogEntryItem(log: String) {
    val isError = log.contains("ERROR", ignoreCase = true)
    val isPhp = log.contains("PHP:", ignoreCase = true)
    val isRequest = log.contains("GET ", ignoreCase = true) || log.contains("POST ", ignoreCase = true)

    val backgroundColor = when {
        isError -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        isPhp -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f)
        else -> MaterialTheme.colorScheme.surface
    }

    val accentColor = when {
        isError -> MaterialTheme.colorScheme.error
        isPhp -> PhpPrimary
        isRequest -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        color = backgroundColor,
        shape = MaterialTheme.shapes.extraSmall,
        modifier = Modifier.padding(vertical = 1.dp)
    ) {
        Text(
            buildAnnotatedString {
                if (log.startsWith("[")) {
                    val closingBracket = log.indexOf(']')
                    if (closingBracket > 0) {
                        withStyle(SpanStyle(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )) {
                            append(log.substring(0, closingBracket + 1))
                        }
                        withStyle(SpanStyle(
                            color = accentColor,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )) {
                            append(log.substring(closingBracket + 1))
                        }
                    } else {
                        withStyle(SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = accentColor
                        )) {
                            append(log)
                        }
                    }
                } else {
                    withStyle(SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = accentColor
                    )) {
                        append(log)
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            lineHeight = 16.sp
        )
    }
}
