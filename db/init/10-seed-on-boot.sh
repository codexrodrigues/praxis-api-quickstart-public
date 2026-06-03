#!/usr/bin/env sh
set -eu

# Executado automaticamente no primeiro boot. Hoje o import e feito via SQLs:
#  - 05-ensure-roles.sql (garante roles)
#  - 20-import-dump.sql   (importa /seed/public-demo-seed.sql)
# Este script apenas evita rodar logica duplicada.

if [ -f "/docker-entrypoint-initdb.d/20-import-dump.sql" ]; then
  echo "Init/seed: import sera tratado por 20-*.sql; pulando."
  exit 0
fi

echo "Init/seed: nenhum 20-*.sql encontrado; sem acao."
