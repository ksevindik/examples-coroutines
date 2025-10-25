import com.google.protobuf.gradle.*
        import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val springBootVersion : String by project
val protobufJavaVersion : String by project
val grpcVersion: String by project
val grpcSpringBootVersion : String by project

plugins {
    `java-library`
    kotlin("jvm") version "2.0.20"
    kotlin("plugin.spring") version "2.0.20"
    id("com.google.protobuf") version "0.9.4"
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter:$springBootVersion")
    implementation("com.google.protobuf:protobuf-java:${protobufJavaVersion}")
    implementation("com.google.protobuf:protobuf-java-util:${protobufJavaVersion}")
    implementation("org.springframework.boot:spring-boot-starter-test:$springBootVersion")
    implementation("io.grpc:grpc-api:${grpcVersion}")
    implementation("io.grpc:grpc-stub:${grpcVersion}")
    implementation("io.grpc:grpc-kotlin-stub:1.4.3")
    implementation("io.grpc:grpc-protobuf:1.63.0")

    // dependencies for the project in addition to parent project dependencies
    testImplementation("net.devh:grpc-spring-boot-starter:$grpcSpringBootVersion")

    // dependencies for the project in addition to parent project dependencies
    implementation(platform("org.jetbrains.kotlinx:kotlinx-coroutines-bom:1.6.0"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.jar {
    from(sourceSets.main.get().output)
}

protobuf {
    // Configure the Protobuf compiler (protoc)
    protoc {
        // This will download the correct protoc binary for your OS and architecture
        artifact = "com.google.protobuf:protoc:3.21.2"
    }

    // Configure the Protobuf plugins
    plugins {
        // Specify the gRPC plugin
        id("grpc") {
            // Use the correct platform-specific artifact for the gRPC code generator
            artifact = "io.grpc:protoc-gen-grpc-java:1.63.0:osx-aarch_64"
        }
        id("grpckt") {
            artifact = "io.grpc:protoc-gen-grpc-kotlin:1.4.3:jdk8@jar"
        }
    }

    // 👇 THIS IS THE MISSING BLOCK!
    // It instructs the plugin to create and configure tasks for generating code.
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                // Generates the standard Java classes from .proto files
                java {
                    // This block can be empty, but it's required for the task to be created
                }
                // Generates the standard Kotlin classes from .proto files
                kotlin {
                    // This block can be empty as well
                }
            }
            task.plugins {
                // Applies the gRPC Java plugin to generate gRPC service stubs.
                id("grpc")
                // Applies the gRPC Kotlin plugin to generate Kotlin service stubs.
                id("grpckt")
            }
        }
    }
}

