package com.phpserver.android.file

import android.content.Context
import com.phpserver.android.server.ServerState
import java.io.File
import java.io.InputStream

object ProjectManager {

    fun getWwwDirectory(context: Context): File {
        val wwwDir = File(context.filesDir, "www")
        if (!wwwDir.exists()) {
            wwwDir.mkdirs()
            createDefaultFiles(wwwDir)
        }
        return wwwDir
    }

    private fun createDefaultFiles(wwwDir: File) {
        // Create index.php
        File(wwwDir, "index.php").writeText("""
            <?php
            /**
             * PHP Local Server - Default Page
             * Welcome to your local PHP development environment!
             */
            ?>
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>PHP Local Server</title>
                <style>
                    * { margin: 0; padding: 0; box-sizing: border-box; }
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                        background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                        min-height: 100vh;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        color: white;
                    }
                    .container {
                        text-align: center;
                        padding: 40px;
                        background: rgba(255,255,255,0.1);
                        border-radius: 20px;
                        backdrop-filter: blur(10px);
                        max-width: 600px;
                        width: 90%%;
                    }
                    .logo {
                        font-size: 80px;
                        margin-bottom: 20px;
                    }
                    h1 { font-size: 2.5em; margin-bottom: 10px; }
                    p { font-size: 1.2em; opacity: 0.9; margin-bottom: 30px; line-height: 1.6; }
                    .info {
                        background: rgba(255,255,255,0.15);
                        padding: 20px;
                        border-radius: 10px;
                        margin: 20px 0;
                        text-align: left;
                    }
                    .info div { margin: 8px 0; }
                    .badge {
                        display: inline-block;
                        background: rgba(255,255,255,0.2);
                        padding: 5px 15px;
                        border-radius: 20px;
                        font-size: 0.9em;
                        margin: 5px;
                    }
                    .btn {
                        display: inline-block;
                        background: white;
                        color: #764ba2;
                        padding: 12px 30px;
                        border-radius: 30px;
                        text-decoration: none;
                        font-weight: bold;
                        margin: 10px;
                        transition: transform 0.2s;
                    }
                    .btn:hover { transform: scale(1.05); }
                    code {
                        background: rgba(0,0,0,0.3);
                        padding: 2px 6px;
                        border-radius: 4px;
                        font-family: monospace;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="logo">🐘</div>
                    <h1>PHP Local Server</h1>
                    <p>Your PHP development environment is running on Android!</p>
                    
                    <div class="info">
                        <div><strong>PHP Version:</strong> <?php echo phpversion(); ?></div>
                        <div><strong>Server:</strong> <?php echo $_SERVER['SERVER_SOFTWARE'] ?? 'PHP Local Server/1.0'; ?></div>
                        <div><strong>Protocol:</strong> <?php echo $_SERVER['SERVER_PROTOCOL'] ?? 'HTTP/1.1'; ?></div>
                        <div><strong>Document Root:</strong> <?php echo $_SERVER['DOCUMENT_ROOT'] ?? '/var/www/html'; ?></div>
                        <div><strong>Request Time:</strong> <?php echo date('Y-m-d H:i:s'); ?></div>
                    </div>
                    
                    <div>
                        <span class="badge">PHP 8.2</span>
                        <span class="badge">Android</span>
                        <span class="badge">Localhost</span>
                    </div>
                    
                    <br>
                    <a href="/phpinfo.php" class="btn">phpinfo()</a>
                    <a href="/test.php" class="btn">Run Test</a>
                </div>
            </body>
            </html>
        """.trimIndent())

        // Create phpinfo.php
        File(wwwDir, "phpinfo.php").writeText("""
            <?php phpinfo(); ?>
        """.trimIndent())

        // Create test.php
        File(wwwDir, "test.php").writeText("""
            <?php
            /**
             * PHP Environment Test
             */
            ?>
            <!DOCTYPE html>
            <html>
            <head>
                <title>PHP Test</title>
                <style>
                    body { font-family: monospace; padding: 20px; background: #1e1e1e; color: #d4d4d4; }
                    .pass { color: #4ec9b0; }
                    .fail { color: #f48771; }
                    .section { margin: 20px 0; padding: 15px; background: #2d2d2d; border-radius: 8px; }
                    h2 { color: #569cd6; border-bottom: 1px solid #404040; padding-bottom: 10px; }
                    table { width: 100%%; border-collapse: collapse; }
                    td { padding: 5px 10px; border-bottom: 1px solid #404040; }
                </style>
            </head>
            <body>
                <h1>PHP Environment Test</h1>
                
                <div class="section">
                    <h2>PHP Configuration</h2>
                    <table>
                        <tr><td>PHP Version</td><td><?php echo PHP_VERSION; ?></td></tr>
                        <tr><td>PHP SAPI</td><td><?php echo PHP_SAPI; ?></td></tr>
                        <tr><td>Max Execution Time</td><td><?php echo ini_get('max_execution_time'); ?>s</td></tr>
                        <tr><td>Memory Limit</td><td><?php echo ini_get('memory_limit'); ?></td></tr>
                        <tr><td>Upload Max Filesize</td><td><?php echo ini_get('upload_max_filesize'); ?></td></tr>
                        <tr><td>Post Max Size</td><td><?php echo ini_get('post_max_size'); ?></td></tr>
                        <tr><td>Display Errors</td><td><?php echo ini_get('display_errors'); ?></td></tr>
                        <tr><td>Error Reporting</td><td><?php echo error_reporting(); ?></td></tr>
                    </table>
                </div>
                
                <div class="section">
                    <h2>Loaded Extensions</h2>
                    <?php
                    $extensions = get_loaded_extensions();
                    sort($extensions);
                    echo '<p>Total: ' . count($extensions) . ' extensions</p>';
                    foreach ($extensions as $ext) {
                        echo '<span class="pass">' . $ext . '</span> ';
                    }
                    ?>
                </div>
                
                <div class="section">
                    <h2>Functions Test</h2>
                    <?php
                    $tests = [
                        ['JSON Encode', function_exists('json_encode')],
                        ['File Operations', function_exists('fopen')],
                        ['String Functions', function_exists('strlen')],
                        ['Array Functions', function_exists('array_map')],
                        ['Date Functions', function_exists('date')],
                        ['Math Functions', function_exists('sqrt')],
                        ['Regex', function_exists('preg_match')],
                        ['Session', function_exists('session_start')],
                    ];
                    foreach ($tests as $test) {
                        $status = $test[1] ? '<span class="pass">PASS</span>' : '<span class="fail">FAIL</span>';
                        echo "<div>{$test[0]}: $status</div>";
                    }
                    ?>
                </div>
                
                <div class="section">
                    <h2>Server Information</h2>
                    <table>
                        <?php foreach ($_SERVER as $key => $value): ?>
                        <tr><td><?php echo htmlspecialchars($key); ?></td><td><?php echo htmlspecialchars($value); ?></td></tr>
                        <?php endforeach; ?>
                    </table>
                </div>
            </body>
            </html>
        """.trimIndent())

        // Create info.php (API status page)
        File(wwwDir, "info.php").writeText("""
            <?php
            header('Content-Type: application/json');
            echo json_encode([
                'status' => 'ok',
                'php_version' => phpversion(),
                'server_time' => date('c'),
                'memory_usage' => memory_get_usage(true),
                'memory_peak' => memory_get_peak_usage(true),
            ]);
            ?>
        """.trimIndent())

        ServerState.logs.add("[ProjectManager] Created default PHP files in www directory")
    }

    fun listFiles(dir: File): List<FileInfo> {
        return dir.listFiles()
            ?.filter { it.isFile }
            ?.sortedByDescending { it.lastModified() }
            ?.map { FileInfo(it.name, it.length(), it.lastModified(), it.extension) }
            ?: emptyList()
    }

    fun listDirectories(dir: File): List<FileInfo> {
        return dir.listFiles()
            ?.filter { it.isDirectory }
            ?.sorted()
            ?.map { FileInfo(it.name, 0, it.lastModified(), "dir") }
            ?: emptyList()
    }

    fun createFile(dir: File, filename: String, content: String): Boolean {
        return try {
            File(dir, filename).writeText(content)
            ServerState.logs.add("[FileManager] Created: $filename")
            true
        } catch (e: Exception) {
            ServerState.logs.add("[FileManager] Error creating $filename: ${e.message}")
            false
        }
    }

    fun readFile(file: File): String {
        return try {
            file.readText()
        } catch (e: Exception) {
            "// Error reading file: ${e.message}"
        }
    }

    fun deleteFile(file: File): Boolean {
        return try {
            file.delete()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun createDirectory(parent: File, name: String): Boolean {
        return File(parent, name).mkdirs()
    }

    fun importFile(dir: File, filename: String, inputStream: InputStream): Boolean {
        return try {
            val file = File(dir, filename)
            file.outputStream().use { output ->
                inputStream.copyTo(output)
            }
            ServerState.logs.add("[FileManager] Imported: $filename")
            true
        } catch (e: Exception) {
            false
        }
    }
}

data class FileInfo(
    val name: String,
    val size: Long,
    val lastModified: Long,
    val extension: String
)
