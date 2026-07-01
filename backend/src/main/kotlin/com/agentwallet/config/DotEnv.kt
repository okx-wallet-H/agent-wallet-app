package com.agentwallet.config

import java.io.File

/**
 * Minimal .env file loader. Reads KEY=VALUE pairs and sets them as system properties.
 * Production should use real env vars. This is for local dev convenience.
 */
object DotEnv {
    fun load(path: String = ".env") {
        val file = File(path)
        if (!file.exists()) return

        file.readLines().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) return@forEach

            val eq = trimmed.indexOf('=')
            if (eq < 0) return@forEach

            val key = trimmed.substring(0, eq).trim()
            val value = trimmed.substring(eq + 1).trim()

            // Only set if not already set (env vars take priority)
            if (System.getenv(key) == null) {
                System.setProperty(key, value)
            }
        }
    }
}
