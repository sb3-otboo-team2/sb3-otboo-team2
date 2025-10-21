# ------------------------------------------------------------
# 실행 환경 (빌드는 GitHub Actions에서 수행)
# ------------------------------------------------------------
FROM amazoncorretto:17-alpine3.21-jdk

WORKDIR /app

# curl (헬스체크용)
RUN apk add --no-cache curl

# GitHub Actions에서 빌드된 jar 복사
COPY build/libs/*.jar app.jar

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -XX:InitialRAMPercentage=50 -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

EXPOSE 8000

# 헬스체크
HEALTHCHECK --interval=30s --timeout=10s --start-period=120s --retries=3 \
  CMD curl -f http://localhost:8000/actuator/health || exit 1

# 실행
# (프로필을 하드코딩하지 않고 ECS env로 넘길 거면 -Dspring.profiles.active=prod 제거)
ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "app.jar"]
