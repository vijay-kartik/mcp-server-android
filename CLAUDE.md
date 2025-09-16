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
- **MCP Protocol**: Implements JSON-RPC 2.0 with `initialize`, `tools/list`, and `tools/call` methods
- **Tool System**: Pluggable automation tools with JSON schema validation
- **Coroutines**: All server operations are async using Kotlin coroutines

## Key Components

1. **MCP Models** (`model/McpModels.kt`):
   - JSON-RPC 2.0 compliant data classes for MCP protocol
   - Tool discovery and invocation models
   - Capability negotiation and server initialization models
   - JSON schema definitions for parameter validation

2. **HTTP Server** (`server/McpHttpServer.kt`):
   - Ktor-based server with JSON-RPC 2.0 support
   - Single POST endpoint handling all MCP methods
   - CORS enabled for localhost clients
   - Capability negotiation with `initialize` method
   - Health check and info endpoints

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

### Clean Build
```bash
./gradlew clean
```

## Dependencies

The library uses `api` configuration to expose key dependencies automatically to consuming apps:

- **Ktor**: HTTP server framework (CIO engine for Android) - version 2.3.4
  - `ktor-server-core`, `ktor-server-cio`, `ktor-server-content-negotiation`
  - `ktor-serialization-kotlinx-json`, `ktor-server-cors`
- **Kotlinx Serialization**: JSON handling for MCP protocol - version 1.6.0
- **Kotlinx Coroutines**: Async operations - version 1.7.3
- **Android Core KTX**: Android utilities - version 1.12.0 (implementation only)

All Ktor and serialization dependencies are exposed as `api` dependencies, meaning they are automatically available to consuming applications without manual declaration.

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

// AI agent can now call JSON-RPC 2.0 methods:
// POST / with method "initialize" - capability negotiation
// POST / with method "tools/list" - discover available tools
// POST / with method "tools/call" - execute tools with parameters

// Stop when done
EmbeddedMcpServer.stop()
```

## MCP JSON-RPC 2.0 Protocol

The server now implements the official MCP specification using JSON-RPC 2.0:

### Initialize Method
```json
POST /
{
  "jsonrpc": "2.0",
  "method": "initialize",
  "id": 1
}
```

Response:
```json
{
  "jsonrpc": "2.0",
  "result": {
    "protocolVersion": "2024-11-05",
    "capabilities": {
      "tools": { "listChanged": false }
    },
    "serverInfo": {
      "name": "Android MCP Server",
      "version": "1.0.0"
    }
  },
  "id": 1
}
```

### List Tools Method
```json
POST /
{
  "jsonrpc": "2.0",
  "method": "tools/list",
  "params": { "cursor": "optional-cursor" },
  "id": 2
}
```

### Call Tool Method
```json
POST /
{
  "jsonrpc": "2.0",
  "method": "tools/call",
  "params": {
    "name": "tapButton",
    "arguments": { "text": "Login" }
  },
  "id": 3
}
```

## Extension Points

- Add new tools in `AutomationTools` class
- Extend MCP models for additional protocol features
- Add authentication/authorization middleware to Ktor server
- Implement real UI automation using Android Accessibility Services
- Add pagination support for tool listing