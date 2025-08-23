# Build stage
FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app
COPY pom.xml mvnw .mvn ./
COPY src src
RUN chmod +x mvnw && ./mvnw clean package -DskipTests

# Package stage
FROM eclipse-temurin:17-jdk   # ✅ changed here
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
