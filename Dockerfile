# Stage 1: Build ứng dụng (Sửa sang bản Java 21)
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Stage 2: Chạy ứng dụng (Sửa sang bản Java 21)
FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY --from=builder /app/target/qr-ordering-1.0-SNAPSHOT.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]