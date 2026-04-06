FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

RUN groupadd -r appgroup && useradd -r -g appgroup appuser

COPY auth-service/build/libs/*.jar app.jar

ENV SPRING_PROFILES_ACTIVE=staging
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:MinRAMPercentage=50.0"

USER appuser

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]