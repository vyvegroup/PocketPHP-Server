package com.pocketphp.server

/**
 * Utility for determining MIME types of static files.
 */
object MimeTypes {
    private val types = mapOf(
        "html" to "text/html",
        "htm" to "text/html",
        "css" to "text/css",
        "js" to "application/javascript",
        "json" to "application/json",
        "png" to "image/png",
        "jpg" to "image/jpeg",
        "jpeg" to "image/jpeg",
        "gif" to "image/gif",
        "svg" to "image/svg+xml",
        "ico" to "image/x-icon",
        "woff" to "font/woff",
        "woff2" to "font/woff2",
        "ttf" to "font/ttf",
        "otf" to "font/otf",
        "xml" to "application/xml",
        "pdf" to "application/pdf",
        "txt" to "text/plain",
        "zip" to "application/zip",
        "mp3" to "audio/mpeg",
        "mp4" to "video/mp4",
        "webm" to "video/webm",
        "webp" to "image/webp"
    )

    fun getMimeType(fileName: String): String {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return types[ext] ?: "application/octet-stream"
    }
}
