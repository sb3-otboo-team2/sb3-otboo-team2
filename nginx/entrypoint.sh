#!/bin/sh
set -e

# === 환경변수 ===
DOMAIN="${DOMAIN:?DOMAIN is required}"
EMAIL="${EMAIL:-admin@$DOMAIN}"

WEBROOT="/var/www/certbot"
CERT_DIR="/etc/letsencrypt/live/$DOMAIN"
CERT_PATH="$CERT_DIR/fullchain.pem"
KEY_PATH="$CERT_DIR/privkey.pem"

# 스테이징 모드 기본값: true (운영 배포 시 CERTBOT_STAGING=false로 설정)
# 레이트 리밋 방지를 위해 기본적으로 스테이징 사용
if [ "${CERTBOT_STAGING:-true}" = "true" ]; then
  ACME_SERVER="--server https://acme-staging-v02.api.letsencrypt.org/directory"
  echo "[entrypoint] Using Let's Encrypt STAGING environment"
else
  ACME_SERVER=""
  echo "[entrypoint] Using Let's Encrypt PRODUCTION environment"
fi

mkdir -p "$WEBROOT"

# 0) Spring Boot 컨테이너들이 준비될 때까지 대기
echo "[entrypoint] 🔍 Waiting for Spring Boot containers to be ready..."

# 최대 5분 대기 (30초 간격으로 10번 체크)
APP_PORTS=""
for i in $(seq 1 10); do
  echo "[entrypoint] Attempt $i/10: Scanning for Spring Boot on ports 32768-32800..."
  
  # 동적 포트 범위에서 Spring Boot 찾기 (포트 스캔 방식)
  FOUND_PORTS=""
  for port in $(seq 32768 32800); do
    if curl -f -s --connect-timeout 1 "http://172.17.0.1:$port/actuator/health" > /dev/null 2>&1; then
      echo "[entrypoint] ✅ Found healthy Spring Boot at port $port"
      FOUND_PORTS="$FOUND_PORTS $port"
    fi
  done
  
  if [ -n "$FOUND_PORTS" ]; then
    APP_PORTS=$(echo $FOUND_PORTS | tr ' ' '\n' | grep -v '^$')
    echo "[entrypoint] ✅ Found $(echo $APP_PORTS | wc -w) Spring Boot instance(s): $APP_PORTS"
    break
  else
    echo "[entrypoint] ⚠️  No running Spring Boot containers found"
  fi
  
  if [ $i -lt 10 ]; then
    echo "[entrypoint] Waiting 30 seconds before next attempt..."
    sleep 30
  fi
done

# upstream 설정 생성
if [ -n "$APP_PORTS" ]; then
  # 기존 upstream 설정 백업
  cp /etc/nginx/conf.d/00-upstream.conf /etc/nginx/conf.d/00-upstream.conf.bak
  
  # 새로운 upstream 설정 생성
  cat > /etc/nginx/conf.d/00-upstream.conf <<EOF
# 동적으로 생성된 업스트림 설정
upstream backend {
    # 클라이언트 IP 기반 라우팅 (세션 유지)
    ip_hash;
EOF

  # 각 포트에 대해 server 라인 추가
  for port in $APP_PORTS; do
    echo "    server 172.17.0.1:$port max_fails=3 fail_timeout=30s;" >> /etc/nginx/conf.d/00-upstream.conf
  done
  
  cat >> /etc/nginx/conf.d/00-upstream.conf <<EOF
    
    # 헬스체크 및 로드밸런싱 설정
    keepalive 32;
    keepalive_requests 100;
    keepalive_timeout 60s;
}
EOF
  
  echo "[entrypoint] ✅ Generated upstream configuration with $(echo $APP_PORTS | wc -w) backend(s)"
else
  echo "[entrypoint] ⚠️  No Spring Boot containers found after waiting, using default configuration"
fi

# 1) 먼저 HTTP(80)으로 Nginx 띄움 - ACME HTTP-01 응답 가능하도록
nginx

# 2) 인증서 없으면 발급 시도
if [ ! -f "$CERT_PATH" ] || [ ! -f "$KEY_PATH" ]; then
  echo "[entrypoint] No existing certificate found. Attempting to issue cert for $DOMAIN ..."
  set +e
  certbot certonly --webroot -w "$WEBROOT" \
    -d "$DOMAIN" --email "$EMAIL" --agree-tos --non-interactive \
    $ACME_SERVER 2>&1 | tee /tmp/certbot.log
  rc=$?
  set -e

  if [ $rc -eq 0 ] && [ -f "$CERT_PATH" ] && [ -f "$KEY_PATH" ]; then
    echo "[entrypoint] ✅ Certificate issued successfully! Enabling HTTPS for $DOMAIN"
    # 443 설정 템플릿 적용
    envsubst '${DOMAIN}' < /etc/nginx/conf.d/ssl.conf.template > /etc/nginx/conf.d/ssl.conf
    nginx -s reload
  else
    echo "[entrypoint] ❌ Certificate issuance failed (exit code: $rc)"
    
    # 레이트 리밋 에러 체크
    if grep -q "too many.*registrations" /tmp/certbot.log; then
      echo "[entrypoint] ⚠️  Hit Let's Encrypt rate limit!"
      echo "[entrypoint] 💡 Solutions:"
      echo "  1. Wait for the retry time mentioned in the error"
      echo "  2. Use CERTBOT_STAGING=true to test (already enabled by default)"
      echo "  3. Once testing is complete, set CERTBOT_STAGING=false"
    fi
    
    echo "[entrypoint] 📝 Keeping HTTP-only service on port 80"
    echo "[entrypoint] 📋 Check /tmp/certbot.log for details"
  fi
else
  echo "[entrypoint] ✅ Existing certificate found; enabling HTTPS"
  envsubst '${DOMAIN}' < /etc/nginx/conf.d/ssl.conf.template > /etc/nginx/conf.d/ssl.conf
  nginx -s reload
fi

# 3) 갱신 크론 등록 (성공 시 nginx reload)
echo "0 3 * * * certbot renew --quiet --deploy-hook 'nginx -s reload' $ACME_SERVER" > /etc/crontabs/root

# 4) Self-signed 인증서 생성 (테스트용)
if [ ! -f "$CERT_PATH" ]; then
  echo "[entrypoint] 🔧 Creating self-signed certificate for testing..."
  openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
    -keyout "$KEY_PATH" \
    -out "$CERT_PATH" \
    -subj "/C=KR/ST=Seoul/L=Seoul/O=Test/CN=${DOMAIN}"
  
  # SSL 설정 활성화
  if [ -f /etc/nginx/conf.d/ssl.conf.template ]; then
    cp /etc/nginx/conf.d/ssl.conf.template /etc/nginx/conf.d/ssl.conf
    nginx -s reload
    echo "[entrypoint] ✅ Self-signed certificate created and SSL enabled"
  fi
fi

# 5) BusyBox crond 백그라운드 실행 (옵션 없이)
crond

echo "[entrypoint] ✅ Container ready - nginx running on HTTP:80"
[ -f "$CERT_PATH" ] && echo "[entrypoint] ✅ HTTPS:443 also available"

# 6) 컨테이너를 살아있게 유지 (nginx 프로세스 모니터링)
while :; do
  sleep 60
  # nginx가 죽었으면 컨테이너도 종료
  if ! pgrep nginx > /dev/null; then
    echo "[entrypoint] ❌ nginx died, exiting..."
    exit 1
  fi
done
