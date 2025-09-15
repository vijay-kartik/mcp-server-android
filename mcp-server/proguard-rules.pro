# MCP Server Library ProGuard rules

# Keep MCP Server public API
-keep public class com.mcpserver.android.EmbeddedMcpServer { *; }
-keep public class com.mcpserver.android.ToolInfo { *; }

# Keep all model classes for serialization
-keep class com.mcpserver.android.model.** { *; }

# Keep serialization attributes
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

# Keep generic signatures for proper type erasure handling
-keepattributes Signature

# Preserve source file names and line numbers for debugging
-keepattributes SourceFile,LineNumberTable

# Don't warn about optional dependencies not present
-dontwarn java.lang.management.**
-dontwarn javax.management.**
-dontwarn org.slf4j.**
-dontwarn ch.qos.logback.**