# ------------------------------------------------------------
# 실행 환경 (빌드는 GitHub Actions에서 수행)
# ------------------------------------------------------------
FROM amazoncorretto:17-alpine3.21-jdk

WORKDIR /app

# curl 설치 (헬스체크용)
RUN apk add --no-cache curl

# GitHub Actions에서 빌드된 jar 파일 복사
COPY build/libs/*.jar app.jar

# JVM 옵션 설정
ENV JVM_OPTS="-Xms256m -Xmx410m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

EXPOSE 8000

# 헬스체크
HEALTHCHECK --interval=30s --timeout=10s --start-period=120s --retries=3 \
  CMD curl -f http://localhost:8000/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JVM_OPTS -Dspring.profiles.active=prod -jar app.jar"]
