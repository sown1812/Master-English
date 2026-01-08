FROM gradle:8.9-jdk17 AS build
WORKDIR /workspace
COPY . .
RUN chmod +x ./gradlew
RUN ./gradlew :server:build --no-daemon

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/server/build/libs/*.jar /app/server.jar
EXPOSE 8080
CMD ["java", "-jar", "/app/server.jar"]
