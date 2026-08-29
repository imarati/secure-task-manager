# Stage 1: сборка jar
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Копируем pom и исходники из контекста (корень secureTaskManager)
COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

# Stage 2: runtime-образ
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/target/secureTaskManager-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]