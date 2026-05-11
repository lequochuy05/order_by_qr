# Stage 1: Build ứng dụng và giải nén các lớp (layers)
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /app

# Chỉ copy file cấu hình để cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy mã nguồn và build jar
COPY src ./src
RUN mvn clean package -DskipTests -B

# Sử dụng tính năng Layering của Spring Boot để tách jar thành các lớp
# Giúp tối ưu hóa Docker Cache (khi code thay đổi, không cần tải lại các dependencies)
RUN java -Djarmode=layertools -jar target/*.jar extract

# Stage 2: Chạy ứng dụng (Runtime)
FROM eclipse-temurin:21-jre-alpine 
WORKDIR /app

# Tạo user không phải root để tăng tính bảo mật
RUN addgroup -S spring && adduser -S spring -G spring
USER spring

# Copy các lớp từ Stage 1 theo thứ tự từ ít thay đổi nhất đến nhiều thay đổi nhất
COPY --chown=spring:spring --from=builder /app/dependencies/ ./
COPY --chown=spring:spring --from=builder /app/spring-boot-loader/ ./
COPY --chown=spring:spring --from=builder /app/snapshot-dependencies/ ./
COPY --chown=spring:spring --from=builder /app/application/ .

# Cấu hình tối ưu cho JVM trong container
# -XX:MaxRAMPercentage: Tự động điều chỉnh memory theo RAM của container
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"

EXPOSE 8080

# Chạy với JarLauncher của Spring Boot để sử dụng các lớp đã giải nén
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]