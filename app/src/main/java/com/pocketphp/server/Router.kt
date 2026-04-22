package com.pocketphp.server

import java.util.regex.Pattern

/**
 * Custom router for PHP server - supports URL rewriting without .htaccess.
 * Routes like /users/profile/123 are mapped to PHP handlers.
 */
class Router {

    data class RouteMatch(
        val target: String,
        val params: Map<String, String>
    )

    private data class Route(
        val pattern: Pattern,
        val target: String,
        val paramNames: List<String>
    )

    private val routes = mutableListOf<Route>()

    /**
     * Add a route with parameter placeholders.
     * Example: addRoute("/api/{action}/{id}", "/api.php")
     * This will match "/api/users/123" and pass action=users, id=123 to api.php
     */
    fun addRoute(pattern: String, target: String) {
        val regexBuilder = StringBuilder("^")
        val paramNames = mutableListOf<String>()

        val parts = pattern.split("/").filter { it.isNotEmpty() }
        for (part in parts) {
            regexBuilder.append("/")
            if (part.startsWith("{") && part.endsWith("}")) {
                val paramName = part.substring(1, part.length - 1)
                paramNames.add(paramName)
                regexBuilder.append("([^/]+)")
            } else {
                regexBuilder.append(Pattern.quote(part))
            }
        }
        regexBuilder.append("/?$")
        routes.add(Route(Pattern.compile(regexBuilder.toString()), target, paramNames))
    }

    /**
     * Add a static route (exact match).
     */
    fun addStaticRoute(path: String, target: String) {
        val escaped = "^${Pattern.quote(path)}/?$"
        routes.add(0, Route(Pattern.compile(escaped), target, emptyList()))
    }

    /**
     * Match a URI path to a route.
     * Returns null if no route matches.
     */
    fun match(uri: String): RouteMatch? {
        // Remove query string
        val path = uri.split("?").first()

        for (route in routes) {
            val matcher = route.pattern.matcher(path)
            if (matcher.matches()) {
                val params = mutableMapOf<String, String>()
                for ((i, name) in route.paramNames.withIndex()) {
                    if (i + 1 <= matcher.groupCount()) {
                        params[name] = matcher.group(i + 1) ?: ""
                    }
                }
                return RouteMatch(route.target, params)
            }
        }
        return null
    }

    /**
     * Try file-based fallback: check if path maps to an existing PHP file.
     * Example: /about -> about.php, /api/users -> api/users.php
     */
    fun fileFallback(uri: String, rootDir: String): String? {
        val path = uri.split("?").first().trim('/')
        if (path.isEmpty()) return "$rootDir/index.php"

        // Try exact .php file
        val candidates = listOf(
            "$rootDir/$path.php",
            "$rootDir/$path/index.php"
        )
        for (candidate in candidates) {
            if (java.io.File(candidate).exists()) return candidate
        }
        return null
    }

    /**
     * Load routes from a routes.php configuration file.
     * Expected format: return an associative array of pattern => target.
     */
    fun loadFromConfig(configContent: String) {
        val lines = configContent.lines()
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("//") || trimmed.startsWith("#")) continue
            val match = Regex("'([^']+)'\\s*=>\\s*'([^']+)'").find(trimmed)
            if (match != null) {
                addRoute(match.groupValues[1], match.groupValues[2])
            }
        }
    }

    companion object {
        /**
         * Create a default router with common PHP framework routes.
         */
        fun createDefault(): Router {
            val router = Router()
            // Static routes
            router.addStaticRoute("/", "/index.php")
            router.addStaticRoute("/index.php", "/index.php")

            // Common framework-style routes: /controller/action/params
            router.addRoute("/{controller}/{action}/{id}", "/index.php")
            router.addRoute("/{controller}/{action}", "/index.php")
            router.addRoute("/{controller}", "/index.php")

            // API routes
            router.addRoute("/api/{version}/{endpoint}", "/api/index.php")
            router.addRoute("/api/{endpoint}", "/api/index.php")

            // RESTful patterns
            router.addRoute("/api/{resource}/{id}", "/api/index.php")
            return router
        }
    }
}
