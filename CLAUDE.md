# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is an Android library project that provides an embedded MCP (Model Context Protocol) server. The library allows Android apps to spin up a local HTTP server that exposes automation tools following the MCP specification for AI agent integration.

## Project Structure

- **mcp-server/**: Main library module containing all implementation code
  - `src/main/kotlin/com/mcpserver/android/`:
    - `EmbeddedMcpServer.kt`: Main public API (start/stop server)
    - `model/McpModels.kt`: MCP protocol data models and JSON schema
    - `server/McpHttpServer.kt`: Ktor-based HTTP server implementation
    - `tools/AutomationTools.kt`: Sample automation tools (tap, input, scroll, etc.)

## Architecture

- **Main API**: `EmbeddedMcpServer` object provides simple start(port)/stop() interface
- **HTTP Server**: Uses Ktor CIO engine, binds to localhost only for security
- **MCP Protocol**: Implements `/list_tools` (GET) and `/call_tool` (POST) endpoints
- **Tool System**: Pluggable automation tools with JSON schema validation
- **Coroutines**: All server operations are async using Kotlin coroutines

## Key Components

1. **MCP Models** (`model/McpModels.kt`):
   - Protocol-compliant data classes for tool discovery and invocation
   - JSON schema definitions for parameter validation
   - Error response models following JSON-RPC patterns

2. **HTTP Server** (`server/McpHttpServer.kt`):
   - Ktor-based server with JSON content negotiation
   - CORS enabled for localhost clients
   - Health check and info endpoints
   - Proper error handling with MCP error codes

3. **Automation Tools** (`tools/AutomationTools.kt`):
   - Sample tools: tapButton, inputText, scroll, getScreenInfo
   - Mock implementations returning structured responses
   - Extensible pattern for adding new automation capabilities

## Development Commands

### Build
```bash
./gradlew :mcp-server:assembleRelease
```

### Run Tests
```bash
./gradlew :mcp-server:test
./gradlew :mcp-server:connectedAndroidTest
```

### Lint and Checks
```bash
./gradlew :mcp-server:lint
./gradlew :mcp-server:ktlintCheck
```

### Publish to Local Maven
```bash
./gradlew :mcp-server:publishToMavenLocal
```

## Dependencies

- **Ktor**: HTTP server framework (CIO engine for Android)
- **Kotlinx Serialization**: JSON handling for MCP protocol
- **Coroutines**: Async operations
- **Android Core KTX**: Android utilities
- **Logback**: Logging framework

## Testing Strategy

- Unit tests for tool execution and server lifecycle
- Integration tests for HTTP endpoints
- Android instrumented tests for full library functionality
- Mock responses for automation tools (no actual UI interaction)

## Key Design Decisions

1. **Localhost Only**: Server binds to 127.0.0.1 for security
2. **Single Process**: No AIDL or multi-process complexity
3. **Simple API**: Just start/stop methods, no runtime tool registration
4. **Mock Tools**: Sample tools return structured responses without real automation
5. **Ktor CIO**: Lightweight HTTP server suitable for Android

## Usage Pattern

```kotlin
// Start server
EmbeddedMcpServer.start(port = 12345)

// Get URL for AI agent
val serverUrl = EmbeddedMcpServer.getServerUrl() // "http://127.0.0.1:12345"

// AI agent can now call:
// GET /list_tools - discover available tools
// POST /call_tool - execute tools with parameters

// Stop when done
EmbeddedMcpServer.stop()
```

## Extension Points

- Add new tools in `AutomationTools` class
- Extend MCP models for additional protocol features
- Add authentication/authorization middleware to Ktor server
- Implement real UI automation using Android Accessibility Services