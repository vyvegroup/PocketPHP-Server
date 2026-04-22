package com.pocketphp.ui.screens

import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pocketphp.server.ServerController
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileBrowserScreen(serverController: ServerController) {
    val wwwRoot = serverController.getWwwRoot()
    val context = LocalContext.current
    var currentDir by remember { mutableStateOf(wwwRoot) }
    var selectedFile by remember { mutableStateOf<File?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var newFileName by remember { mutableStateOf("") }
    var isFolder by remember { mutableStateOf(false) }

    val files = remember(currentDir) {
        currentDir.listFiles()
            ?.sortedWith(compareByDescending<File> { it.isDirectory }.thenBy { it.name.lowercase() })
            ?: emptyList()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top Bar
        TopAppBar(
            title = {
                Column {
                    Text("File Browser", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = currentDir.relativeTo(wwwRoot).path.let {
                            if (it == ".") "/ (root)" else "/$it"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            actions = {
                IconButton(onClick = {
                    if (currentDir != wwwRoot) currentDir = currentDir.parentFile ?: wwwRoot
                }) {
                    Icon(Icons.Default.ArrowUpward, "Go up")
                }
                IconButton(onClick = { currentDir = wwwRoot }) {
                    Icon(Icons.Default.Home, "Home")
                }
                IconButton(onClick = { isFolder = false; showCreateDialog = true }) {
                    Icon(Icons.Default.NoteAdd, "New file")
                }
                IconButton(onClick = { isFolder = true; showCreateDialog = true }) {
                    Icon(Icons.Default.CreateNewFolder, "New folder")
                }
            }
        )

        // File List
        if (files.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Empty directory", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(files) { file ->
                    ListItem(
                        headlineContent = {
                            Text(
                                file.name,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        },
                        supportingContent = {
                            if (file.isFile) {
                                Text(
                                    "${file.length()} bytes",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            } else {
                                val count = file.listFiles()?.size ?: 0
                                Text("$count items", style = MaterialTheme.typography.bodySmall)
                            }
                        },
                        leadingContent = {
                            Icon(
                                imageVector = if (file.isDirectory) Icons.Default.Folder else Icons.Default.InsertDriveFile,
                                contentDescription = null,
                                tint = if (file.isDirectory)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingContent = {
                            IconButton(onClick = {
                                selectedFile = file
                                showDeleteDialog = true
                            }) {
                                Icon(Icons.Default.Delete, null)
                            }
                        },
                        modifier = Modifier.clickable {
                            if (file.isDirectory) {
                                currentDir = file
                            } else {
                                Toast.makeText(context, "Selected: ${file.name}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                    Divider()
                }
            }
        }
    }

    // Create File/Folder Dialog
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text(if (isFolder) "Create Folder" else "Create File") },
            text = {
                Column {
                    Text(
                        "Enter ${if (isFolder) "folder" else "file"} name:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newFileName,
                        onValueChange = { newFileName = it },
                        label = { Text(if (isFolder) "folder_name" else "filename.php") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newFileName.isNotBlank()) {
                        val target = File(currentDir, newFileName)
                        if (!target.exists()) {
                            if (isFolder) {
                                target.mkdirs()
                                Toast.makeText(context, "Created folder: $newFileName", Toast.LENGTH_SHORT).show()
                            } else {
                                target.writeText("<?php\necho 'Hello from $newFileName!';\n")
                                Toast.makeText(context, "Created: $newFileName", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    newFileName = ""
                    showCreateDialog = false
                }) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Delete Confirmation
    if (showDeleteDialog && selectedFile != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete") },
            text = { Text("Delete ${selectedFile!!.name}? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    selectedFile?.deleteRecursively()
                    selectedFile = null
                    showDeleteDialog = false
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = {
                    selectedFile = null
                    showDeleteDialog = false
                }) { Text("Cancel") }
            }
        )
    }
}
