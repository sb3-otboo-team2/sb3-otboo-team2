FROM --platform=linux/amd64 nginx:1.25-alpine

# 필요한 패키지 설치 (openssl, bind-tools 추가됨)
RUN apk add --no-cache certbot curl bash cronie openssl bind-tools \
 && mkdir -p /var/www/certbot

# 설정 및 엔트리포인트 복사
COPY default.conf /etc/nginx/conf.d/default.conf
COPY ssl.conf.template /etc/nginx/conf.d/ssl.conf.template
COPY entrypoint.sh /entrypoint.sh
COPY 00-upstream.conf /etc/nginx/conf.d/00-upstream.conf
RUN chmod +x /entrypoint.sh

EXPOSE 80 443

# 헬스체크: / 또는 actuator 사용
HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
  CMD curl -sf http://localhost/actuator/health || curl -sf http://localhost || exit 1

ENV DOMAIN=3.38.166.191.sslip.io
ENV EMAIL=hokang1870@gmail.com

# 실행 진입점
CMD ["/entrypoint.sh"]
