package com.phpserver.android.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.phpserver.android.file.FileInfo
import com.phpserver.android.file.ProjectManager
import com.phpserver.android.server.ServerService
import com.phpserver.android.server.ServerState
import com.phpserver.android.server.ServerStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class ServerViewModel(application: Application) : AndroidViewModel(application) {

    private var serverService: ServerService? = null

    private val _serverStatus = MutableStateFlow(ServerStatus(false, 8080, 0, 0))
    val serverStatus: StateFlow<ServerStatus> = _serverStatus.asStateFlow()

    private val _files = MutableStateFlow<List<FileInfo>>(emptyList())
    val files: StateFlow<List<FileInfo>> = _files.asStateFlow()

    private val _directories = MutableStateFlow<List<FileInfo>>(emptyList())
    val directories: StateFlow<List<FileInfo>> = _directories.asStateFlow()

    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    private val _currentPath = MutableStateFlow("")
    val currentPath: StateFlow<String> = _currentPath.asStateFlow()

    private val _port = MutableStateFlow(8080)
    val port: StateFlow<Int> = _port.asStateFlow()

    private val _isServerStarting = MutableStateFlow(false)
    val isServerStarting: StateFlow<Boolean> = _isServerStarting.asStateFlow()

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as ServerService.LocalBinder
            serverService = binder.getService()
            refreshServerStatus()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            serverService = null
        }
    }

    init {
        val wwwDir = ProjectManager.getWwwDirectory(getApplication())
        _currentPath.value = wwwDir.absolutePath
        loadFiles(wwwDir)
    }

    fun startServer(port: Int = _port.value) {
        viewModelScope.launch(Dispatchers.IO) {
            _isServerStarting.value = true
            ServerState.port = port

            val intent = Intent(getApplication(), ServerService::class.java).apply {
                action = ServerService.ACTION_START
                putExtra("port", port)
            }

            getApplication<Application>().startForegroundService(intent)

            // Try to bind
            getApplication<Application>().bindService(
                intent,
                serviceConnection,
                Context.BIND_AUTO_CREATE
            )

            kotlinx.coroutines.delay(2000)
            _isServerStarting.value = false
            refreshServerStatus()
        }
    }

    fun stopServer() {
        viewModelScope.launch(Dispatchers.IO) {
            val intent = Intent(getApplication(), ServerService::class.java).apply {
                action = ServerService.ACTION_STOP
            }
            getApplication<Application>().startForegroundService(intent)

            try {
                getApplication<Application>().unbindService(serviceConnection)
            } catch (_: Exception) {}

            serverService = null
            refreshServerStatus()
        }
    }

    fun refreshServerStatus() {
        serverService?.let {
            _serverStatus.value = it.getServerStatus()
        }
        _logs.value = ServerState.logs.toList()
    }

    fun loadFiles(directory: File) {
        viewModelScope.launch(Dispatchers.IO) {
            _directories.value = ProjectManager.listDirectories(directory)
            _files.value = ProjectManager.listFiles(directory)
            _currentPath.value = directory.absolutePath
        }
    }

    fun navigateToDirectory(directory: FileInfo) {
        val dir = File(_currentPath.value, directory.name)
        loadFiles(dir)
    }

    fun navigateUp() {
        val current = File(_currentPath.value)
        val parent = current.parentFile
        if (parent != null && parent.canRead()) {
            loadFiles(parent)
        }
    }

    fun createFile(filename: String, template: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            ProjectManager.createFile(File(_currentPath.value), filename, template)
            loadFiles(File(_currentPath.value))
        }
    }

    fun readFile(filename: String): String {
        return ProjectManager.readFile(File(_currentPath.value, filename))
    }

    fun deleteFile(filename: String) {
        viewModelScope.launch(Dispatchers.IO) {
            ProjectManager.deleteFile(File(_currentPath.value, filename))
            loadFiles(File(_currentPath.value))
        }
    }

    fun updateFile(filename: String, content: String) {
        viewModelScope.launch(Dispatchers.IO) {
            ProjectManager.createFile(File(_currentPath.value), filename, content)
        }
    }

    fun createDirectory(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            ProjectManager.createDirectory(File(_currentPath.value), name)
            loadFiles(File(_currentPath.value))
        }
    }

    fun setPort(port: Int) {
        _port.value = port
    }

    fun clearLogs() {
        ServerState.logs.clear()
        _logs.value = emptyList()
    }

    fun getDefaultPath(): File {
        return ProjectManager.getWwwDirectory(getApplication())
    }

    override fun onCleared() {
        try {
            getApplication<Application>().unbindService(serviceConnection)
        } catch (_: Exception) {}
        super.onCleared()
    }
}
