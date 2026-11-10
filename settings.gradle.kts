// Composite root of the JVM builds in this repository. Each included build also
// runs on its own: `./gradlew -p shared check`.
rootProject.name = "delfos"

includeBuild("shared")
