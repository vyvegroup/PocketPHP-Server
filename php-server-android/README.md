# PHP Local Server for Android

Run PHP applications directly on your Android device without needing a VPS!

## Features

- 🐘 **PHP 8.2** - Run PHP scripts natively on Android
- 🚀 **NanoHTTPD** - Embedded web server
- 📱 **Material 3** - Modern Android UI with Jetpack Compose
- 📁 **File Manager** - Create, edit, delete PHP files
- 📊 **Server Logs** - Real-time request logging
- ⚙️ **Configurable** - Custom port, PHP settings
- 🔒 **Signed APK** - v1 + v2 + v3 signature schemes

## Quick Start

1. Install the APK on your Android device
2. Tap **Start Server**
3. Open browser → `http://localhost:8080`
4. Use File Manager to create/edit PHP files

## API Endpoints

| Endpoint | Description |
|----------|-------------|
| `GET /` | Default PHP page |
| `GET /phpinfo.php` | PHP information |
| `GET /test.php` | PHP test suite |
| `GET /api/status` | Server status (JSON) |
| `GET /api/phpinfo` | PHP config (JSON) |
| `GET /api/ping` | Health check |

## Build

```bash
./gradlew assembleRelease
```

## Requirements

- Android 8.0+ (API 26)
- GitHub Actions for CI/CD

## License

MIT License
