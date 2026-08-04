# ===== Stage 1: Build =====
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy chỉ pom.xml trước để tận dụng Docker layer cache — chỉ tải lại dependency
# khi pom.xml đổi, không phải mỗi lần sửa code Java.
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# Tách layer để tối ưu cache — layer "dependencies" (~50 thư viện) hầu như không
# đổi giữa các lần deploy, chỉ "application" (code của mình) đổi thường xuyên.
RUN mkdir -p /app/extracted && \
    java -Djarmode=layertools -jar target/*.jar extract --destination /app/extracted

# ===== Stage 2: Run =====
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Không chạy bằng root trong container.
RUN addgroup -S spring && adduser -S spring -G spring
USER spring

# Copy từng layer riêng biệt - layer ít đổi (dependencies) copy trước
COPY --from=build --chown=spring:spring /app/extracted/dependencies/ ./
COPY --from=build --chown=spring:spring /app/extracted/spring-boot-loader/ ./
COPY --from=build --chown=spring:spring /app/extracted/snapshot-dependencies/ ./
COPY --from=build --chown=spring:spring /app/extracted/application/ ./

ENV JAVA_OPTS="-Xms256m -Xmx512m"

EXPOSE 8080

# Dùng endpoint Actuator đã bật (health-check container, vd Render/Docker Compose).
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1

# Không còn app.jar (đã extract layer) nên chạy trực tiếp qua JarLauncher.
# Dùng "sh -c" (không phải exec-form mảng) để JAVA_OPTS thật sự được áp dụng.
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
