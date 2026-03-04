FROM eclipse-temurin:21-jdk-jammy AS build

WORKDIR /app

# gradle wrapper
COPY gradlew .
COPY gradle gradle
RUN chmod +x gradlew

# gradle config
COPY build.gradle settings.gradle ./

# modules
COPY auth-service auth-service
COPY common common
COPY user-service user-service

# build
RUN ./gradlew :auth-service:clean :auth-service:bootJar --no-daemon



FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

RUN groupadd -r appgroup && useradd -r -g appgroup appuser

COPY --from=build --chown=appuser:appgroup \
  /app/auth-service/build/libs/*.jar app.jar

ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS="-Xms512m -Xmx512m"

USER appuser

EXPOSE 8080

ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]