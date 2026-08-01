#!/bin/bash
set -e
cd "$(dirname "$0")/.."

docker run --rm \
  -v stock-market-service_certbot-etc:/etc/letsencrypt \
  -v stock-market-service_certbot-webroot:/var/www/certbot \
  certbot/certbot renew --webroot -w /var/www/certbot --quiet

docker compose -f docker-compose.prod.yml restart nginx
