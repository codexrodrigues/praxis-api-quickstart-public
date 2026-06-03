#!/bin/bash
set -euo pipefail

ENV_FILE="${ENV_FILE:-.env.dev}"
if [ -f "$ENV_FILE" ]; then
  while IFS= read -r line || [ -n "$line" ]; do
    line="${line#"${line%%[![:space:]]*}"}"
    line="${line%"${line##*[![:space:]]}"}"
    case "$line" in
      ''|\#*) continue ;;
    esac
    name="${line%%=*}"
    value="${line#*=}"
    name="${name%"${name##*[![:space:]]}"}"
    value="${value#"${value%%[![:space:]]*}"}"
    value="${value%"${value##*[![:space:]]}"}"
    if [[ "$value" == \"*\" && "$value" == *\" ]]; then
      value="${value:1:${#value}-2}"
    fi
    if [[ "$value" == \'*\' && "$value" == *\' ]]; then
      value="${value:1:${#value}-2}"
    fi
    if [[ "$name" =~ ^[A-Za-z_][A-Za-z0-9_]*$ ]]; then
      export "$name=$value"
    fi
  done < "$ENV_FILE"
fi

export SPRING_DATASOURCE_URL="${SPRING_DATASOURCE_URL:-}"
export SPRING_DATASOURCE_USERNAME="${SPRING_DATASOURCE_USERNAME:-}"
export SPRING_DATASOURCE_PASSWORD="${SPRING_DATASOURCE_PASSWORD:-}"
export CONFIG_DATASOURCE_URL="${CONFIG_DATASOURCE_URL:-$SPRING_DATASOURCE_URL}"
export CONFIG_DATASOURCE_USERNAME="${CONFIG_DATASOURCE_USERNAME:-$SPRING_DATASOURCE_USERNAME}"
export CONFIG_DATASOURCE_PASSWORD="${CONFIG_DATASOURCE_PASSWORD:-$SPRING_DATASOURCE_PASSWORD}"
export PRAXIS_AI_PROVIDER="${PRAXIS_AI_PROVIDER:-openai}"
export EMBEDDING_PROVIDER="${EMBEDDING_PROVIDER:-openai}"
export SPRING_AI_OPENAI_API_KEY="${SPRING_AI_OPENAI_API_KEY:-REPLACE_ME}"
export SPRING_AI_OPENAI_CHAT_OPTIONS_MODEL="${SPRING_AI_OPENAI_CHAT_OPTIONS_MODEL:-gpt-5-mini-2025-08-07}"
export PRAXIS_AI_GEMINI_API_KEY="${PRAXIS_AI_GEMINI_API_KEY:-REPLACE_ME}"
export SPRING_AUTOCONFIGURE_EXCLUDE="${SPRING_AUTOCONFIGURE_EXCLUDE:-org.springframework.ai.model.google.genai.autoconfigure.chat.GoogleGenAiChatAutoConfiguration,org.springframework.ai.model.google.genai.autoconfigure.embedding.GoogleGenAiEmbeddingConnectionAutoConfiguration,org.springframework.ai.model.google.genai.autoconfigure.embedding.GoogleGenAiTextEmbeddingAutoConfiguration}"
export DB_POOL_SIZE="${DB_POOL_SIZE:-5}"
export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-dev}"
export SERVER_ADDRESS="${SERVER_ADDRESS:-::}"
export PRACTICE_TEMP_PASSWORD="${PRACTICE_TEMP_PASSWORD:-REPLACE_ME}"
export APP_JWT_SECRET="${APP_JWT_SECRET:-REPLACE_ME}"
export APP_JWT_EXP_MIN="${APP_JWT_EXP_MIN:-60}"
export APP_SESSION_COOKIE_NAME="${APP_SESSION_COOKIE_NAME:-SESSION}"
export APP_SESSION_SECURE="${APP_SESSION_SECURE:-false}"
export APP_SESSION_SAMESITE="${APP_SESSION_SAMESITE:-Lax}"
export CORS_ALLOWED_ORIGINS="${CORS_ALLOWED_ORIGINS:-http://localhost:4003,http://127.0.0.1:4003,http://localhost:4200,http://127.0.0.1:4200}"
export APP_SECURITY_CONFIG_ORIGIN_RESTRICTION_ALLOWED_ORIGINS="${APP_SECURITY_CONFIG_ORIGIN_RESTRICTION_ALLOWED_ORIGINS:-http://localhost:4003,http://127.0.0.1:4003,http://localhost:4200,http://127.0.0.1:4200}"
export APP_SECURITY_READ_OPEN="${APP_SECURITY_READ_OPEN:-true}"
export APP_SECURITY_CSRF_DISABLE="${APP_SECURITY_CSRF_DISABLE:-true}"
export APP_SECURITY_WRITE_DISABLED="${APP_SECURITY_WRITE_DISABLED:-false}"
export APP_SECURITY_SCHEMAS_AGGREGATOR_ENABLED="${APP_SECURITY_SCHEMAS_AGGREGATOR_ENABLED:-true}"
export PRAXIS_AI_SECURITY_CORPORATE_MODE="${PRAXIS_AI_SECURITY_CORPORATE_MODE:-false}"
export PRAXIS_AI_SECURITY_ALLOW_HEADER_IDENTITY_IN_LOCAL="${PRAXIS_AI_SECURITY_ALLOW_HEADER_IDENTITY_IN_LOCAL:-true}"
export PRAXIS_AI_SECURITY_LOCAL_DEFAULT_TENANT="${PRAXIS_AI_SECURITY_LOCAL_DEFAULT_TENANT:-desenv}"
export PRAXIS_AI_SECURITY_LOCAL_DEFAULT_USER="${PRAXIS_AI_SECURITY_LOCAL_DEFAULT_USER:-demo}"
export PRAXIS_AI_SECURITY_LOCAL_DEFAULT_ENVIRONMENT="${PRAXIS_AI_SECURITY_LOCAL_DEFAULT_ENVIRONMENT:-local}"

require_env() {
  local name="$1"
  local value="${!name:-}"
  if [ -z "$value" ] || [ "$value" = "REPLACE_ME" ] || [[ "$value" == \<* ]]; then
    echo "Missing required env $name. Export it or create .env.dev from .env.dev.example." >&2
    exit 1
  fi
}

require_jdbc_env() {
  local name="$1"
  require_env "$name"
  local value="${!name}"
  case "$value" in
    jdbc:postgresql://*) ;;
    *)
      echo "$name must be a PostgreSQL JDBC URL starting with jdbc:postgresql://." >&2
      exit 1
      ;;
  esac
}

require_jdbc_env SPRING_DATASOURCE_URL
require_env SPRING_DATASOURCE_USERNAME
require_env SPRING_DATASOURCE_PASSWORD
require_jdbc_env CONFIG_DATASOURCE_URL
require_env CONFIG_DATASOURCE_USERNAME
require_env CONFIG_DATASOURCE_PASSWORD

APP_JAR="${APP_JAR:-$(ls -1 target/praxis-api-quickstart-*.jar 2>/dev/null | head -n 1)}"
if [ -z "$APP_JAR" ] || [ ! -f "$APP_JAR" ]; then
  echo "Jar not found under target/. Run ./mvnw -B -DskipTests package first." >&2
  exit 1
fi

export SPRING_AI_OPENAI_API_KEY
export SPRING_AI_OPENAI_CHAT_OPTIONS_MODEL
export SPRING_AI_GOOGLE_GENAI_API_KEY="${SPRING_AI_GOOGLE_GENAI_API_KEY:-$PRAXIS_AI_GEMINI_API_KEY}"
export SPRING_AI_GOOGLE_GENAI_EMBEDDING_API_KEY="${SPRING_AI_GOOGLE_GENAI_EMBEDDING_API_KEY:-$PRAXIS_AI_GEMINI_API_KEY}"

exec java -Dspring.ai.embedding.provider="$EMBEDDING_PROVIDER" -jar "$APP_JAR"
