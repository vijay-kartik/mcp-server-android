// Top-level build file for the MCP Server Android Library
plugins {
    id("com.android.library") version "8.1.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.0" apply false
    id("maven-publish")
}

task("clean", Delete::class) {
    delete(rootProject.buildDir)
}