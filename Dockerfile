# Stage 1: Build the application
FROM maven:3.9.13-eclipse-temurin-25 AS build
WORKDIR /app
COPY . .
RUN mvn -B -q package -DskipTests

# Stage 2: Create a minimal runtime image
FROM gcr.io/distroless/java25-debian13
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
