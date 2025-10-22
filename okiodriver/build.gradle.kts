plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.publish)
}

val artifactIdPrefix: String by project
val moduleArtifactId = "$artifactIdPrefix-okiodriver"

kotlin {
    jvmToolchain(11)
    jvm()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(mapOf("path" to ":rpccore")))
                implementation(libs.okhttp)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.kotlinx.serialization.json)
            }
        }
    }
}

mavenPublishing {
    coordinates(group as String, moduleArtifactId, version as String)
}
