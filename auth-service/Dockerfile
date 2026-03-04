# 1. Build Stage
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

RUN chmod +x gradlew

# 의존성 캐싱
RUN ./gradlew dependencies --no-daemon

# 소스 복사
COPY auth-service auth-service
COPY common common

RUN ./gradlew :auth-service:bootJar

# 2. Run Stage
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

RUN groupadd -r appgroup && useradd -r -g appgroup appuser

# jar 복사
COPY --from=build --chown=appuser:appgroup /app/auth-service/build/libs/*.jar app.jar

ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS="-Xms512m -Xmx512m"

USER appuser

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]