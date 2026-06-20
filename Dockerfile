# Stage 1: Build ứng dụng với Maven
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /app

# Chỉ copy file cấu hình để cache dependencies
COPY backend/pom.xml ./backend/
RUN mvn -f backend/pom.xml dependency:go-offline -B

# Copy mã nguồn và build jar
COPY backend/src ./backend/src
RUN mvn -f backend/pom.xml clean package -DskipTests -B

# Giải nén jar thành các layer
RUN java -Djarmode=layertools -jar backend/target/*.jar extract

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Tối ưu: chỉ cài đặt những gì cần thiết
RUN apk add --no-cache tzdata curl && \
    addgroup -S spring && adduser -S spring -G spring

USER spring

# Copy layers từ builder
COPY --chown=spring:spring --from=builder /app/dependencies/ ./
COPY --chown=spring:spring --from=builder /app/spring-boot-loader/ ./
COPY --chown=spring:spring --from=builder /app/snapshot-dependencies/ ./
COPY --chown=spring:spring --from=builder /app/application/ .

# JVM tuning: MaxRAMPercentage thấp hơn để tiết kiệm RAM trên Render
ENV TZ="Asia/Ho_Chi_Minh"
ENV JAVA_OPTS="-Duser.timezone=Asia/Ho_Chi_Minh \
    -XX:MaxRAMPercentage=50.0 \
    -XX:+UseG1GC \
    -XX:+UseStringDeduplication \
    -XX:+ExitOnOutOfMemoryError \
    -XX:+AlwaysPreTouch \
    -Djava.security.egd=file:/dev/./urandom"

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
