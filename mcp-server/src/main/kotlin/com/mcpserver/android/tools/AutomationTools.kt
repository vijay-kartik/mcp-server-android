package com.mcpserver.android.tools

import com.mcpserver.android.model.*
import kotlinx.serialization.json.*

/**
 * Sample automation tools for Android app interaction.
 * These demonstrate the MCP tool pattern but return mock responses.
 * In a real implementation, these would interact with UI Automator,
 * Accessibility Services, or other Android automation frameworks.
 */
class AutomationTools {

    /**
     * Registry of all available tools with their metadata
     */
    val availableTools: List<Tool> = listOf(
        createTapButtonTool(),
        createInputTextTool(),
        createGetScreenInfoTool(),
        createScrollTool()
    )

    /**
     * Execute a tool by name with provided arguments
     */
    suspend fun executeTool(request: CallToolRequest): CallToolResponse {
        return try {
            when (request.name) {
                "tapButton" -> executeTapButton(request.arguments)
                "inputText" -> executeInputText(request.arguments)
                "getScreenInfo" -> executeGetScreenInfo(request.arguments)
                "scroll" -> executeScroll(request.arguments)
                else -> createErrorResponse("Tool '${request.name}' not found")
            }
        } catch (e: Exception) {
            createErrorResponse("Tool execution failed: ${e.message}")
        }
    }

    /**
     * Tool: Tap a button by text or resource ID
     */
    private fun createTapButtonTool(): Tool {
        return Tool(
            name = "tapButton",
            description = "Tap a button on the screen by its text content or resource ID",
            inputSchema = ToolInputSchema(
                properties = mapOf(
                    "text" to PropertySchema(
                        type = "string",
                        description = "The text displayed on the button to tap"
                    ),
                    "resourceId" to PropertySchema(
                        type = "string",
                        description = "The Android resource ID of the button (alternative to text)"
                    ),
                    "timeout" to PropertySchema(
                        type = "number",
                        description = "Maximum time to wait for the button in milliseconds",
                        default = JsonPrimitive(5000)
                    )
                ),
                required = listOf() // Either text or resourceId should be provided
            )
        )
    }

    private suspend fun executeTapButton(arguments: Map<String, JsonElement>): CallToolResponse {
        val text = arguments["text"]?.jsonPrimitive?.content
        val resourceId = arguments["resourceId"]?.jsonPrimitive?.content
        val timeout = arguments["timeout"]?.jsonPrimitive?.long ?: 5000

        // Mock implementation - in real usage this would use UI Automator or Accessibility Service
        val target = when {
            text != null -> "button with text '$text'"
            resourceId != null -> "button with resource ID '$resourceId'"
            else -> return createErrorResponse("Either 'text' or 'resourceId' parameter is required")
        }

        // Simulate tool execution delay
        kotlinx.coroutines.delay(100)

        return CallToolResponse(
            content = listOf(
                ToolContent(
                    type = "text",
                    text = "Successfully tapped $target (timeout: ${timeout}ms)"
                )
            )
        )
    }

    /**
     * Tool: Input text into a field
     */
    private fun createInputTextTool(): Tool {
        return Tool(
            name = "inputText",
            description = "Input text into a text field or editable view",
            inputSchema = ToolInputSchema(
                properties = mapOf(
                    "text" to PropertySchema(
                        type = "string",
                        description = "The text to input"
                    ),
                    "fieldId" to PropertySchema(
                        type = "string",
                        description = "The resource ID of the target text field"
                    ),
                    "fieldHint" to PropertySchema(
                        type = "string",
                        description = "The hint text of the target field (alternative to fieldId)"
                    ),
                    "clearFirst" to PropertySchema(
                        type = "boolean",
                        description = "Whether to clear existing text before input",
                        default = JsonPrimitive(true)
                    )
                ),
                required = listOf("text")
            )
        )
    }

    private suspend fun executeInputText(arguments: Map<String, JsonElement>): CallToolResponse {
        val text = arguments["text"]?.jsonPrimitive?.content
            ?: return createErrorResponse("'text' parameter is required")

        val fieldId = arguments["fieldId"]?.jsonPrimitive?.content
        val fieldHint = arguments["fieldHint"]?.jsonPrimitive?.content
        val clearFirst = arguments["clearFirst"]?.jsonPrimitive?.boolean ?: true

        val target = when {
            fieldId != null -> "field with ID '$fieldId'"
            fieldHint != null -> "field with hint '$fieldHint'"
            else -> "focused text field"
        }

        // Mock implementation
        kotlinx.coroutines.delay(50)

        val action = if (clearFirst) "cleared and entered" else "appended"
        return CallToolResponse(
            content = listOf(
                ToolContent(
                    type = "text",
                    text = "Successfully $action text '$text' into $target"
                )
            )
        )
    }

    /**
     * Tool: Get screen information
     */
    private fun createGetScreenInfoTool(): Tool {
        return Tool(
            name = "getScreenInfo",
            description = "Get information about the current screen content and UI elements",
            inputSchema = ToolInputSchema(
                properties = mapOf(
                    "includeInvisible" to PropertySchema(
                        type = "boolean",
                        description = "Whether to include invisible UI elements",
                        default = JsonPrimitive(false)
                    ),
                    "maxDepth" to PropertySchema(
                        type = "number",
                        description = "Maximum depth of UI hierarchy to scan",
                        default = JsonPrimitive(10)
                    )
                ),
                required = listOf()
            )
        )
    }

    private suspend fun executeGetScreenInfo(arguments: Map<String, JsonElement>): CallToolResponse {
        val includeInvisible = arguments["includeInvisible"]?.jsonPrimitive?.boolean ?: false
        val maxDepth = arguments["maxDepth"]?.jsonPrimitive?.int ?: 10

        // Mock screen analysis
        kotlinx.coroutines.delay(200)

        val mockScreenInfo = buildString {
            appendLine("Screen Analysis Results:")
            appendLine("- Screen size: 1080x2340 pixels")
            appendLine("- Orientation: Portrait")
            appendLine("- Visible elements: 12 buttons, 3 text fields, 1 scroll view")
            if (includeInvisible) {
                appendLine("- Hidden elements: 2 buttons, 1 progress bar")
            }
            appendLine("- UI hierarchy depth: $maxDepth levels scanned")
            appendLine("- Current activity: com.example.MainActivity")
        }

        return CallToolResponse(
            content = listOf(
                ToolContent(
                    type = "text",
                    text = mockScreenInfo
                )
            )
        )
    }

    /**
     * Tool: Scroll in a specified direction
     */
    private fun createScrollTool(): Tool {
        return Tool(
            name = "scroll",
            description = "Scroll the screen or a specific scrollable view",
            inputSchema = ToolInputSchema(
                properties = mapOf(
                    "direction" to PropertySchema(
                        type = "string",
                        description = "Direction to scroll",
                        enum = listOf("up", "down", "left", "right")
                    ),
                    "distance" to PropertySchema(
                        type = "string",
                        description = "How far to scroll",
                        enum = listOf("short", "medium", "long"),
                        default = JsonPrimitive("medium")
                    ),
                    "containerId" to PropertySchema(
                        type = "string",
                        description = "Resource ID of specific scrollable container (optional)"
                    )
                ),
                required = listOf("direction")
            )
        )
    }

    private suspend fun executeScroll(arguments: Map<String, JsonElement>): CallToolResponse {
        val direction = arguments["direction"]?.jsonPrimitive?.content
            ?: return createErrorResponse("'direction' parameter is required")

        val distance = arguments["distance"]?.jsonPrimitive?.content ?: "medium"
        val containerId = arguments["containerId"]?.jsonPrimitive?.content

        if (direction !in listOf("up", "down", "left", "right")) {
            return createErrorResponse("Invalid direction. Must be one of: up, down, left, right")
        }

        // Mock implementation
        kotlinx.coroutines.delay(100)

        val target = containerId?.let { "container '$it'" } ?: "main screen"
        return CallToolResponse(
            content = listOf(
                ToolContent(
                    type = "text",
                    text = "Successfully scrolled $direction ($distance distance) on $target"
                )
            )
        )
    }

    /**
     * Helper function to create error responses
     */
    private fun createErrorResponse(message: String): CallToolResponse {
        return CallToolResponse(
            content = listOf(
                ToolContent(
                    type = "text",
                    text = "Error: $message"
                )
            ),
            isError = true
        )
    }
}