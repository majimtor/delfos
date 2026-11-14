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
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // The published APK is signed with the debug key generated on the build machine: a
            // stable release key would be a secret in the repository or the CI, and the budget of
            // this project is zero secrets for distribution (PLAN, section 15; docs/known-issues.md).
            signingConfig = signingConfigs.getByName("debug")
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
