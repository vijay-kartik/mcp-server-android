package com.mcpserver.android

import com.mcpserver.android.server.McpHttpServer
import com.mcpserver.android.tools.AutomationTools
import kotlinx.coroutines.*
import android.util.Log

/**
 * Main API for the Embedded MCP Server library.
 * Provides a simple interface for Android apps to start and manage
 * an in-process MCP (Model Context Protocol) server.
 *
 * Usage:
 * ```kotlin
 * // Start the server
 * EmbeddedMcpServer.start(port = 12345)
 *
 * // Get the server URL for agent integration
 * val serverUrl = EmbeddedMcpServer.getServerUrl() // "http://localhost:12345"
 *
 * // Stop the server when done
 * EmbeddedMcpServer.stop()
 * ```
 */
object EmbeddedMcpServer {
    private const val TAG = "EmbeddedMcpServer"
    private var mcpServer: McpHttpServer? = null
    private var serverScope: CoroutineScope? = null

    /**
     * Start the embedded MCP server on the specified port.
     *
     * @param port The port number to bind the server to (default: 12345)
     * @param timeout Maximum time to wait for server startup in milliseconds (default: 5000)
     * @throws IllegalStateException if server is already running
     * @throws IllegalArgumentException if port is invalid
     * @throws RuntimeException if server fails to start within timeout
     */
    @JvmStatic
    @JvmOverloads
    fun start(port: Int = 12345, timeout: Long = 5000) {
        if (isRunning()) {
            throw IllegalStateException("MCP server is already running on port ${getCurrentPort()}")
        }

        if (port !in 1024..65535) {
            throw IllegalArgumentException("Port must be between 1024 and 65535, got: $port")
        }

        Log.i(TAG, "Starting embedded MCP server on port $port")

        try {
            // Create server scope for coroutines
            serverScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

            // Create and start the server
            mcpServer = McpHttpServer(port)

            // Start server with timeout
            runBlocking {
                withTimeout(timeout) {
                    mcpServer?.start()
                }
            }

            Log.i(TAG, "Embedded MCP server started successfully at ${getServerUrl()}")

        } catch (e: TimeoutCancellationException) {
            cleanup()
            throw RuntimeException("Server failed to start within ${timeout}ms", e)
        } catch (e: Exception) {
            cleanup()
            Log.e(TAG, "Failed to start embedded MCP server", e)
            throw RuntimeException("Failed to start MCP server: ${e.message}", e)
        }
    }

    /**
     * Stop the embedded MCP server.
     * This method is safe to call multiple times.
     */
    @JvmStatic
    fun stop() {
        if (!isRunning()) {
            Log.d(TAG, "MCP server is not running, stop() called redundantly")
            return
        }

        Log.i(TAG, "Stopping embedded MCP server")

        try {
            mcpServer?.stop()
            cleanup()
            Log.i(TAG, "Embedded MCP server stopped successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping MCP server", e)
            cleanup()
            throw RuntimeException("Error stopping MCP server: ${e.message}", e)
        }
    }

    /**
     * Check if the MCP server is currently running.
     *
     * @return true if the server is running, false otherwise
     */
    @JvmStatic
    fun isRunning(): Boolean {
        return mcpServer?.isRunning() == true
    }

    /**
     * Get the server URL if running.
     * This URL can be passed to AI agents or MCP clients.
     *
     * @return the server URL (e.g., "http://127.0.0.1:12345") or null if not running
     */
    @JvmStatic
    fun getServerUrl(): String? {
        return mcpServer?.getServerUrl()
    }

    /**
     * Get the current port number if server is running.
     *
     * @return the port number or null if not running
     */
    @JvmStatic
    fun getCurrentPort(): Int? {
        return getServerUrl()?.substringAfterLast(":")?.toIntOrNull()
    }

    /**
     * Get information about available automation tools.
     * Useful for debugging or displaying capabilities to users.
     *
     * @return list of tool names and descriptions
     */
    @JvmStatic
    fun getAvailableTools(): List<ToolInfo> {
        val tools = AutomationTools().availableTools
        return tools.map { tool ->
            ToolInfo(
                name = tool.name,
                description = tool.description,
                requiredParameters = tool.inputSchema.required,
                allParameters = tool.inputSchema.properties.keys.toList()
            )
        }
    }

    /**
     * Restart the server on a different port.
     * Convenience method that stops the current server and starts on new port.
     *
     * @param newPort the new port to use
     * @param timeout timeout for startup in milliseconds
     */
    @JvmStatic
    @JvmOverloads
    fun restart(newPort: Int, timeout: Long = 5000) {
        Log.i(TAG, "Restarting MCP server on port $newPort")
        stop()
        start(newPort, timeout)
    }

    /**
     * Clean up server resources
     */
    private fun cleanup() {
        mcpServer = null
        serverScope?.cancel()
        serverScope = null
    }
}

/**
 * Information about an available automation tool
 */
data class ToolInfo(
    val name: String,
    val description: String,
    val requiredParameters: List<String>,
    val allParameters: List<String>
)