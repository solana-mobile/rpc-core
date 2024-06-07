plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.publish)
}

val artifactIdPrefix: String by project
val moduleArtifactId = "$artifactIdPrefix-ktordriver"

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
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.cio)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.ktor.client.mock)
            }
        }
    }
}

mavenPublishing {
    coordinates(group as String, moduleArtifactId, version as String)
}
