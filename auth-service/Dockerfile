FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

RUN groupadd -r appgroup && useradd -r -g appgroup appuser

COPY auth-service/build/libs/*SNAPSHOT.jar app.jar

ENV SPRING_PROFILES_ACTIVE=staging
ENV JAVA_OPTS="-Xms512m -Xmx512m"

USER appuser

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]