import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.kmp.library) apply false
}

group = "com.manuelalejandro.delfos"
version = "0.1.0"

// The mobile targets exist only when asked for. The API includes this build to compile against
// the JVM target and its container image has no Android SDK or Xcode, and needs none; the mobile
// builds and the CI jobs that cover them pass `-Pdelfos.android=true` and `-Pdelfos.ios=true`.
// The two are separate so that the iOS job on the macOS runner needs no Android SDK either.
fun flag(name: String) = providers.gradleProperty(name).map { it.toBoolean() }.getOrElse(false)
val android = flag("delfos.android")
val ios = flag("delfos.ios")
if (android) {
    apply(plugin = libs.plugins.android.kmp.library.get().pluginId)
}

// The conformance suite lives with the contract. It is embedded into a generated test source so
// that the same cases run on every target, including the ones without a file system at hand.
val generateConformanceCases = tasks.register("generateConformanceCases") {
    // Local values: the action may not hold a reference to the script, for the configuration cache.
    val cases = rootDir.resolve("../contract/conformance")
    val generated = layout.buildDirectory.dir("generated/conformance/commonTest/kotlin")
    inputs.dir(cases)
    outputs.dir(generated)
    doLast {
        val target = generated.get().asFile.resolve("com/manuelalejandro/delfos/evaluation/ConformanceCases.kt")
        target.parentFile.mkdirs()
        val entries = cases.listFiles { f -> f.name.endsWith(".json") }!!.sortedBy { it.name }.joinToString(",\n") { file ->
            val text = file.readText().replace("$", "\${'$'}")
            "    \"${file.name}\" to \"\"\"$text\"\"\""
        }
        target.writeText(
            "package com.manuelalejandro.delfos.evaluation\n\n" +
                "// Generated from contract/conformance by the shared build; do not edit.\n" +
                "internal val CONFORMANCE_CASES: List<Pair<String, String>> = listOf(\n$entries,\n)\n",
        )
    }
}

kotlin {
    // Library module: every public declaration states its visibility and type.
    explicitApi()
    jvmToolchain(25)
    jvm {
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }
    if (ios) {
        // Devices and the simulator on Apple silicon; the test task runs the suite on the simulator
        // and is skipped by the Kotlin plugin on an Intel host, where only the compilation is checked.
        iosArm64()
        iosSimulatorArm64()
    }
    if (android) {
        // The Android target is configured by name: its DSL is reachable only with the plugin applied.
        extensions.configure<com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget>("androidLibrary") {
            namespace = "com.manuelalejandro.delfos.shared"
            compileSdk = libs.versions.android.compileSdk.get().toInt()
            minSdk = libs.versions.android.minSdk.get().toInt()
            compilerOptions {
                jvmTarget.set(JvmTarget.JVM_17)
            }
            // Runs the common tests on the host JVM against the Android class path.
            withHostTestBuilder { }
        }
    }
    sourceSets {
        commonMain.dependencies {
            // Part of the public API: field-error parameters are JsonPrimitive values.
            api(libs.kotlinx.serialization.json)
        }
        commonTest {
            kotlin.srcDir(generateConformanceCases)
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}
