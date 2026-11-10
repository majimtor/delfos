import org.springframework.boot.gradle.plugin.SpringBootPlugin

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.kotlin.jpa)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.openapi.generator)
}

group = "com.manuelalejandro.delfos"
version = "0.1.0"

kotlin {
    jvmToolchain(25)
    compilerOptions {
        // Spring's nullability annotations become Kotlin nullability.
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

dependencies {
    implementation(platform(SpringBootPlugin.BOM_COORDINATES))
    implementation("com.manuelalejandro.delfos:shared")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("tools.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    runtimeOnly("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:testcontainers-postgresql")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

val generatedDir = layout.buildDirectory.dir("generated/openapi")

openApiGenerate {
    generatorName.set("kotlin-spring")
    inputSpec.set(rootDir.resolve("../contract/openapi.yaml").path)
    outputDir.set(generatedDir.get().asFile.path)
    apiPackage.set("com.manuelalejandro.delfos.api.generated")
    modelPackage.set("com.manuelalejandro.delfos.api.generated.model")
    packageName.set("com.manuelalejandro.delfos.api.generated")
    // Schemas that the shared module already implements; the generated code references those classes.
    schemaMappings.set(
        mapOf(
            "Question" to "com.manuelalejandro.delfos.definition.Question",
            "Option" to "com.manuelalejandro.delfos.definition.Option",
            "QuestionType" to "com.manuelalejandro.delfos.definition.QuestionType",
            "VisibilityRule" to "com.manuelalejandro.delfos.definition.VisibilityRule",
            "Condition" to "com.manuelalejandro.delfos.definition.Condition",
            "ConditionValue" to "com.manuelalejandro.delfos.definition.ConditionValue",
            "ConditionOperator" to "com.manuelalejandro.delfos.definition.ConditionOperator",
            "MatchMode" to "com.manuelalejandro.delfos.definition.MatchMode",
            "Answer" to "com.manuelalejandro.delfos.evaluation.Answer",
            "FieldError" to "com.manuelalejandro.delfos.evaluation.FieldError",
            "FieldErrorCode" to "com.manuelalejandro.delfos.evaluation.FieldErrorCode",
            "EvaluationResult" to "com.manuelalejandro.delfos.evaluation.EvaluationResult",
        ),
    )
    globalProperties.set(mapOf("apis" to "", "models" to ""))
    configOptions.set(
        mapOf(
            "useSpringBoot4" to "true",
            "interfaceOnly" to "true",
            "skipDefaultInterface" to "true",
            "useTags" to "true",
            "requestMappingMode" to "api_interface",
            "documentationProvider" to "none",
            "useSwaggerUI" to "false",
            "exceptionHandler" to "false",
            "gradleBuildFile" to "false",
            "useBeanValidation" to "true",
            "openApiNullable" to "false",
            "enumPropertyNaming" to "UPPERCASE",
            "serializableModel" to "false",
        ),
    )
}

kotlin.sourceSets.main {
    kotlin.srcDir(generatedDir.map { it.dir("src/main/kotlin") })
}

tasks.compileKotlin {
    dependsOn(tasks.openApiGenerate)
}

tasks.withType<Test> {
    useJUnitPlatform()
}
