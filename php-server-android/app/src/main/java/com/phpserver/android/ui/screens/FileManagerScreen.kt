package com.phpserver.android.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.phpserver.android.file.FileInfo
import com.phpserver.android.ui.theme.*
import com.phpserver.android.viewmodel.ServerViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileManagerScreen(
    viewModel: ServerViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToEditor: (String) -> Unit,
    onNavigateToEditorNew: (String) -> Unit,
) {
    val files by viewModel.files.collectAsState()
    val directories by viewModel.directories.collectAsState()
    val currentPath by viewModel.currentPath.collectAsState()
    var showCreateFileDialog by remember { mutableStateOf(false) }
    var showCreateDirDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.refreshServerStatus()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "File Manager",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            currentPath,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        val defaultPath = viewModel.getDefaultPath().absolutePath
                        if (currentPath != defaultPath) {
                            viewModel.navigateUp()
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(
                            if (currentPath != viewModel.getDefaultPath().absolutePath)
                                Icons.Default.ArrowBack
                            else
                                Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showCreateFileDialog = true }) {
                        Icon(Icons.Default.NoteAdd, contentDescription = "New File")
                    }
                    IconButton(onClick = { showCreateDirDialog = true }) {
                        Icon(Icons.Default.CreateNewFolder, contentDescription = "New Folder")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search files...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                shape = MaterialTheme.shapes.large,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
                singleLine = true
            )

            // Path Breadcrumb
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Folder,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        currentPath,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // File List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Navigate up button
                if (currentPath != viewModel.getDefaultPath().absolutePath) {
                    item {
                        FileItem(
                            fileInfo = FileInfo("..", 0, 0, "dir"),
                            onClick = { viewModel.navigateUp() }
                        )
                    }
                }

                // Directories
                val filteredDirs = if (searchQuery.isEmpty()) directories
                    else directories.filter { it.name.contains(searchQuery, ignoreCase = true) }

                items(filteredDirs, key = { "dir_${it.name}" }) { dir ->
                    FileItem(
                        fileInfo = dir,
                        onClick = { viewModel.navigateToDirectory(dir) },
                        onDelete = null
                    )
                }

                // Files
                val filteredFiles = if (searchQuery.isEmpty()) files
                    else files.filter { it.name.contains(searchQuery, ignoreCase = true) }

                items(filteredFiles, key = { "file_${it.name}" }) { file ->
                    FileItem(
                        fileInfo = file,
                        onClick = { onNavigateToEditor(file.name) },
                        onDelete = { showDeleteDialog = file.name }
                    )
                }

                // Empty state
                if (directories.isEmpty() && files.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.FolderOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "No files found",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                                Text(
                                    "Create a new file or folder",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Create File Dialog
    if (showCreateFileDialog) {
        CreateFileDialog(
            onDismiss = { showCreateFileDialog = false },
            onConfirm = { filename ->
                val template = when {
                    filename.endsWith(".php") -> getPhpTemplate(filename)
                    filename.endsWith(".html") -> getHtmlTemplate()
                    filename.endsWith(".css") -> "/* Stylesheet */\n"
                    filename.endsWith(".js") -> "// JavaScript\n"
                    filename.endsWith(".json") -> "{\n  \n}"
                    else -> ""
                }
                viewModel.createFile(filename, template)
                onNavigateToEditorNew(filename)
                showCreateFileDialog = false
            }
        )
    }

    // Create Directory Dialog
    if (showCreateDirDialog) {
        CreateDirDialog(
            onDismiss = { showCreateDirDialog = false },
            onConfirm = { name ->
                viewModel.createDirectory(name)
                showCreateDirDialog = false
            }
        )
    }

    // Delete Confirmation
    showDeleteDialog?.let { filename ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete File") },
            text = { Text("Are you sure you want to delete '$filename'? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteFile(filename)
                        showDeleteDialog = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun FileItem(
    fileInfo: FileInfo,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    val icon = when {
        fileInfo.name == ".." -> Icons.Default.ArrowUpward
        fileInfo.extension == "dir" || fileInfo.name == ".." -> Icons.Default.Folder
        fileInfo.extension == "php" -> Icons.Default.Code
        fileInfo.extension == "html" || fileInfo.extension == "htm" -> Icons.Default.Language
        fileInfo.extension == "css" -> Icons.Default.Palette
        fileInfo.extension == "js" -> Icons.Default.Javascript
        fileInfo.extension == "json" -> Icons.Default.DataObject
        fileInfo.extension == "xml" -> Icons.Default.Description
        fileInfo.extension == "txt" -> Icons.Default.TextFields
        fileInfo.extension == "md" -> Icons.Default.Markdown
        fileInfo.extension in listOf("png", "jpg", "jpeg", "gif", "svg", "ico") -> Icons.Default.Image
        fileInfo.extension in listOf("sql", "db") -> Icons.Default.Storage
        else -> Icons.Default.InsertDriveFile
    }

    val iconColor = when {
        fileInfo.extension == "dir" || fileInfo.name == ".." -> MaterialTheme.colorScheme.primary
        fileInfo.extension == "php" -> PhpPrimary
        fileInfo.extension in listOf("html", "htm", "css", "js") -> MaterialTheme.colorScheme.tertiary
        fileInfo.extension == "json" -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = iconColor
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    fileInfo.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (fileInfo.extension != "dir" && fileInfo.name != "..") {
                    Row {
                        Text(
                            formatFileSize(fileInfo.size),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            formatDate(fileInfo.lastModified),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            if (fileInfo.extension == "php" && fileInfo.name != "..") {
                Surface(
                    color = PhpPrimary.copy(alpha = 0.15f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        "PHP",
                        style = MaterialTheme.typography.labelSmall,
                        color = PhpPrimary,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
            if (onDelete != null) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
fun CreateFileDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var filename by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New File") },
        text = {
            Column {
                OutlinedTextField(
                    value = filename,
                    onValueChange = { filename = it },
                    label = { Text("File name") },
                    placeholder = { Text("example.php") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Supported: .php, .html, .css, .js, .json, .txt",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (filename.isNotBlank()) onConfirm(filename.trim()) },
                enabled = filename.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun CreateDirDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var dirname by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Folder") },
        text = {
            OutlinedTextField(
                value = dirname,
                onValueChange = { dirname = it },
                label = { Text("Folder name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (dirname.isNotBlank()) onConfirm(dirname.trim()) },
                enabled = dirname.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> "${bytes / (1024 * 1024)} MB"
    }
}

private fun formatDate(timestamp: Long): String {
    if (timestamp == 0L) return ""
    val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun getPhpTemplate(filename: String): String {
    return """<?php
/**
 * $filename
 * Created: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}
 */

// Your PHP code here
echo "Hello from $filename!";


""".trimIndent() + "\n"
}

private fun getHtmlTemplate(): String {
    return """<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Page</title>
</head>
<body>
    <h1>Hello World</h1>
</body>
</html>
"""
}
