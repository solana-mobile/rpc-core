plugins {
    kotlin("multiplatform") version "1.9.0" apply false
    id("com.vanniktech.maven.publish") version "0.25.3" apply false
}

//tasks.register("clean", Delete::class) {
//    delete(rootProject.buildDir)
//}