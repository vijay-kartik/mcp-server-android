package com.mcpserver.android.model

import android.annotation.SuppressLint
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * MCP (Model Context Protocol) data models following the official JSON-RPC 2.0 specification.
 * These classes represent the JSON-RPC schema for tool discovery and invocation.
 */

// JSON-RPC 2.0 Base Models

/**
 * JSON-RPC 2.0 request base
 */
@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class JsonRpcRequest(
    val jsonrpc: String = "2.0",
    val method: String,
    val params: JsonElement? = null,
    val id: JsonElement? = null
)

/**
 * JSON-RPC 2.0 response base
 */
@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class JsonRpcResponse(
    val jsonrpc: String = "2.0",
    val result: JsonElement? = null,
    val error: JsonRpcError? = null,
    val id: JsonElement? = null
)

/**
 * JSON-RPC 2.0 error object
 */
@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class JsonRpcError(
    val code: Int,
    val message: String,
    val data: JsonElement? = null
)

// MCP Capability Models

/**
 * MCP server capabilities
 */
@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class ServerCapabilities(
    val tools: ToolsCapability? = null
)

/**
 * Tools capability configuration
 */
@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class ToolsCapability(
    val listChanged: Boolean = false
)

/**
 * MCP initialization result
 */
@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class InitializeResult(
    val protocolVersion: String = "2024-11-05",
    val capabilities: ServerCapabilities,
    val serverInfo: ServerInfo,
    val instructions: String? = null
)

/**
 * Server information
 */
@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class ServerInfo(
    val name: String,
    val title: String,
    val version: String
)

// MCP Tools Models

/**
 * Parameters for tools/list method
 */
@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class ListToolsParams(
    val cursor: String? = null
)

/**
 * Response for tools/list method
 */
@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class ListToolsResult(
    val tools: List<Tool>,
    val nextCursor: String? = null
)

/**
 * Parameters for tools/call method
 */
@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class CallToolParams(
    val name: String,
    val arguments: JsonElement? = null
)

/**
 * Response for tools/call method
 */
@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class CallToolResult(
    val content: List<ToolContent>,
    val isError: Boolean = false
)

/**
 * Represents a single tool that can be invoked
 */
@SuppressLint("UnsafeOptInUsageError")
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
@SuppressLint("UnsafeOptInUsageError")
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
@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class PropertySchema(
    val type: String,
    val description: String,
    val default: JsonElement? = null,
    val enum: List<String>? = null
)


/**
 * Content returned by tool execution
 */
@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class ToolContent(
    val type: String,
    val text: String
)

/**
 * Common JSON-RPC 2.0 error codes
 */
object JsonRpcErrorCodes {
    const val PARSE_ERROR = -32700
    const val INVALID_REQUEST = -32600
    const val METHOD_NOT_FOUND = -32601
    const val INVALID_PARAMS = -32602
    const val INTERNAL_ERROR = -32603

    // MCP-specific error codes
    const val TOOL_NOT_FOUND = -32000
    const val TOOL_EXECUTION_ERROR = -32001
}