pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven(url = "https://jitpack.io")
    }
}

rootProject.name = "Rpc Core"
include(":rpccore")
include(":okiodriver")
include(":ktordriver")
include(":solanaclient")

