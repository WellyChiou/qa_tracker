FROM maven:3.8-eclipse-temurin-17 AS build

WORKDIR /app

RUN apt-get update && \
    apt-get install -y --no-install-recommends ca-certificates ca-certificates-java && \
    update-ca-certificates -f && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

COPY pom.xml .
COPY backend/auth-core/pom.xml backend/auth-core/pom.xml
COPY backend/invest/pom.xml backend/invest/pom.xml

COPY backend/auth-core backend/auth-core
RUN mvn -f backend/auth-core/pom.xml install -DskipTests -B -U || \
    (echo "⚠️ auth-core 打包失敗，改用 SSL fallback 參數重試..." && \
    mvn -f backend/auth-core/pom.xml install -DskipTests -B -U \
      -Dmaven.wagon.http.ssl.insecure=true \
      -Dmaven.wagon.http.ssl.allowall=true \
      -Dmaven.wagon.http.ssl.ignore.validity.dates=true)

COPY backend/invest backend/invest

RUN mvn -f backend/invest/pom.xml package -DskipTests -B -U || \
    (echo "⚠️ invest 打包失敗，改用 SSL fallback 參數重試..." && \
    mvn -f backend/invest/pom.xml package -DskipTests -B -U \
      -Dmaven.wagon.http.ssl.insecure=true \
      -Dmaven.wagon.http.ssl.allowall=true \
      -Dmaven.wagon.http.ssl.ignore.validity.dates=true)

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
