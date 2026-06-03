#!/usr/bin/env sh
set -eu

# Executado automaticamente no primeiro boot. Hoje o import é feito via SQLs:
#  - 05-ensure-roles.sql (garante roles)
#  - 20-import-dump.sql   (importa /seed/neon-init.sql)
# Este script apenas evita rodar lógica duplicada.

if [ -f "/docker-entrypoint-initdb.d/20-import-dump.sql" ] || [ -f "/docker-entrypoint-initdb.d/20-neon-init.sql" ]; then
  echo "Init/seed: import será tratado por 20-*.sql; pulando."
  exit 0
fi

echo "Init/seed: nenhum 20-*.sql encontrado; sem ação."
