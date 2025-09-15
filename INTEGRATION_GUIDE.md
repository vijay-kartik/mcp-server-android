# 🚀 MCP Server Android Library - Integration Guide

## ✅ Fixed Issues
- **CORS Configuration Error**: Fixed invalid wildcard patterns
- **NoClassDefFoundError**: Resolved by exposing dependencies via `api` configuration
- **Simplified Build**: Removed complex fat .aar approach for standard Android library approach

## 📦 What You Need

### Updated .aar File
```
Location: mcp-server/build/outputs/aar/mcp-server-release.aar
Size: ~103KB (much smaller, no bundled dependencies)
```

## 🛠️ Integration Steps

### Step 1: Add the .aar File

1. **Copy the .aar file** to your app's `libs` directory:
   ```
   YourAndroidProject/
   └── app/
       └── libs/
           └── mcp-server-release.aar
   ```

### Step 2: Update Your App's build.gradle.kts

Add **ONLY these dependencies** (automatic transitive resolution):

```kotlin
dependencies {
    // MCP Server library - all dependencies automatically included
    implementation(files("libs/mcp-server-release.aar"))

    // 🎉 NO MANUAL TRANSITIVE DEPENDENCIES NEEDED!
    // The library exposes all required dependencies automatically

    // Your existing app dependencies...
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    // ... etc
}
```

### Step 3: Usage in Your App

```kotlin
import com.mcpserver.android.EmbeddedMcpServer

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        try {
            // Start MCP server - CORS issues fixed!
            EmbeddedMcpServer.start(port = 12345)

            val serverUrl = EmbeddedMcpServer.getServerUrl()
            Log.d("MCP", "✅ Server started at: $serverUrl")

            // Pass to your AI agent
            setupAiAgent(serverUrl!!)

        } catch (e: Exception) {
            Log.e("MCP", "❌ Failed to start MCP server", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        EmbeddedMcpServer.stop()
    }
}
```

## 🧪 Testing Your Integration

### Test Code Sample
```kotlin
class McpTestActivity : AppCompatActivity() {
    private val httpClient = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Start server
        EmbeddedMcpServer.start(port = 12345)

        // Test endpoints
        testMcpEndpoints()
    }

    private fun testMcpEndpoints() {
        lifecycleScope.launch {
            try {
                val serverUrl = EmbeddedMcpServer.getServerUrl()

                // Test list_tools
                val listToolsResponse = makeGetRequest("$serverUrl/list_tools")
                Log.d("MCP", "📋 Available tools: $listToolsResponse")

                // Test call_tool
                val callToolRequest = """
                    {
                        "name": "getScreenInfo",
                        "arguments": {}
                    }
                """.trimIndent()

                val callToolResponse = makePostRequest("$serverUrl/call_tool", callToolRequest)
                Log.d("MCP", "🔧 Tool response: $callToolResponse")

            } catch (e: Exception) {
                Log.e("MCP", "❌ Test failed", e)
            }
        }
    }

    private suspend fun makeGetRequest(url: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).build()
        httpClient.newCall(request).execute().use { response ->
            response.body?.string() ?: "Empty response"
        }
    }

    private suspend fun makePostRequest(url: String, jsonBody: String): String = withContext(Dispatchers.IO) {
        val requestBody = jsonBody.toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(url).post(requestBody).build()
        httpClient.newCall(request).execute().use { response ->
            response.body?.string() ?: "Empty response"
        }
    }
}
```

## 🔧 Available Tools

Your MCP server exposes these automation tools:

### 1. `tapButton`
```json
{
  "name": "tapButton",
  "arguments": {
    "text": "Login",           // Button text (optional)
    "resourceId": "btn_login", // Resource ID (optional)
    "timeout": 5000          // Timeout in ms (optional)
  }
}
```

### 2. `inputText`
```json
{
  "name": "inputText",
  "arguments": {
    "text": "Hello World",      // Text to input (required)
    "fieldId": "et_username", // Field resource ID (optional)
    "clearFirst": true        // Clear existing text (optional)
  }
}
```

### 3. `scroll`
```json
{
  "name": "scroll",
  "arguments": {
    "direction": "down",      // up, down, left, right (required)
    "distance": "medium",     // short, medium, long (optional)
    "containerId": "rv_list"  // Container ID (optional)
  }
}
```

### 4. `getScreenInfo`
```json
{
  "name": "getScreenInfo",
  "arguments": {
    "includeInvisible": false, // Include hidden elements (optional)
    "maxDepth": 10            // Max UI hierarchy depth (optional)
  }
}
```

## 🚀 What's Fixed

### ✅ CORS Configuration
- Fixed invalid wildcard patterns (`127.0.0.1:*` → proper host configuration)
- Server now starts without CORS errors

### ✅ Dependency Management
- Changed from `implementation` to `api` for key dependencies
- Automatic transitive dependency resolution
- No more `NoClassDefFoundError` for kotlinx.serialization
- Much simpler integration (just add the .aar)

### ✅ Build Process
- Removed complex fat .aar generation
- Standard Android library approach
- Smaller .aar file (~103KB vs 10MB+)
- Better ProGuard/R8 compatibility

## 🎯 Migration from Previous Version

If you were using the previous version:

1. **Remove all manual dependencies**:
   ```kotlin
   // ❌ REMOVE THESE - no longer needed!
   // implementation("io.ktor:ktor-server-core:2.3.4")
   // implementation("io.ktor:ktor-server-cio:2.3.4")
   // implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
   // etc...
   ```

2. **Replace with just the .aar**:
   ```kotlin
   // ✅ ONLY THIS IS NEEDED
   implementation(files("libs/mcp-server-release.aar"))
   ```

3. **Update your .aar file** with the new version from:
   ```
   D:\Android\mcp-server-android\mcp-server\build\outputs\aar\mcp-server-release.aar
   ```

## 🐛 Troubleshooting

### If you still get dependency errors:
1. **Clean your project**: `./gradlew clean`
2. **Invalidate caches**: Android Studio → File → Invalidate Caches and Restart
3. **Check .aar placement**: Ensure the .aar is in `app/libs/` directory
4. **Verify import**: `implementation(files("libs/mcp-server-release.aar"))`

### If CORS errors occur:
- This should be fixed in the new version
- Check Android logs for any remaining CORS issues
- Ensure you're using the latest .aar file

## ✨ Success Indicators

You'll know it's working when you see:

```
D/McpHttpServer: Starting MCP server on port 12345
I/McpHttpServer: MCP server started successfully on http://127.0.0.1:12345
D/McpHttpServer: Successfully returned 4 tools
```

No dependency-related crashes or CORS configuration errors!

---

**New .aar Location**: `D:\Android\mcp-server-android\mcp-server\build\outputs\aar\mcp-server-release.aar`

**Size**: ~103KB (lightweight, dependencies automatically managed)