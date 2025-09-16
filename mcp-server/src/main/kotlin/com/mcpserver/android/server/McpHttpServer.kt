package com.mcpserver.android.server

import com.mcpserver.android.model.*
import com.mcpserver.android.tools.AutomationTools
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import android.util.Log
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement

/**
 * Ktor-based HTTP server that implements MCP (Model Context Protocol) endpoints.
 * Provides /list_tools and /call_tool endpoints for AI agent integration.
 *
 * The server runs on localhost only and is designed to be embedded within
 * Android applications for in-process automation tool access.
 */
class McpHttpServer(
    private val port: Int,
    private val automationTools: AutomationTools = AutomationTools()
) {
    companion object {
        private const val TAG = "McpHttpServer"
    }
    private var server: ApplicationEngine? = null
    private val serverScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * Start the MCP HTTP server on the specified port
     * @throws IllegalStateException if server is already running
     */
    fun start() {
        if (server != null) {
            throw IllegalStateException("Server is already running")
        }

        Log.i(TAG, "Starting MCP server on port $port")

        try {
            server = embeddedServer(
                factory = CIO,
                port = port,
                host = "127.0.0.1", // Localhost only for security
                module = { configureApplication() }
            ).start(wait = false)

            Log.i(TAG, "MCP server started successfully on http://127.0.0.1:$port")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start MCP server", e)
            throw e
        }
    }

    /**
     * Stop the MCP HTTP server
     */
    fun stop() {
        Log.i(TAG, "Stopping MCP server")

        server?.stop(
            gracePeriodMillis = 1000,
            timeoutMillis = 5000
        )
        server = null
        serverScope.cancel()

        Log.i(TAG, "MCP server stopped")
    }

    /**
     * Check if the server is currently running
     */
    fun isRunning(): Boolean = server != null

    /**
     * Get the server URL if running
     */
    fun getServerUrl(): String? = if (isRunning()) "http://127.0.0.1:$port" else null

    /**
     * Configure the Ktor application with routing and plugins
     */
    private fun Application.configureApplication() {
        // Install JSON content negotiation
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }

        // Install CORS for potential web-based agent clients
        install(CORS) {
            allowHost("127.0.0.1", listOf("http", "https"))
            allowHost("localhost", listOf("http", "https"))
            allowHeader(HttpHeaders.ContentType)
            allowMethod(HttpMethod.Get)
            allowMethod(HttpMethod.Post)
            allowMethod(HttpMethod.Options)
            allowCredentials = false
            maxAgeInSeconds = 3600
        }

        // Configure routing
        routing {
            configureMcpRoutes()
            configureHealthRoutes()
        }

        // Global exception handling
        install(createApplicationPlugin("McpExceptionHandler") {
            onCallRespond { call, _ ->
                call.response.header("X-MCP-Server", "Android-Embedded-v1.0")
            }
        })
    }

    /**
     * Configure MCP JSON-RPC 2.0 protocol routes
     */
    private fun Routing.configureMcpRoutes() {
        /**
         * POST / - Main JSON-RPC 2.0 endpoint for all MCP methods
         */
        post("/") {
            try {
                val jsonRpcRequest = call.receive<JsonRpcRequest>()
                Log.d(TAG, "Handling JSON-RPC method: ${jsonRpcRequest.method}")

                val response = when (jsonRpcRequest.method) {
                    "initialize" -> handleInitialize(jsonRpcRequest)
                    "tools/list" -> handleToolsList(jsonRpcRequest)
                    "tools/call" -> handleToolsCall(jsonRpcRequest)
                    else -> createErrorResponse(
                        jsonRpcRequest.id,
                        JsonRpcErrorCodes.METHOD_NOT_FOUND,
                        "Method '${jsonRpcRequest.method}' not found"
                    )
                }

                call.respond(HttpStatusCode.OK, response)
                Log.d(TAG, "JSON-RPC method completed: ${jsonRpcRequest.method}")

            } catch (e: Exception) {
                Log.e(TAG, "Error handling JSON-RPC request", e)
                val errorResponse = createErrorResponse(
                    null,
                    JsonRpcErrorCodes.INTERNAL_ERROR,
                    "Internal server error: ${e.message}"
                )
                call.respond(HttpStatusCode.InternalServerError, errorResponse)
            }
        }
    }

    /**
     * Handle initialize method for capability negotiation
     */
    private fun handleInitialize(request: JsonRpcRequest): JsonRpcResponse {
        Log.d(TAG, "Handling initialize request")

        val result = InitializeResult(
            protocolVersion = "2024-11-05",
            capabilities = ServerCapabilities(
                tools = ToolsCapability(listChanged = false)
            ),
            serverInfo = ServerInfo(
                name = "Android MCP Server",
                title = "MCP server for android related work",
                version = "1.0.0"
            ),
            instructions = "This contains any instruction that should be followed by an mcp client."
        )

        return JsonRpcResponse(
            result = Json.encodeToJsonElement(result),
            id = request.id
        )
    }

    /**
     * Handle tools/list method
     */
    private fun handleToolsList(request: JsonRpcRequest): JsonRpcResponse {
        Log.d(TAG, "Handling tools/list request")

        return try {
            // Parse optional params for cursor-based pagination
            request.params?.let {
                Json.decodeFromJsonElement<ListToolsParams>(it)
            }

            val result = ListToolsResult(
                tools = automationTools.availableTools,
                nextCursor = null // No pagination support for now
            )

            Log.d(TAG, "Successfully returned ${result.tools.size} tools")

            JsonRpcResponse(
                result = Json.encodeToJsonElement(result),
                id = request.id
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error in tools/list", e)
            createErrorResponse(request.id, JsonRpcErrorCodes.INTERNAL_ERROR, e.message ?: "Unknown error")
        }
    }

    /**
     * Handle tools/call method
     */
    private suspend fun handleToolsCall(request: JsonRpcRequest): JsonRpcResponse {
        Log.d(TAG, "Handling tools/call request")

        return try {
            val params = request.params?.let {
                Json.decodeFromJsonElement<CallToolParams>(it)
            } ?: throw IllegalArgumentException("Missing parameters for tools/call")

            Log.d(TAG, "Calling tool: ${params.name}")

            val result = automationTools.executeTool(params)

            if (result.isError) {
                createErrorResponse(
                    request.id,
                    JsonRpcErrorCodes.TOOL_EXECUTION_ERROR,
                    result.content.firstOrNull()?.text ?: "Tool execution failed"
                )
            } else {
                JsonRpcResponse(
                    result = Json.encodeToJsonElement(result),
                    id = request.id
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in tools/call", e)
            createErrorResponse(request.id, JsonRpcErrorCodes.INTERNAL_ERROR, e.message ?: "Unknown error")
        }
    }

    /**
     * Create a JSON-RPC error response
     */
    private fun createErrorResponse(id: JsonElement?, code: Int, message: String): JsonRpcResponse {
        return JsonRpcResponse(
            error = JsonRpcError(
                code = code,
                message = message
            ),
            id = id
        )
    }

    /**
     * Configure health check and server info routes
     */
    private fun Routing.configureHealthRoutes() {
        /**
         * GET /health - Server health check endpoint
         */
        get("/health") {
            call.respond(
                HttpStatusCode.OK,
                mapOf(
                    "status" to "healthy",
                    "server" to "MCP Android Server",
                    "version" to "1.0.0",
                    "port" to port,
                    "tools_count" to automationTools.availableTools.size
                )
            )
        }

        /**
         * GET / - Server information and available endpoints
         */
        get("/") {
            call.respond(
                HttpStatusCode.OK,
                mapOf(
                    "name" to "MCP Android Server",
                    "version" to "1.0.0",
                    "description" to "Embedded MCP server for Android automation",
                    "endpoints" to mapOf(
                        "jsonrpc" to "POST / - Main JSON-RPC 2.0 endpoint for all MCP methods",
                        "health" to "GET /health - Server health check"
                    ),
                    "documentation" to "https://modelcontextprotocol.io/specification/"
                )
            )
        }
    }

}