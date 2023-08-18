plugins {
    kotlin("multiplatform")
    id("com.vanniktech.maven.publish")
}

group = "io.github.funkatronics"
version = "0.1.0"

kotlin {
    jvm {
        jvmToolchain(11)
        withJava()
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(mapOf("path" to ":rpccore")))
//                implementation("com.squareup.okhttp3:okhttp:4.10.0")
                implementation("com.squareup.okhttp3:okhttp:5.0.0-alpha.11")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.2")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0-RC")
            }
        }
    }
}

mavenPublishing {
    coordinates("io.github.funkatronics", "rpccore-okiodriver", "0.1.0-SNAPSHOT")
}
