# Example Usage

This document provides detailed examples of how to integrate and use the Android MCP Server library.

## Basic Integration Example

### MainActivity.kt
```kotlin
package com.example.myapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mcpserver.android.EmbeddedMcpServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private val mainScope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Start MCP server when app launches
        startMcpServer()
    }

    private fun startMcpServer() {
        mainScope.launch {
            try {
                // Start server on port 12345
                EmbeddedMcpServer.start(port = 12345)

                val serverUrl = EmbeddedMcpServer.getServerUrl()
                println("MCP Server started at: $serverUrl")

                // Display available tools
                val tools = EmbeddedMcpServer.getAvailableTools()
                tools.forEach { tool ->
                    println("Available tool: ${tool.name} - ${tool.description}")
                }

                // Initialize your AI agent with the server URL
                initializeAiAgent(serverUrl!!)

            } catch (e: Exception) {
                println("Failed to start MCP server: ${e.message}")
            }
        }
    }

    private fun initializeAiAgent(serverUrl: String) {
        // Example: Configure your AI agent to use the MCP server
        // This would depend on your specific AI framework

        // For ADK integration:
        // val mcpToolset = MCPToolset(serverUrl = serverUrl)
        // myAgent.addToolset(mcpToolset)

        // For custom HTTP client:
        // myAiClient.configureMcpEndpoint(serverUrl)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Always stop the server when app is destroyed
        EmbeddedMcpServer.stop()
    }
}
```

## Advanced Configuration Example

### Application.kt
```kotlin
package com.example.myapp

import android.app.Application
import com.mcpserver.android.EmbeddedMcpServer

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Start MCP server early in app lifecycle
        setupMcpServer()
    }

    private fun setupMcpServer() {
        try {
            // Use dynamic port assignment for flexibility
            val port = findAvailablePort()
            EmbeddedMcpServer.start(port = port, timeout = 10000)

            // Store server URL for other components
            val serverUrl = EmbeddedMcpServer.getServerUrl()
            storeServerUrl(serverUrl)

        } catch (e: Exception) {
            // Handle startup failure gracefully
            handleMcpStartupFailure(e)
        }
    }

    private fun findAvailablePort(): Int {
        // Try common ports, fall back to default
        val candidatePorts = listOf(12345, 12346, 12347, 8080, 8081)

        for (port in candidatePorts) {
            try {
                EmbeddedMcpServer.start(port = port)
                EmbeddedMcpServer.stop() // Test successful, stop and return port
                return port
            } catch (e: Exception) {
                // Port unavailable, try next
                continue
            }
        }

        return 12345 // Default fallback
    }

    private fun storeServerUrl(serverUrl: String?) {
        // Store in shared preferences or dependency injection container
        getSharedPreferences("mcp_config", MODE_PRIVATE)
            .edit()
            .putString("server_url", serverUrl)
            .apply()
    }

    private fun handleMcpStartupFailure(error: Exception) {
        // Log error, show user notification, or fallback to non-MCP mode
        android.util.Log.e("MCP", "Failed to start MCP server", error)
    }

    override fun onTerminate() {
        super.onTerminate()
        EmbeddedMcpServer.stop()
    }
}
```

## Service Integration Example

### McpServerService.kt
```kotlin
package com.example.myapp.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.mcpserver.android.EmbeddedMcpServer
import kotlinx.coroutines.*

/**
 * Android Service wrapper for MCP server management.
 * Useful for long-running automation scenarios.
 */
class McpServerService : Service() {

    private val binder = McpServerBinder()
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    inner class McpServerBinder : Binder() {
        fun getService(): McpServerService = this@McpServerService
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        startMcpServer()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopMcpServer()
        serviceScope.cancel()
    }

    private fun startMcpServer() {
        serviceScope.launch {
            try {
                val port = intent?.getIntExtra("port", 12345) ?: 12345
                EmbeddedMcpServer.start(port = port)

                // Notify other components that server is ready
                sendBroadcast(Intent("MCP_SERVER_STARTED").apply {
                    putExtra("server_url", EmbeddedMcpServer.getServerUrl())
                })

            } catch (e: Exception) {
                sendBroadcast(Intent("MCP_SERVER_ERROR").apply {
                    putExtra("error", e.message)
                })
            }
        }
    }

    private fun stopMcpServer() {
        try {
            EmbeddedMcpServer.stop()
            sendBroadcast(Intent("MCP_SERVER_STOPPED"))
        } catch (e: Exception) {
            // Log error but don't crash
        }
    }

    // Public API for service clients
    fun getServerUrl(): String? = EmbeddedMcpServer.getServerUrl()
    fun isServerRunning(): Boolean = EmbeddedMcpServer.isRunning()
    fun restartServer(newPort: Int) {
        serviceScope.launch {
            EmbeddedMcpServer.restart(newPort)
        }
    }
}
```

## AI Agent Integration Examples

### With OkHttp Client
```kotlin
package com.example.myapp.agent

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

class McpHttpClient(private val serverUrl: String) {
    private val httpClient = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun discoverTools(): List<Tool> {
        val request = Request.Builder()
            .url("$serverUrl/list_tools")
            .get()
            .build()

        val response = httpClient.newCall(request).execute()
        val responseBody = response.body?.string() ?: throw Exception("Empty response")

        val listToolsResponse = json.decodeFromString<ListToolsResponse>(responseBody)
        return listToolsResponse.tools
    }

    suspend fun callTool(toolName: String, arguments: Map<String, Any>): CallToolResponse {
        val request = CallToolRequest(name = toolName, arguments = arguments)
        val requestJson = json.encodeToString(request)

        val httpRequest = Request.Builder()
            .url("$serverUrl/call_tool")
            .post(requestJson.toRequestBody("application/json".toMediaType()))
            .build()

        val response = httpClient.newCall(httpRequest).execute()
        val responseBody = response.body?.string() ?: throw Exception("Empty response")

        return json.decodeFromString<CallToolResponse>(responseBody)
    }
}

// Usage
class MyAiAgent {
    private lateinit var mcpClient: McpHttpClient

    fun initialize(mcpServerUrl: String) {
        mcpClient = McpHttpClient(mcpServerUrl)

        // Discover available tools
        val tools = mcpClient.discoverTools()
        tools.forEach { tool ->
            registerTool(tool)
        }
    }

    suspend fun automateLogin(username: String, password: String) {
        // Input username
        mcpClient.callTool("inputText", mapOf(
            "text" to username,
            "fieldHint" to "Username"
        ))

        // Input password
        mcpClient.callTool("inputText", mapOf(
            "text" to password,
            "fieldHint" to "Password"
        ))

        // Tap login button
        mcpClient.callTool("tapButton", mapOf(
            "text" to "Login"
        ))
    }
}
```

### With Retrofit
```kotlin
package com.example.myapp.agent

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

interface McpApiService {
    @GET("list_tools")
    suspend fun listTools(): ListToolsResponse

    @POST("call_tool")
    suspend fun callTool(@Body request: CallToolRequest): CallToolResponse

    @GET("health")
    suspend fun healthCheck(): Map<String, Any>
}

class McpRetrofitClient(serverUrl: String) {
    private val retrofit = Retrofit.Builder()
        .baseUrl(serverUrl)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val apiService = retrofit.create(McpApiService::class.java)

    suspend fun listTools() = apiService.listTools()
    suspend fun callTool(name: String, args: Map<String, Any>) =
        apiService.callTool(CallToolRequest(name, args))
}
```

## Testing Examples

### Unit Tests
```kotlin
package com.example.myapp

import com.mcpserver.android.EmbeddedMcpServer
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Assert.*

class McpServerTest {

    @Test
    fun testServerStartStop() = runTest {
        // Test basic start/stop functionality
        assertFalse(EmbeddedMcpServer.isRunning())

        EmbeddedMcpServer.start(port = 12999)
        assertTrue(EmbeddedMcpServer.isRunning())
        assertEquals("http://127.0.0.1:12999", EmbeddedMcpServer.getServerUrl())

        EmbeddedMcpServer.stop()
        assertFalse(EmbeddedMcpServer.isRunning())
        assertNull(EmbeddedMcpServer.getServerUrl())
    }

    @Test
    fun testToolDiscovery() {
        val tools = EmbeddedMcpServer.getAvailableTools()
        assertTrue(tools.isNotEmpty())

        val toolNames = tools.map { it.name }
        assertTrue(toolNames.contains("tapButton"))
        assertTrue(toolNames.contains("inputText"))
        assertTrue(toolNames.contains("scroll"))
        assertTrue(toolNames.contains("getScreenInfo"))
    }
}
```

### Integration Tests
```kotlin
package com.example.myapp

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mcpserver.android.EmbeddedMcpServer
import kotlinx.coroutines.test.runTest
import okhttp3.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class McpIntegrationTest {

    @Test
    fun testHttpEndpoints() = runTest {
        EmbeddedMcpServer.start(port = 13000)

        val client = OkHttpClient()
        val baseUrl = "http://127.0.0.1:13000"

        // Test list_tools endpoint
        val listToolsRequest = Request.Builder()
            .url("$baseUrl/list_tools")
            .get()
            .build()

        val listToolsResponse = client.newCall(listToolsRequest).execute()
        assertEquals(200, listToolsResponse.code)
        assertTrue(listToolsResponse.body?.string()?.contains("tapButton") == true)

        // Test call_tool endpoint
        val callToolJson = """{"name": "getScreenInfo", "arguments": {}}"""
        val callToolRequest = Request.Builder()
            .url("$baseUrl/call_tool")
            .post(callToolJson.toRequestBody("application/json".toMediaType()))
            .build()

        val callToolResponse = client.newCall(callToolRequest).execute()
        assertEquals(200, callToolResponse.code)

        EmbeddedMcpServer.stop()
    }
}
```

## Error Handling Patterns

### Robust Error Handling
```kotlin
class RobustMcpManager {
    private var retryCount = 0
    private val maxRetries = 3

    suspend fun startWithRetry(port: Int) {
        repeat(maxRetries) { attempt ->
            try {
                EmbeddedMcpServer.start(port = port + attempt)
                retryCount = 0
                return
            } catch (e: IllegalStateException) {
                // Server already running
                throw e
            } catch (e: Exception) {
                if (attempt == maxRetries - 1) {
                    throw RuntimeException("Failed to start MCP server after $maxRetries attempts", e)
                }
                // Try next port
                continue
            }
        }
    }

    fun stopSafely() {
        try {
            EmbeddedMcpServer.stop()
        } catch (e: Exception) {
            // Log but don't crash
            android.util.Log.w("MCP", "Error stopping server", e)
        }
    }
}
```

This completes the comprehensive usage examples for the Android MCP Server library!