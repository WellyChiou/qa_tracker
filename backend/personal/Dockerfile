# 使用 Maven 官方映像作為構建階段
FROM maven:3.8-eclipse-temurin-17 AS build

WORKDIR /app

# 更新 Java 證書庫以解決 SSL 證書驗證問題
RUN apt-get update && \
    apt-get install -y --no-install-recommends ca-certificates-java && \
    update-ca-certificates -f && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# 先複製 pom 以利用 Docker cache
COPY pom.xml .
COPY backend/auth-core/pom.xml backend/auth-core/pom.xml
COPY backend/personal/pom.xml backend/personal/pom.xml

# 再複製完整模組來源，以進行建置
COPY backend/auth-core backend/auth-core
RUN mvn -f backend/auth-core/pom.xml install -DskipTests -B -U || \
    (echo "⚠️ auth-core 打包失敗，改用 SSL fallback 參數重試..." && \
    mvn -f backend/auth-core/pom.xml install -DskipTests -B -U \
      -Dmaven.wagon.http.ssl.insecure=true \
      -Dmaven.wagon.http.ssl.allowall=true \
      -Dmaven.wagon.http.ssl.ignore.validity.dates=true)

COPY backend/personal backend/personal

# 構建 personal 模組（含共用 auth core）
RUN mvn -f backend/personal/pom.xml package -DskipTests -B -U || \
    (echo "⚠️ personal 打包失敗，改用 SSL fallback 參數重試..." && \
    mvn -f backend/personal/pom.xml package -DskipTests -B -U \
      -Dmaven.wagon.http.ssl.insecure=true \
      -Dmaven.wagon.http.ssl.allowall=true \
      -Dmaven.wagon.http.ssl.ignore.validity.dates=true)

# 運行階段：使用 Eclipse Temurin（OpenJDK 的替代品）
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# 設置時區為台灣時間，並安裝 mysql-client 和 gzip（用於資料庫備份）
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
      tzdata \
      default-mysql-client \
      gzip && \
    ln -sf /usr/share/zoneinfo/Asia/Taipei /etc/localtime && \
    echo "Asia/Taipei" > /etc/timezone && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# 從構建階段複製 JAR 文件
COPY --from=build /app/backend/personal/target/*.jar app.jar

# 複製備份腳本到容器內
COPY backend/personal/personal-backup-database.sh /app/personal-backup-database.sh
COPY backend/personal/church-backup-database.sh /app/church-backup-database.sh
RUN chmod +x /app/personal-backup-database.sh /app/church-backup-database.sh

# 暴露端口
EXPOSE 8080

# 設置時區環境變數
ENV TZ=Asia/Taipei
ENV APP_DOMAIN=personal

# 運行應用
ENTRYPOINT ["java", "-jar", "app.jar"]
