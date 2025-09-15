# Consumer ProGuard rules for MCP Server Library
# These rules will be applied to apps that consume this library

# Keep MCP Server public API
-keep public class com.mcpserver.android.EmbeddedMcpServer { *; }
-keep public class com.mcpserver.android.ToolInfo { *; }

# Keep model classes that are serialized
-keep class com.mcpserver.android.model.** { *; }

# Keep serialization attributes for MCP models
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# Keep serializers for MCP model classes
-keep,includedescriptorclasses class com.mcpserver.android.**$$serializer { *; }
-keepclassmembers class com.mcpserver.android.** {
    *** Companion;
}
-keepclasseswithmembers class com.mcpserver.android.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Preserve attributes needed for proper operation
-keepattributes Signature

# Don't warn about optional dependencies
-dontwarn java.lang.management.**
-dontwarn javax.management.**