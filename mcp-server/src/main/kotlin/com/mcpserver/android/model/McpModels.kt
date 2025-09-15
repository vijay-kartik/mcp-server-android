package com.mcpserver.android.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * MCP (Model Context Protocol) data models following the official specification.
 * These classes represent the JSON schema for tool discovery and invocation.
 */

/**
 * Response model for /list_tools endpoint
 * Contains all available tools that can be called via the MCP server
 */
@Serializable
data class ListToolsResponse(
    val tools: List<Tool>
)

/**
 * Represents a single tool that can be invoked
 */
@Serializable
data class Tool(
    val name: String,
    val description: String,
    val inputSchema: ToolInputSchema
)

/**
 * JSON Schema definition for tool input parameters
 * Follows JSON Schema Draft 7 specification
 */
@Serializable
data class ToolInputSchema(
    val type: String = "object",
    val properties: Map<String, PropertySchema>,
    val required: List<String> = emptyList(),
    val additionalProperties: Boolean = false
)

/**
 * Schema for individual properties within tool input
 */
@Serializable
data class PropertySchema(
    val type: String,
    val description: String,
    val default: JsonElement? = null,
    val enum: List<String>? = null
)

/**
 * Request model for /call_tool endpoint
 * Contains the tool name and parameters to execute
 */
@Serializable
data class CallToolRequest(
    val name: String,
    val arguments: Map<String, JsonElement> = emptyMap()
)

/**
 * Response model for /call_tool endpoint
 * Contains the result of tool execution
 */
@Serializable
data class CallToolResponse(
    val content: List<ToolContent>,
    val isError: Boolean = false
)

/**
 * Content returned by tool execution
 */
@Serializable
data class ToolContent(
    val type: String,
    val text: String
)

/**
 * Error response model for failed requests
 */
@Serializable
data class McpErrorResponse(
    val error: McpError
)

/**
 * Error details for MCP protocol errors
 */
@Serializable
data class McpError(
    val code: Int,
    val message: String,
    val data: JsonElement? = null
)

/**
 * Common MCP error codes following JSON-RPC 2.0 specification
 */
object McpErrorCodes {
    const val PARSE_ERROR = -32700
    const val INVALID_REQUEST = -32600
    const val METHOD_NOT_FOUND = -32601
    const val INVALID_PARAMS = -32602
    const val INTERNAL_ERROR = -32603
    const val TOOL_NOT_FOUND = -32000
    const val TOOL_EXECUTION_ERROR = -32001
}