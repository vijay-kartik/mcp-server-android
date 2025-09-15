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
     * Configure MCP protocol routes
     */
    private fun Routing.configureMcpRoutes() {
        route("/") {
            /**
             * GET /list_tools - Returns all available automation tools
             * This endpoint follows MCP specification for tool discovery
             */
            get("list_tools") {
                try {
                    Log.d(TAG, "Handling list_tools request")

                    val response = ListToolsResponse(
                        tools = automationTools.availableTools
                    )

                    call.respond(HttpStatusCode.OK, response)
                    Log.d(TAG, "Successfully returned ${response.tools.size} tools")

                } catch (e: Exception) {
                    Log.e(TAG, "Error handling list_tools request", e)
                    handleServerError(call, e)
                }
            }

            /**
             * POST /call_tool - Execute a specific tool with parameters
             * This endpoint follows MCP specification for tool invocation
             */
            post("call_tool") {
                try {
                    Log.d(TAG, "Handling call_tool request")

                    val request = call.receive<CallToolRequest>()
                    Log.d(TAG, "Calling tool: ${request.name} with args: ${request.arguments}")

                    val response = automationTools.executeTool(request)

                    val statusCode = if (response.isError) {
                        HttpStatusCode.BadRequest
                    } else {
                        HttpStatusCode.OK
                    }

                    call.respond(statusCode, response)
                    Log.d(TAG, "Tool execution completed: ${request.name}")

                } catch (e: Exception) {
                    Log.e(TAG, "Error handling call_tool request", e)
                    handleServerError(call, e)
                }
            }
        }
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
                        "list_tools" to "GET /list_tools - List all available automation tools",
                        "call_tool" to "POST /call_tool - Execute a tool with parameters",
                        "health" to "GET /health - Server health check"
                    ),
                    "documentation" to "https://spec.modelcontextprotocol.io/specification/"
                )
            )
        }
    }

    /**
     * Handle server errors with proper MCP error response format
     */
    private suspend fun handleServerError(call: ApplicationCall, exception: Exception) {
        val errorResponse = McpErrorResponse(
            error = McpError(
                code = McpErrorCodes.INTERNAL_ERROR,
                message = "Internal server error: ${exception.message}",
                data = null
            )
        )

        call.respond(HttpStatusCode.InternalServerError, errorResponse)
    }
}