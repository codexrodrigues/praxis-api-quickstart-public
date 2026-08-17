#!/bin/sh
set -eu

case "${1:-}" in
  "")
    ;;
  *)
    echo "Unsupported operational migration argument: $1" >&2
    exit 64
    ;;
esac

if [ "$#" -ne 0 ]; then
  echo "Operational migration does not accept arguments." >&2
  exit 64
fi

: "${OPERATIONAL_MIGRATION_DATASOURCE_URL:?OPERATIONAL_MIGRATION_DATASOURCE_URL is required}"
: "${OPERATIONAL_MIGRATION_DATASOURCE_USERNAME:?OPERATIONAL_MIGRATION_DATASOURCE_USERNAME is required}"
: "${OPERATIONAL_MIGRATION_DATASOURCE_PASSWORD:?OPERATIONAL_MIGRATION_DATASOURCE_PASSWORD is required}"
: "${OPERATIONAL_RUNTIME_ROLE:?OPERATIONAL_RUNTIME_ROLE is required}"

exec java \
  -Dloader.main=com.example.praxis.apiquickstart.config.OperationalDatasourceMigrator \
  -cp /app/app.jar \
  org.springframework.boot.loader.launch.PropertiesLauncher
