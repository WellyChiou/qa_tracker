FROM maven:3.8-eclipse-temurin-17 AS build

WORKDIR /app

RUN apt-get update && \
    apt-get install -y --no-install-recommends ca-certificates-java && \
    update-ca-certificates -f && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

COPY pom.xml .
COPY backend/auth-core/pom.xml backend/auth-core/pom.xml
COPY backend/invest/pom.xml backend/invest/pom.xml

RUN mvn -f backend/auth-core/pom.xml dependency:resolve -B -U || \
    (echo "⚠️ auth-core 依賴下載失敗，10 秒後重試..." && sleep 10 && mvn -f backend/auth-core/pom.xml dependency:resolve -B -U) || \
    (echo "⚠️ auth-core 第二次下載失敗，20 秒後最後重試..." && sleep 20 && mvn -f backend/auth-core/pom.xml dependency:resolve -B -U) || \
    (echo "⚠️ 依賴下載失敗，將在構建階段自動下載..." && true)

RUN mvn -f backend/invest/pom.xml dependency:resolve -B -U || \
    (echo "⚠️ invest 依賴下載失敗，10 秒後重試..." && sleep 10 && mvn -f backend/invest/pom.xml dependency:resolve -B -U) || \
    (echo "⚠️ invest 第二次下載失敗，20 秒後最後重試..." && sleep 20 && mvn -f backend/invest/pom.xml dependency:resolve -B -U) || \
    (echo "⚠️ 依賴下載失敗，將在構建階段自動下載..." && true)

COPY backend/auth-core backend/auth-core
RUN mvn -f backend/auth-core/pom.xml clean install -DskipTests -B

COPY backend/invest backend/invest

RUN mvn -f backend/invest/pom.xml clean package -DskipTests -B

FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

RUN apt-get update && \
    apt-get install -y --no-install-recommends tzdata && \
    ln -sf /usr/share/zoneinfo/Asia/Taipei /etc/localtime && \
    echo "Asia/Taipei" > /etc/timezone && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

COPY --from=build /app/backend/invest/target/*.jar app.jar

EXPOSE 8080

ENV TZ=Asia/Taipei
ENV APP_DOMAIN=invest

ENTRYPOINT ["java", "-jar", "app.jar"]
