#!/usr/bin/env bash
set -euo pipefail

# Importa um dump .sql para o Postgres local em Docker
# Uso:
#   bash scripts/db-import.sh /caminho/para/dump.sql
# Ou coloque o arquivo em ./db/init e suba o serviço pela primeira vez (init automático)

CONTAINER_NAME=${CONTAINER_NAME:-praxis-postgres}
DB_NAME=${DB_NAME:-praxis}
DB_USER=${DB_USER:-postgres}

if [[ ${1:-} == "" ]]; then
  echo "Erro: informe o caminho do arquivo .sql para importar." >&2
  echo "Ex.: bash scripts/db-import.sh ~/Downloads/neon-dump.sql" >&2
  exit 1
fi

DUMP_PATH=$1
if [[ ! -f "$DUMP_PATH" ]]; then
  echo "Arquivo não encontrado: $DUMP_PATH" >&2
  exit 1
fi

if ! docker ps --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
  echo "Container ${CONTAINER_NAME} não está em execução. Suba com: docker compose up -d db" >&2
  exit 1
fi

echo "Importando dump em ${CONTAINER_NAME}:${DB_NAME}..."
cat "$DUMP_PATH" | docker exec -i "$CONTAINER_NAME" psql -U "$DB_USER" -d "$DB_NAME"
echo "Import concluído com sucesso."

