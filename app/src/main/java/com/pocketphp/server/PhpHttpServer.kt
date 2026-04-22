package com.pocketphp.server

import android.content.Context
import android.util.Log
import fi.iki.elonen.NanoHTTPD
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException

/**
 * NanoHTTPD-based PHP web server with integrated router.
 * Supports URL rewriting like /users/profile/123 without .htaccess.
 */
class PhpHttpServer(
    private val context: Context,
    private val port: Int = 8080
) : NanoHTTPD(port) {

    companion object {
        private const val TAG = "PhpHttpServer"
        private const val DEFAULT_PORT = 8080
    }

    private val router: Router = Router.createDefault()
    private val phpExecutor: PhpExecutor = PhpExecutor(context)
    private val wwwRoot: File = File(context.filesDir, "www")
    private var serverStatus: ServerStatus = ServerStatus.STOPPED
    private var statusListener: ((ServerStatus) -> Unit)? = null

    enum class ServerStatus {
        STOPPED, STARTING, RUNNING, ERROR
    }

    // Log callback
    var logCallback: ((String) -> Unit)? = null

    override fun start() {
        if (!wwwRoot.exists()) {
            wwwRoot.mkdirs()
            extractDefaultFiles()
        }
        serverStatus = ServerStatus.STARTING
        statusListener?.invoke(serverStatus)
        try {
            super.start(SOCKET_READ_TIMEOUT, false)
            serverStatus = ServerStatus.RUNNING
            statusListener?.invoke(serverStatus)
            log("Server started on port $port")
        } catch (e: IOException) {
            serverStatus = ServerStatus.ERROR
            statusListener?.invoke(serverStatus)
            Log.e(TAG, "Failed to start server", e)
            log("ERROR: Failed to start server - ${e.message}")
        }
    }

    override fun stop() {
        super.stop()
        serverStatus = ServerStatus.STOPPED
        statusListener?.invoke(serverStatus)
        log("Server stopped")
    }

    fun getStatus(): ServerStatus = serverStatus

    fun setOnStatusChangeListener(listener: (ServerStatus) -> Unit) {
        statusListener = listener
    }

    fun getRouter(): Router = router

    fun getWwwRoot(): File = wwwRoot

    fun getPhpExecutor(): PhpExecutor = phpExecutor

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        val method = session.method.name

        log("[$method] $uri")

        // Extract query string
        val queryString = session.queryParameterString ?: ""
        val remoteIp = session.remoteIpAddress ?: "unknown"

        // Route the request
        val routeMatch = router.match(uri)

        // Determine which PHP file to execute
        val targetFile: String? = if (routeMatch != null) {
            val target = routeMatch.target
            val targetPath = File(wwwRoot, target.removePrefix("/"))
            if (targetPath.exists()) {
                targetPath.absolutePath
            } else {
                router.fileFallback(uri, wwwRoot.absolutePath)
            }
        } else {
            // No route match - try file-based fallback
            router.fileFallback(uri, wwwRoot.absolutePath)
        }

        if (targetFile != null && File(targetFile).exists()) {
            return servePhpFile(session, targetFile, uri, queryString, routeMatch?.params, remoteIp)
        }

        // Try serving static files
        val staticFile = File(wwwRoot, uri.removePrefix("/"))
        if (staticFile.exists() && staticFile.isFile && !staticFile.name.endsWith(".php")) {
            return serveStaticFile(staticFile)
        }

        // Check for routes.php in www root to try user-defined routes
        val routesFile = File(wwwRoot, "routes.php")
        if (routesFile.exists()) {
            routesFile.readText().let { router.loadFromConfig(it) }
            val retryMatch = router.match(uri)
            if (retryMatch != null) {
                val retryTarget = File(wwwRoot, retryMatch.target.removePrefix("/"))
                if (retryTarget.exists()) {
                    return servePhpFile(session, retryTarget.absolutePath, uri, queryString, retryMatch.params, remoteIp)
                }
            }
        }

        // 404 Not Found
        log("404 Not Found: $uri")
        return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/html",
            "<h1>404 Not Found</h1><p>The requested URL $uri was not found on this server.</p>"
        )
    }

    private fun servePhpFile(
        session: IHTTPSession,
        scriptPath: String,
        uri: String,
        queryString: String,
        params: Map<String, String>?,
        remoteIp: String
    ): Response {
        if (!phpExecutor.isPhpInstalled()) {
            return newFixedLengthResponse(Response.Status.SERVICE_UNAVAILABLE, "text/html",
                "<h1>PHP Not Installed</h1><p>Please install PHP from the app settings.</p>"
            )
        }

        // Build CGI server variables
        val serverVars = mutableMapOf(
            "SCRIPT_FILENAME" to scriptPath,
            "SCRIPT_NAME" to uri,
            "REQUEST_URI" to "$uri${if (queryString.isNotEmpty()) "?$queryString" else ""}",
            "QUERY_STRING" to queryString,
            "REQUEST_METHOD" to session.method.name,
            "REMOTE_ADDR" to remoteIp,
            "SERVER_NAME" to "localhost",
            "SERVER_PORT" to port.toString(),
            "SERVER_ADDR" to "127.0.0.1",
            "HTTP_HOST" to "localhost:$port",
            "HTTP_ACCEPT" to session.headers.get("accept") ?: "*/*",
            "HTTP_USER_AGENT" to session.headers.get("user-agent") ?: "PocketPHP/1.0"
        )

        // Add route params as GET variables
        params?.forEach { (key, value) ->
            serverVars["ROUTE_PARAM_$key"] = value
        }

        // Handle POST data
        var postData: String? = null
        if (session.method == Method.POST || session.method == Method.PUT) {
            val contentLength = session.headers.get("content-length", "0")
            if (contentLength.toIntOrNull()?.let { it > 0 } == true) {
                val buf = ByteArrayOutputStream()
                session.inputStream?.copyTo(buf)
                postData = buf.toString("UTF-8")
                serverVars["CONTENT_TYPE"] = session.headers.get("content-type", "application/x-www-form-urlencoded")
                serverVars["CONTENT_LENGTH"] = contentLength
            }
        }

        val rawOutput = phpExecutor.execute(scriptPath, serverVars, postData)
        val headers = phpExecutor.parseHeaders(rawOutput)

        // Determine content type from headers or default to text/html
        val contentType = headers["Content-Type"] ?: "text/html; charset=UTF-8"
        val statusCode = parseStatus(headers["X-PHP-Status"])

        log("PHP response: ${statusCode ?: 200} for $uri")

        return newFixedLengthResponse(statusCode ?: Response.Status.OK, contentType, rawOutput)
    }

    private fun serveStaticFile(file: File): Response {
        return try {
            val contentType = guessContentType(file.name)
            val fis = FileInputStream(file)
            newChunkedResponse(Response.Status.OK, contentType, fis)
        } catch (e: IOException) {
            newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Error reading file")
        }
    }

    private fun parseStatus(statusStr: String?): Response.Status? {
        if (statusStr == null) return null
        val code = statusStr.split(" ").firstOrNull()?.toIntOrNull() ?: return null
        return when (code) {
            200 -> Response.Status.OK
            301 -> Response.Status.REDIRECT
            302 -> Response.Status.REDIRECT
            304 -> Response.Status.NOT_MODIFIED
            400 -> Response.Status.BAD_REQUEST
            401 -> Response.Status.UNAUTHORIZED
            403 -> Response.Status.FORBIDDEN
            404 -> Response.Status.NOT_FOUND
            500 -> Response.Status.INTERNAL_ERROR
            else -> null
        }
    }

    private fun guessContentType(fileName: String): String {
        return when {
            fileName.endsWith(".html") || fileName.endsWith(".htm") -> "text/html; charset=UTF-8"
            fileName.endsWith(".css") -> "text/css; charset=UTF-8"
            fileName.endsWith(".js") -> "application/javascript; charset=UTF-8"
            fileName.endsWith(".json") -> "application/json; charset=UTF-8"
            fileName.endsWith(".png") -> "image/png"
            fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") -> "image/jpeg"
            fileName.endsWith(".gif") -> "image/gif"
            fileName.endsWith(".svg") -> "image/svg+xml"
            fileName.endsWith(".ico") -> "image/x-icon"
            fileName.endsWith(".woff") -> "font/woff"
            fileName.endsWith(".woff2") -> "font/woff2"
            fileName.endsWith(".ttf") -> "font/ttf"
            fileName.endsWith(".xml") -> "application/xml"
            fileName.endsWith(".pdf") -> "application/pdf"
            fileName.endsWith(".txt") -> "text/plain; charset=UTF-8"
            else -> "application/octet-stream"
        }
    }

    private fun extractDefaultFiles() {
        try {
            context.assets.open("www/index.php").use { input ->
                File(wwwRoot, "index.php").outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            context.assets.open("www/info.php").use { input ->
                File(wwwRoot, "info.php").outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            context.assets.open("www/routes.php").use { input ->
                File(wwwRoot, "routes.php").outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error extracting default files", e)
        }
    }

    private fun log(message: String) {
        Log.d(TAG, message)
        logCallback?.invoke(message)
    }
}
