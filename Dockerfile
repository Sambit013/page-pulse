# --- Build stage: compile with Maven + full JDK ---
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# --- Run stage: lean JRE only, no build tools ---
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/page-pulse-1.0.0.jar app.jar

# Render sets PORT at runtime; application.properties already reads it.
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
