plugins {
    // Kotlin is built into the Android Gradle plugin; the Compose compiler plugin is still applied.
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// The application version follows the repository version that release-please maintains.
val releaseVersion = rootDir.resolve("../version.txt").readText().trim()
val (major, minor, patch) = releaseVersion.split(".").map { it.toInt() }

android {
    namespace = "com.manuelalejandro.delfos.app"
    compileSdk {
        version = release(libs.versions.android.compileSdk.get().toInt()) {
            minorApiLevel = libs.versions.android.compileSdkMinor.get().toInt()
        }
    }
    defaultConfig {
        applicationId = "com.manuelalejandro.delfos"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.compileSdk.get().toInt()
        versionCode = major * 10_000 + minor * 100 + patch
        versionName = releaseVersion
    }
    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":app"))
    implementation(libs.androidx.activity.compose)
}
