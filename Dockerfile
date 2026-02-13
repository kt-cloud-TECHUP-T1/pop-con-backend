# 1. 빌드 단계 (Build Stage)
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app

# Gradle 래퍼 및 설정 파일 복사 (캐싱을 위해 먼저 복사)
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# 라이브러리 의존성 미리 다운로드 (소스 코드 변경 시에도 캐시 활용)
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon

# 소스 코드 복사 및 실행 가능한 jar 파일 빌드
COPY src src
RUN ./gradlew bootJar -x test --no-daemon && rm -f build/libs/*-plain.jar

# 2. 실행 단계 (Run Stage)
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# 보안: root가 아닌 별도 사용자로 실행 (보안 모범 사례)
RUN groupadd -r appgroup && useradd -r -g appgroup appuser

# 빌드 단계에서 생성된 jar 파일만 복사 및 소유권 변경
COPY --from=build --chown=appuser:appgroup /app/build/libs/*.jar app.jar

# 환경 변수 기본값 설정
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS="-Xms512m -Xmx512m"

# 권한 변경 후 일반 사용자 계정으로 전환
USER appuser

EXPOSE 8080

# JVM 옵션을 포함하여 실행 (sh -c 사용으로 환경변수 치환 가능하게 함)
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
