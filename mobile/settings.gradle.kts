// Standalone build of the mobile client: a Compose Multiplatform library that holds the whole
// application (`app`) and the thin Android application around it (`androidApp`). It includes the
// shared module as a composite build; run it with `./gradlew -p mobile ...` from the repository root.
pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    // Provisions the JDK declared by the toolchain when it is not installed.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "mobile"

include(":app", ":androidApp")

includeBuild("../shared")
