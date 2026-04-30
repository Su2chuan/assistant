# Stage 1: 构建 Vue 前端
FROM node:20-alpine AS frontend-builder

WORKDIR /app/frontend

COPY frontend/package*.json ./
RUN npm install

COPY frontend/ ./
RUN npm run build

# Stage 2: 构建 Java 后端
FROM maven:3.9-eclipse-temurin-21-alpine AS backend-builder

WORKDIR /app

# 复制 Vue 构建产物到 static 目录
COPY --from=frontend-builder /app/frontend/dist ./src/main/resources/static

# 复制 Maven 配置和源代码
COPY pom.xml ./
COPY src ./src

# 构建 jar（跳过测试以加速）
RUN mvn clean package -DskipTests

# Stage 3: 运行时镜像
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# 创建非 root 用户
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# 复制 jar 文件
COPY --from=backend-builder /app/target/assistant-1.0.0-SNAPSHOT.jar app.jar

# 切换用户
USER appuser

# 暴露端口
EXPOSE 8080

# 启动应用
ENTRYPOINT ["java", "-jar", "app.jar"]