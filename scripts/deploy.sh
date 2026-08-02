#!/bin/bash
set -e
cd "$(dirname "$0")/.."

git pull origin main
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d --force-recreate --build backend
