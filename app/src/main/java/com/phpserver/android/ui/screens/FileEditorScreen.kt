package com.phpserver.android.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
// mutableIntStateOf not available in this compose version
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phpserver.android.ui.theme.*
import com.phpserver.android.viewmodel.ServerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileEditorScreen(
    viewModel: ServerViewModel,
    filename: String,
    onNavigateBack: () -> Unit,
) {
    var content by remember { mutableStateOf("") }
    var textFieldValue by remember { mutableStateOf(TextFieldValue("")) }
    var isModified by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var showUnsavedDialog by remember { mutableStateOf(false) }
    var lineCount by remember { mutableStateOf(1) }
    var cursorPosition by remember { mutableStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    var showSavedMessage by remember { mutableStateOf(false) }

    // Show save message
    LaunchedEffect(showSavedMessage) {
        if (showSavedMessage) {
            snackbarHostState.showSnackbar("File saved successfully!")
            showSavedMessage = false
        }
    }

    // Load file content
    LaunchedEffect(filename) {
        content = viewModel.readFile(filename)
        textFieldValue = TextFieldValue(content)
        lineCount = content.count { it == '\n' } + 1
    }

    val isPhp = filename.endsWith(".php", ignoreCase = true)
    val extension = filename.substringAfterLast('.', "").lowercase()
    val isEditable = extension in editableExtensions

    BackHandler(enabled = isModified) {
        showUnsavedDialog = true
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Icon(
                                if (isPhp) Icons.Default.Code else Icons.Default.Description,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = if (isPhp) PhpPrimary else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                filename,
                                style = MaterialTheme.typography.titleMedium,
                                fontFamily = FontFamily.Monospace
                            )
                            if (isModified) {
                                Text(
                                    " •",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        Text(
                            "$lineCount lines${if (cursorPosition > 0) " • Col $cursorPosition" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isModified) showUnsavedDialog else onNavigateBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isEditable) {
                        IconButton(onClick = {
                            content = textFieldValue.text
                            viewModel.updateFile(filename, content)
                            isModified = false
                            showSavedMessage = true
                        }, enabled = isModified) {
                            Icon(
                                Icons.Default.Save,
                                contentDescription = "Save",
                                tint = if (isModified) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 3.dp,
                shadowElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
                ) {
                    Row {
                        Text(
                            "PHP",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isPhp) PhpPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        Text(
                            "UTF-8",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                    if (isModified) {
                        Text(
                            "Modified",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFF57F17),
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                    } else {
                        Text(
                            "Saved",
                            style = MaterialTheme.typography.labelSmall,
                            color = PhpSuccess
                        )
                    }
                }
            }
        }
    ) { padding ->
        if (isEditable) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Line numbers + Editor
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .horizontalScroll(rememberScrollState())
                ) {
                    // Line numbers
                    Column(
                        modifier = Modifier.padding(start = 8.dp, top = 8.dp)
                    ) {
                        val lines = textFieldValue.text.split("\n")
                        lines.forEachIndexed { index, _ ->
                            Text(
                                "${index + 1}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.padding(vertical = 1.dp)
                            )
                        }
                    }

                    Divider(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(1.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    // Code editor
                    OutlinedTextField(
                        value = textFieldValue,
                        onValueChange = { new ->
                            textFieldValue = new
                            isModified = new.text != content
                            lineCount = new.text.count { it == '\n' } + 1
                            // Calculate cursor position (column)
                            val textBeforeCursor = new.text.substring(0, new.selection.start)
                            cursorPosition = textBeforeCursor.count { it == '\n' }.let { lineNum ->
                                textBeforeCursor.substringAfterLast('\n').length + 1
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            unfocusedBorderColor = Color.Transparent,
                            disabledBorderColor = Color.Transparent,
                            cursorColor = PhpPrimary,
                        ),
                        readOnly = false
                    )
                }
            }
        } else {
            // Read-only viewer
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    content,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp
                    ),
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }

    // Unsaved changes dialog
    if (showUnsavedDialog) {
        AlertDialog(
            onDismissRequest = { showUnsavedDialog = false },
            title = { Text("Unsaved Changes") },
            text = { Text("You have unsaved changes. Do you want to discard them?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showUnsavedDialog = false
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Discard")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showUnsavedDialog = false
                    content = textFieldValue.text
                    viewModel.updateFile(filename, content)
                    isModified = false
                    onNavigateBack()
                }) {
                    Text("Save & Exit")
                }
            }
        )
    }
}

private val editableExtensions = setOf("php", "html", "htm", "css", "js", "json", "xml", "txt", "md", "sql", "env", "htaccess", "ini", "cfg", "yml", "yaml", "sh", "bat")
