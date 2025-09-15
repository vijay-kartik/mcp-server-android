plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
    kotlin("plugin.serialization") version "1.9.0"
}

android {
    namespace = "com.mcpserver.android"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
        targetSdk = 34

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

dependencies {
    // Ktor server dependencies - exposed as API for consuming apps
    api("io.ktor:ktor-server-core:2.3.4")
    api("io.ktor:ktor-server-cio:2.3.4")
    api("io.ktor:ktor-server-content-negotiation:2.3.4")
    api("io.ktor:ktor-serialization-kotlinx-json:2.3.4")
    api("io.ktor:ktor-server-cors:2.3.4")

    // Kotlinx serialization - exposed as API
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

    // Coroutines - exposed as API
    api("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Android core - internal implementation
    implementation("androidx.core:core-ktx:1.12.0")

    // Android logging is built-in, no external logging dependencies needed

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}

// Standard AAR with API dependencies automatically exposed to consuming apps

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "com.mcpserver"
            artifactId = "android-mcp-server"
            version = "1.0.0"

            afterEvaluate {
                from(components["release"])
            }

            pom {
                name.set("Android MCP Server")
                description.set("Embedded MCP (Model Context Protocol) server for Android applications")
                url.set("https://github.com/your-org/mcp-server-android")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }

                developers {
                    developer {
                        id.set("mcpserver")
                        name.set("MCP Server Team")
                        email.set("team@mcpserver.com")
                    }
                }
            }
        }
    }
}