# Stage 1: build
FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./

# Download dependencies (cache layer)
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon

# Copy source sau cùng
COPY src src

RUN ./gradlew build -x test --no-daemon

# Stage 2: run
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
# Copy file jar từ stage build
COPY --from=builder /app/build/libs/*.jar app.jar
# Expose port
EXPOSE 8080
# Optimize JVM memory
ENTRYPOINT ["java", "-Xms256m", "-Xmx512m", "-jar", "app.jar"]