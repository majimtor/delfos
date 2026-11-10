plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

group = "com.manuelalejandro.delfos"
version = "0.1.0"

kotlin {
    // Library module: every public declaration states its visibility and type.
    explicitApi()
    jvmToolchain(25)
    jvm {
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
            // The conformance suite lives with the contract, not with the code.
            systemProperty("delfos.conformance.dir", rootDir.resolve("../contract/conformance").absolutePath)
        }
    }
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        jvmTest.dependencies {
            implementation(libs.junit.jupiter)
            runtimeOnly(libs.junit.platform.launcher)
        }
    }
}
