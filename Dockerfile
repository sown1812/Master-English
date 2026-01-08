# Use a multi-stage build to keep the image small
FROM gradle:8.5-jdk17 AS build
WORKDIR /app
ENV SKIP_ANDROID=true
COPY . .
# Build the application shading dependencies into a fat JAR (or distZip/installDist)
# Build the application shading dependencies into a fat JAR (or distZip/installDist)
# Using installDist is standard for Ktor to get the script + libs
RUN chmod +x ./gradlew
RUN ./gradlew :server:installDist --no-daemon

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
# Copy the installed application from the build stage
COPY --from=build /app/server/build/install/server /app

# Expose the default port (Render will override this env var, but good for doc)
ENV PORT=8080
EXPOSE 8080

# Run the startup script
CMD ["./bin/server"]
