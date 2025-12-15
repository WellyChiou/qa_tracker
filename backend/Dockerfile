# 使用 Maven 官方映像作為構建階段
FROM maven:3.8-eclipse-temurin-17 AS build

WORKDIR /app

# 複製 pom.xml 和源代碼
COPY pom.xml .
COPY src ./src

# 先嘗試下載所有依賴（這一步會被 Docker 緩存，除非 pom.xml 改變）
# 使用 -B 批次模式減少輸出，-U 強制更新快照依賴
# 如果下載失敗，記錄警告但繼續構建（mvn clean package 會自動下載依賴）
RUN mvn dependency:resolve -B -U || \
    (echo "⚠️ 依賴下載失敗，10 秒後重試..." && sleep 10 && mvn dependency:resolve -B -U) || \
    (echo "⚠️ 第二次下載失敗，20 秒後最後重試..." && sleep 20 && mvn dependency:resolve -B -U) || \
    (echo "⚠️ 依賴下載失敗，將在構建階段自動下載..." && true)

# 構建應用（如果依賴未下載，這一步會自動下載並構建）
RUN mvn clean package -DskipTests -B

# 運行階段：使用 Eclipse Temurin（OpenJDK 的替代品）
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 設置時區為台灣時間，並安裝 mysql-client 和 gzip（用於資料庫備份）
RUN apk add --no-cache tzdata mysql-client gzip && \
    cp /usr/share/zoneinfo/Asia/Taipei /etc/localtime && \
    echo "Asia/Taipei" > /etc/timezone && \
    apk del tzdata

# 從構建階段複製 JAR 文件
COPY --from=build /app/target/*.jar app.jar

# 複製備份腳本到容器內
COPY personal-backup-database.sh /app/personal-backup-database.sh
COPY church-backup-database.sh /app/church-backup-database.sh
RUN chmod +x /app/personal-backup-database.sh /app/church-backup-database.sh

# 暴露端口
EXPOSE 8080

# 設置時區環境變數
ENV TZ=Asia/Taipei

# 運行應用
ENTRYPOINT ["java", "-jar", "app.jar"]


