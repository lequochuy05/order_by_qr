# Stage 1: Build ứng dụng với Maven
FROM maven:3.9.16-eclipse-temurin-25-alpine AS builder
WORKDIR /app

# Chỉ copy file cấu hình để cache dependencies
COPY backend/pom.xml ./backend/
RUN mvn -f backend/pom.xml dependency:go-offline -B

# Copy mã nguồn và build jar
COPY backend/src ./backend/src
RUN mvn -f backend/pom.xml clean package -DskipTests -Dspotless.check.skip=true -B

# Giải nén jar thành các layer
RUN java -Djarmode=tools -jar backend/target/*.jar extract --layers --launcher --destination /app/extracted

# Stage 2: Runtime
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

# Tối ưu: chỉ cài đặt những gì cần thiết
RUN apk add --no-cache tzdata curl && \
    addgroup -S spring && adduser -S spring -G spring

USER spring

# Copy layers từ builder
COPY --chown=spring:spring --from=builder /app/extracted/dependencies/ ./
COPY --chown=spring:spring --from=builder /app/extracted/spring-boot-loader/ ./
COPY --chown=spring:spring --from=builder /app/extracted/snapshot-dependencies/ ./
COPY --chown=spring:spring --from=builder /app/extracted/application/ .

# JVM tuning mặc định an toàn cho container 512 MiB / 0.1 vCPU (Render Free tier).
# Có thể ghi đè JAVA_OPTS bằng -XX:+UseG1GC -XX:MaxRAMPercentage=65.0 trên server >= 2 GiB RAM.
ENV TZ="Asia/Ho_Chi_Minh"
ENV JAVA_OPTS="-Duser.timezone=Asia/Ho_Chi_Minh \
    -Xms64m \
    -XX:MaxRAMPercentage=45.0 \
    -XX:MaxMetaspaceSize=128m \
    -XX:ReservedCodeCacheSize=48m \
    -XX:+UseSerialGC \
    -XX:TieredStopAtLevel=1 \
    -Xss256k \
    -XX:+ExitOnOutOfMemoryError \
    -Djava.security.egd=file:/dev/./urandom"

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl -f "http://localhost:${PORT:-8080}/actuator/health/liveness" || exit 1

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
