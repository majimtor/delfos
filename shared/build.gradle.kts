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
            compileSdk {
                version = release(libs.versions.android.compileSdk.get().toInt()) {
                    minorApiLevel = libs.versions.android.compileSdkMinor.get().toInt()
                }
            }
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

// Mutation testing of the JVM target with pitest (plan, section 10): the mutants that survive the
// conformance suite show what the suite does not pin down. The threshold is a floor under the
// measured score, not a target. The tests and the generated serializers are not mutated; the null
// checks Kotlin compiles into every public function yield mutants no test can kill, and they count.
val pitest: Configuration by configurations.creating
dependencies {
    pitest(libs.pitest.command.line)
    pitest(libs.pitest.junit5.plugin)
}
val jvmMainCompilation = kotlin.jvm().compilations.getByName("main")
val jvmTestCompilation = kotlin.jvm().compilations.getByName("test")
tasks.register<JavaExec>("mutationTest") {
    group = "verification"
    description = "Runs pitest on the JVM target and fails under the recorded mutation score."
    dependsOn(jvmTestCompilation.compileAllTaskName)
    classpath = pitest
    mainClass.set("org.pitest.mutationtest.commandline.MutationCoverageReport")
    // Local values: the argument provider may not reach the project, for the configuration cache.
    val mutated = jvmMainCompilation.output.classesDirs
    val tests = jvmTestCompilation.output.classesDirs
    val runtime = jvmTestCompilation.runtimeDependencyFiles
    val sources = listOf("src/commonMain/kotlin", "src/jvmMain/kotlin").map { projectDir.resolve(it) }
    val report = layout.buildDirectory.dir("reports/pitest")
    val threads = Runtime.getRuntime().availableProcessors().toString()
    inputs.files(mutated, tests, runtime)
    outputs.dir(report)
    argumentProviders.add {
        listOf(
            "--classPath", (mutated + tests + runtime).files.joinToString(",") { it.path },
            "--targetClasses", "com.manuelalejandro.delfos.*",
            "--excludedClasses", "*Test*,*\$\$serializer",
            "--targetTests", "com.manuelalejandro.delfos.*",
            "--sourceDirs", sources.joinToString(",") { it.path },
            "--reportDir", report.get().asFile.path,
            "--outputFormats", "HTML,XML",
            "--timestampedReports", "false",
            "--threads", threads,
            "--mutationThreshold", "75",
        )
    }
}
