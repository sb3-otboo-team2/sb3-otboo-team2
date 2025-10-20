FROM nginx:1.25-alpine

RUN apk add --no-cache certbot curl bash cronie \
 && mkdir -p /var/www/certbot

# 설정과 엔트리포인트 복사
COPY default.conf /etc/nginx/conf.d/default.conf
COPY ssl.conf.template /etc/nginx/conf.d/ssl.conf.template
COPY entrypoint.sh /entrypoint.sh
COPY 00-upstream.conf /etc/nginx/conf.d/00-upstream.conf
RUN chmod +x /entrypoint.sh

EXPOSE 80 443

# 헬스체크: 처음엔 80만 열려있을 수 있으니 /로도 체크
HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
  CMD curl -sf http://localhost/actuator/health || curl -sf http://localhost || exit 1

# 기본값(미설정 시 fallback)
ENV DOMAIN=3.38.166.191.sslip.io
ENV EMAIL=hokang1870@gmail.com

CMD ["/entrypoint.sh"]
