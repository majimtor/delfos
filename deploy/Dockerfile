# syntax=docker/dockerfile:1
# Build context: the repository root (the API build includes the shared module and the contract).

FROM eclipse-temurin:25-jdk AS build
WORKDIR /src
COPY gradlew settings.gradle.kts ./
COPY gradle ./gradle
COPY contract/openapi.yaml ./contract/openapi.yaml
COPY shared ./shared
COPY api ./api
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew -p api bootJar --no-daemon --console=plain \
    && cp api/build/libs/api-*.jar /src/app.jar

FROM eclipse-temurin:25-jre
# Conservative settings for a 512 MB instance with a fraction of a CPU; measured in the plan.
ENV JVM_FLAGS="-XX:+UseSerialGC -XX:TieredStopAtLevel=1 -Xss512k -XX:MaxRAMPercentage=50"
WORKDIR /app
COPY --from=build /src/app.jar /app/app.jar
RUN java -Djarmode=tools -jar app.jar extract --destination application && rm app.jar
WORKDIR /app/application
# Training run for the JVM AOT cache. It refreshes the context and exits; the database is not
# reachable at build time, so Flyway and Hibernate's metadata access are disabled only here.
RUN DELFOS_DB_URL=jdbc:postgresql://localhost:1/training DELFOS_DB_USERNAME=training DELFOS_DB_PASSWORD=training \
    SPRING_FLYWAY_ENABLED=false SPRING_JPA_HIBERNATE_DDL_AUTO=none \
    SPRING_JPA_DATABASE_PLATFORM=org.hibernate.dialect.PostgreSQLDialect \
    SPRING_JPA_PROPERTIES_HIBERNATE_BOOT_ALLOW_JDBC_METADATA_ACCESS=false \
    java $JVM_FLAGS -XX:AOTCacheOutput=app.aot -Dspring.context.exit=onRefresh -jar app.jar \
    && ls -la app.aot
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java $JVM_FLAGS -XX:AOTCache=app.aot -jar app.jar"]
