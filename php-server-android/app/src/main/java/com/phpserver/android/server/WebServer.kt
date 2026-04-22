package com.phpserver.android.server

import android.content.Context
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.*
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

class WebServer(private val port: Int) : NanoHTTPD(port) {

    private val startTime = System.currentTimeMillis()
    private val requestCounter = AtomicInteger(0)
    private val phpEngine = PhpEngine()

    override fun serve(session: IHTTPSession): Response {
        requestCounter.incrementAndGet()

        val uri = session.uri
        val method = session.method.name
        val clientAddress = session.remoteIpAddress

        logRequest(method, uri, clientAddress)

        return try {
            when {
                uri == "/" || uri == "/index.php" -> servePhpFile("index.php", session)
                uri.endsWith(".php") -> servePhpFile(uri.removePrefix("/"), session)
                uri.startsWith("/api/") -> serveApiRequest(uri, session)
                else -> serveStaticFile(uri.removePrefix("/"), session)
            }
        } catch (e: Exception) {
            logError("$method $uri - ${e.message}")
            newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain",
                "500 Internal Server Error\n${e.message}")
        }
    }

    private fun servePhpFile(filename: String, session: IHTTPSession): Response {
        val context = PhpServerApp.instance
        val wwwDir = File(context.filesDir, "www")

        val phpFile = File(wwwDir, filename)
        if (!phpFile.exists()) {
            val defaultPhp = File(wwwDir, "index.php")
            if (defaultPhp.exists()) {
                return executePhp(defaultPhp, session)
            }
            return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/html",
                "<h1>404 - File Not Found</h1><p>$filename not found. Place your PHP files in the www directory.</p>")
        }

        return executePhp(phpFile, session)
    }

    private fun executePhp(phpFile: File, session: IHTTPSession): Response {
        val output = phpEngine.execute(phpFile, session)
        ServerState.logs.add("[${timestamp()}] PHP: ${phpFile.name} -> ${output.length} bytes")
        return newFixedLengthResponse(Response.Status.OK, "text/html", output)
    }

    private fun serveStaticFile(filename: String, session: IHTTPSession): Response {
        val wwwDir = File(PhpServerApp.instance.filesDir, "www")
        val file = File(wwwDir, filename)

        if (!file.exists() || !file.isFile) {
            return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/html",
                "<h1>404 - Not Found</h1>")
        }

        val contentType = when {
            filename.endsWith(".html") || filename.endsWith(".htm") -> "text/html"
            filename.endsWith(".css") -> "text/css"
            filename.endsWith(".js") -> "application/javascript"
            filename.endsWith(".json") -> "application/json"
            filename.endsWith(".png") -> "image/png"
            filename.endsWith(".jpg") || filename.endsWith(".jpeg") -> "image/jpeg"
            filename.endsWith(".gif") -> "image/gif"
            filename.endsWith(".svg") -> "image/svg+xml"
            filename.endsWith(".ico") -> "image/x-icon"
            filename.endsWith(".xml") -> "application/xml"
            filename.endsWith(".txt") -> "text/plain"
            filename.endsWith(".pdf") -> "application/pdf"
            else -> "application/octet-stream"
        }

        return try {
            val fis = FileInputStream(file)
            newChunkedResponse(Response.Status.OK, contentType, fis)
        } catch (e: Exception) {
            newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Error reading file")
        }
    }

    private fun serveApiRequest(uri: String, session: IHTTPSession): Response {
        val apiPath = uri.removePrefix("/api/")
        val response = when (apiPath) {
            "status" -> """{"status":"running","port":$port,"php_version":"${ServerState.phpVersion}","uptime":${getUptime()},"requests":${requestCounter.get()}}"""
            "phpinfo" -> phpEngine.getPhpInfo()
            "ping" -> """{"pong":true,"time":"${timestamp()}"}"""
            "logs" -> """{"logs":${ServerState.logs.takeLast(50).joinToString(",") { "\"$it\"" }}}}"""
            else -> """{"error":"Unknown endpoint: $apiPath"}"""
        }
        return newFixedLengthResponse(Response.Status.OK, "application/json", response)
    }

    private fun logRequest(method: String, uri: String, ip: String) {
        ServerState.logs.add("[${timestamp()}] $method $uri - $ip")
        if (ServerState.logs.size > 500) {
            ServerState.logs.removeRange(0, 50)
        }
    }

    private fun logError(message: String) {
        ServerState.logs.add("[${timestamp()}] ERROR: $message")
    }

    private fun timestamp(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
    }

    fun getUptime(): Long = (System.currentTimeMillis() - startTime) / 1000

    fun getRequestCount(): Int = requestCounter.get()
}
