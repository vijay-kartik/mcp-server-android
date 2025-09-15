# Android MCP Server Library

A self-contained Android library that provides an embedded MCP (Model Context Protocol) server for AI agent automation within Android applications.

## Overview

This library allows any Android app to spin up a local MCP server directly in its own process, exposing automation tools over HTTP endpoints that AI agents can discover and invoke. No external dependencies, no separate processes, no complex setup required.

## Features

- **Zero-setup integration**: Add dependency and start server with one line
- **MCP Protocol Compliant**: Implements official MCP tool discovery and invocation schema
- **Android-optimized**: Uses Ktor CIO server, designed for mobile performance
- **Localhost-only**: Security-focused, server only binds to 127.0.0.1
- **Built-in Tools**: Includes sample automation tools (tap, input text, scroll, screen info)
- **Simple API**: Just `start(port)` and `stop()` methods

## Quick Start

### 1. Add Dependency

Add to your app's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.mcpserver:android-mcp-server:1.0.0")
}
```

### 2. Start the Server

```kotlin
import com.mcpserver.android.EmbeddedMcpServer

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Start MCP server on port 12345
        EmbeddedMcpServer.start(port = 12345)

        // Get the server URL for your AI agent
        val serverUrl = EmbeddedMcpServer.getServerUrl()
        // Returns: "http://127.0.0.1:12345"

        // Now pass serverUrl to your AI agent/ADK integration
        setupAiAgent(serverUrl)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Stop the server when app is destroyed
        EmbeddedMcpServer.stop()
    }
}
```

### 3. Use with AI Agents

Once started, your AI agent can discover and call tools:

```bash
# Discover available tools
GET http://127.0.0.1:12345/list_tools

# Call a tool
POST http://127.0.0.1:12345/call_tool
{
  "name": "tapButton",
  "arguments": {
    "text": "Submit",
    "timeout": 5000
  }
}
```

## Available Tools

The library includes these built-in automation tools:

### `tapButton`
Tap a button by text or resource ID
```json
{
  "name": "tapButton",
  "arguments": {
    "text": "Login",          // Button text (optional)
    "resourceId": "btn_login", // Resource ID (optional)
    "timeout": 5000         // Timeout in ms (optional, default: 5000)
  }
}
```

### `inputText`
Input text into a field
```json
{
  "name": "inputText",
  "arguments": {
    "text": "username@example.com", // Text to input (required)
    "fieldId": "et_username",      // Field resource ID (optional)
    "fieldHint": "Enter username", // Field hint (optional)
    "clearFirst": true             // Clear existing text (optional, default: true)
  }
}
```

### `scroll`
Scroll the screen or a container
```json
{
  "name": "scroll",
  "arguments": {
    "direction": "down",     // up, down, left, right (required)
    "distance": "medium",    // short, medium, long (optional, default: medium)
    "containerId": "rv_list" // Container resource ID (optional)
  }
}
```

### `getScreenInfo`
Get information about current screen
```json
{
  "name": "getScreenInfo",
  "arguments": {
    "includeInvisible": false, // Include hidden elements (optional, default: false)
    "maxDepth": 10            // Max UI hierarchy depth (optional, default: 10)
  }
}
```

## API Reference

### EmbeddedMcpServer

The main entry point for the library:

```kotlin
object EmbeddedMcpServer {
    // Start server on specified port
    fun start(port: Int = 12345, timeout: Long = 5000)

    // Stop the server
    fun stop()

    // Check if server is running
    fun isRunning(): Boolean

    // Get server URL if running
    fun getServerUrl(): String?

    // Get current port
    fun getCurrentPort(): Int?

    // Get available tools info
    fun getAvailableTools(): List<ToolInfo>

    // Restart on different port
    fun restart(newPort: Int, timeout: Long = 5000)
}
```

## Error Handling

The library provides proper error handling:

```kotlin
try {
    EmbeddedMcpServer.start(port = 8080)
} catch (e: IllegalStateException) {
    // Server already running
} catch (e: IllegalArgumentException) {
    // Invalid port number
} catch (e: RuntimeException) {
    // Server failed to start
}
```

## MCP Protocol Compliance

This library implements the official MCP specification:

- **Tool Discovery**: `GET /list_tools` returns JSON with tool schemas
- **Tool Invocation**: `POST /call_tool` accepts tool name and arguments
- **Error Handling**: Proper MCP error codes and messages
- **JSON Schema**: Full parameter validation and documentation

## Integration Examples

### With ADK (Android Development Kit)
```kotlin
// Start MCP server
EmbeddedMcpServer.start(port = 12345)

// Configure ADK with MCP endpoint
val mcpToolset = MCPToolset(serverUrl = "http://127.0.0.1:12345")
val agent = AdkAgent.builder()
    .addToolset(mcpToolset)
    .build()
```

### With Custom AI Agent
```kotlin
class MyAiAgent {
    private val httpClient = OkHttpClient()

    fun setupWithMcpServer(serverUrl: String) {
        // Discover tools
        val tools = discoverTools(serverUrl)

        // Use tools in agent workflow
        tools.forEach { tool ->
            registerTool(tool.name, tool.description, tool.inputSchema)
        }
    }
}
```

## Building and Publishing

### Build the Library
```bash
./gradlew :mcp-server:assembleRelease
```

### Run Tests
```bash
./gradlew :mcp-server:test
```

### Publish to Maven
```bash
./gradlew :mcp-server:publishToMavenLocal
```

## Requirements

- **Android API Level**: 24+ (Android 7.0)
- **Kotlin**: 1.9.0+
- **Java**: 17+
- **Permissions**: `INTERNET` (automatically added)

## Security Considerations

- Server only binds to `127.0.0.1` (localhost)
- No external network access
- Runs in app's own process and security context
- No additional permissions required beyond `INTERNET`

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## License

MIT License - see LICENSE file for details.

## Support

- **Issues**: Report bugs and feature requests on GitHub
- **Documentation**: Full API docs at [link-to-docs]
- **MCP Specification**: https://spec.modelcontextprotocol.io/