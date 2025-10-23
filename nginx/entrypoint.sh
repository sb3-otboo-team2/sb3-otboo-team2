#!/bin/sh
set -e

DOMAIN="${DOMAIN:?DOMAIN is required}"
EMAIL="${EMAIL:-admin@$DOMAIN}"

WEBROOT="/var/www/certbot"
CERT_DIR="/etc/letsencrypt/live/$DOMAIN"
CERT_PATH="$CERT_DIR/fullchain.pem"
KEY_PATH="$CERT_DIR/privkey.pem"

# 스테이징 기본 false
if [ "${CERTBOT_STAGING:-false}" = "true" ]; then
  ACME_SERVER="--server https://acme-staging-v02.api.letsencrypt.org/directory"
  echo "[entrypoint] Using Let's Encrypt STAGING environment"
else
  ACME_SERVER=""
  echo "[entrypoint] Using Let's Encrypt PRODUCTION environment"
fi

mkdir -p "$WEBROOT"

# Spring Boot 대기 (포트 스캔)
echo "[entrypoint] 🔍 Waiting for Spring Boot containers to be ready..."
APP_PORTS=""
for i in $(seq 1 10); do
  echo "[entrypoint] Attempt $i/10: Scanning for Spring Boot on ports 32768-32800..."
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

# 동적 업스트림 생성
if [ -n "$APP_PORTS" ]; then
  cp /etc/nginx/conf.d/00-upstream.conf /etc/nginx/conf.d/00-upstream.conf.bak

  cat > /etc/nginx/conf.d/00-upstream.conf <<EOF
# 동적으로 생성된 업스트림 설정
upstream backend {
    ip_hash;
EOF

  for port in $APP_PORTS; do
    echo "    server 172.17.0.1:$port max_fails=3 fail_timeout=30s;" >> /etc/nginx/conf.d/00-upstream.conf
  done

  cat >> /etc/nginx/conf.d/00-upstream.conf <<EOF
    keepalive 32;
    keepalive_requests 100;
    keepalive_timeout 60s;
}
EOF

  echo "[entrypoint] ✅ Generated upstream configuration with $(echo $APP_PORTS | wc -w) backend(s)"
else
  echo "[entrypoint] ⚠️  No Spring Boot containers found after waiting, using default configuration"
fi

# HTTP 먼저 시작
nginx

# 인증서 디렉토리 미리 생성
mkdir -p "$CERT_DIR"

# DuckDNS 도메인 DNS 확인
echo "[entrypoint] 🔍 Checking DNS resolution for $DOMAIN..."
if nslookup "$DOMAIN" > /dev/null 2>&1; then
  echo "[entrypoint] ✅ DNS resolution successful for $DOMAIN"
else
  echo "[entrypoint] ❌ DNS resolution failed for $DOMAIN"
  echo "[entrypoint] 💡 Please check your DuckDNS configuration:"
  echo "   1. Go to https://www.duckdns.org/"
  echo "   2. Login and select your domain: ikuzo"
  echo "   3. Update the IP address to your current server IP"
  echo "   4. Wait a few minutes for DNS propagation"
fi

# 인증서 발급 시도
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
    envsubst '${DOMAIN}' < /etc/nginx/conf.d/ssl.conf.template > /etc/nginx/conf.d/ssl.conf
    nginx -s reload
  else
    echo "[entrypoint] ❌ Certificate issuance failed (exit code: $rc)"

    if grep -q "too many.*registrations" /tmp/certbot.log; then
      echo "[entrypoint] ⚠️  Hit Let's Encrypt rate limit!"
      echo "[entrypoint] 💡 Solutions:"
      echo "  1. Wait before retrying"
      echo "  2. Use CERTBOT_STAGING=true for testing"
    fi

    echo "[entrypoint] 📝 Keeping HTTP-only service on port 80"
    echo "[entrypoint] 📋 Check /tmp/certbot.log for details"
  fi
else
  echo "[entrypoint] ✅ Existing certificate found; enabling HTTPS"
  envsubst '${DOMAIN}' < /etc/nginx/conf.d/ssl.conf.template > /etc/nginx/conf.d/ssl.conf
  nginx -s reload
fi

# 크론 등록
echo "0 3 * * * certbot renew --quiet --deploy-hook 'nginx -s reload' $ACME_SERVER" > /etc/crontabs/root

# self-signed fallback
if [ ! -f "$CERT_PATH" ]; then
  echo "[entrypoint] 🔧 Creating self-signed certificate for testing..."
  
  # 인증서 디렉토리 다시 확인 및 생성
  mkdir -p "$CERT_DIR"
  
  openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
    -keyout "$KEY_PATH" \
    -out "$CERT_PATH" \
    -subj "/C=KR/ST=Seoul/L=Seoul/O=Test/CN=${DOMAIN}"

  if [ -f /etc/nginx/conf.d/ssl.conf.template ]; then
    envsubst '${DOMAIN}' < /etc/nginx/conf.d/ssl.conf.template > /etc/nginx/conf.d/ssl.conf
    nginx -s reload
    echo "[entrypoint] ✅ Self-signed certificate created and SSL enabled"
  fi
fi

# crond 백그라운드 실행
crond

echo "[entrypoint] ✅ Container ready - nginx running on HTTP:80"
[ -f "$CERT_PATH" ] && echo "[entrypoint] ✅ HTTPS:443 also available"

# nginx 생존 확인 루프
while :; do
  sleep 60
  if ! pgrep nginx > /dev/null; then
    echo "[entrypoint] ❌ nginx died, exiting..."
    exit 1
  fi
done
