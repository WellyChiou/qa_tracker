# 使用 Maven 官方映像作為構建階段
FROM maven:3.8-eclipse-temurin-17 AS build

WORKDIR /app

# 複製 pom.xml 和源代碼
COPY pom.xml .
COPY src ./src

# 構建應用
RUN mvn clean package -DskipTests

# 運行階段：使用 Eclipse Temurin（OpenJDK 的替代品）
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 從構建階段複製 JAR 文件
COPY --from=build /app/target/*.jar app.jar

# 暴露端口
EXPOSE 8080

# 運行應用
ENTRYPOINT ["java", "-jar", "app.jar"]


