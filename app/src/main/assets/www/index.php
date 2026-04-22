<?php
/**
 * PocketPHP - Default Index Page
 * This is the default page served by PocketPHP Server.
 */

// Get route parameters (set by the router)
$controller = isset($_SERVER['ROUTE_PARAM_controller']) ? $_SERVER['ROUTE_PARAM_controller'] : 'home';
$action = isset($_SERVER['ROUTE_PARAM_action']) ? $_SERVER['ROUTE_PARAM_action'] : 'index';
$id = isset($_SERVER['ROUTE_PARAM_id']) ? $_SERVER['ROUTE_PARAM_id'] : null;

// Parse query string
parse_str($_SERVER['QUERY_STRING'] ?? '', $query);

header('Content-Type: text/html; charset=UTF-8');

?>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>PocketPHP Server</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
            min-height: 100vh;
            color: #fff;
            padding: 20px;
        }
        .container { max-width: 800px; margin: 0 auto; }
        .header {
            text-align: center;
            padding: 40px 20px;
            margin-bottom: 30px;
        }
        .header h1 {
            font-size: 2.5em;
            background: linear-gradient(90deg, #e94560, #533483);
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
            margin-bottom: 10px;
        }
        .header p { color: #a8a8b3; font-size: 1.1em; }
        .card {
            background: rgba(255,255,255,0.05);
            border: 1px solid rgba(255,255,255,0.1);
            border-radius: 12px;
            padding: 24px;
            margin-bottom: 20px;
            backdrop-filter: blur(10px);
        }
        .card h2 {
            color: #e94560;
            margin-bottom: 12px;
            font-size: 1.3em;
        }
        .card pre {
            background: rgba(0,0,0,0.3);
            padding: 16px;
            border-radius: 8px;
            overflow-x: auto;
            font-size: 0.9em;
            color: #4ecca3;
        }
        .card code { color: #4ecca3; }
        .card p { line-height: 1.6; color: #c8c8d0; }
        .status {
            display: inline-block;
            padding: 4px 12px;
            border-radius: 20px;
            font-size: 0.85em;
            font-weight: 600;
        }
        .status.online { background: rgba(78,204,163,0.2); color: #4ecca3; }
        .routes { display: grid; gap: 8px; }
        .route {
            display: flex;
            justify-content: space-between;
            padding: 8px 12px;
            background: rgba(0,0,0,0.2);
            border-radius: 6px;
        }
        .route .pattern { color: #e94560; font-family: monospace; }
        .route .target { color: #4ecca3; font-family: monospace; }
        .php-info { margin-top: 10px; }
        .php-info a {
            color: #e94560;
            text-decoration: none;
            display: inline-block;
            padding: 8px 16px;
            border: 1px solid #e94560;
            border-radius: 6px;
            transition: all 0.3s;
        }
        .php-info a:hover { background: #e94560; color: #fff; }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>PocketPHP Server</h1>
            <p>PHP Running on Android</p>
            <br>
            <span class="status online">&#9679; Online</span>
        </div>

        <div class="card">
            <h2>Welcome!</h2>
            <p>
                Your PHP server is running successfully on your Android device.
                You can deploy any PHP project to the www directory and access it
                through this server. The built-in router handles URL rewriting
                without needing .htaccess.
            </p>
        </div>

        <div class="card">
            <h2>Route Parameters Detected</h2>
            <pre>
Controller: <?php echo htmlspecialchars($controller); ?>
Action:     <?php echo htmlspecialchars($action); ?>
<?php if ($id): ?>ID:         <?php echo htmlspecialchars($id); ?>
<?php endif; ?>
Query:      <?php echo htmlspecialchars($_SERVER['QUERY_STRING'] ?: 'none'); ?>
Request:    <?php echo htmlspecialchars($_SERVER['REQUEST_METHOD']); ?>
            </pre>
        </div>

        <div class="card">
            <h2>Built-in Router Patterns</h2>
            <div class="routes">
                <div class="route">
                    <span class="pattern">/{controller}/{action}/{id}</span>
                    <span class="target">index.php</span>
                </div>
                <div class="route">
                    <span class="pattern">/{controller}/{action}</span>
                    <span class="target">index.php</span>
                </div>
                <div class="route">
                    <span class="pattern">/{controller}</span>
                    <span class="target">index.php</span>
                </div>
                <div class="route">
                    <span class="pattern">/api/{resource}/{id}</span>
                    <span class="target">api/index.php</span>
                </div>
            </div>
        </div>

        <div class="card">
            <h2>PHP Info</h2>
            <p>Server time: <strong><?php echo date('Y-m-d H:i:s'); ?></strong></p>
            <p>PHP version: <strong><?php echo phpversion(); ?></strong></p>
            <div class="php-info">
                <a href="/info.php">View phpinfo()</a>
            </div>
        </div>

        <div class="card">
            <h2>Try These URLs</h2>
            <pre>
/users/profile/123    -> controller=users, action=profile, id=123
/api/posts            -> api/index.php
/about                -> about.php (if exists)
            </pre>
        </div>
    </div>
</body>
</html>
