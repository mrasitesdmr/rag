#!/usr/bin/env bash
# Iki veritabani ve iki kullanici olusturur; capraz erisim kapali. Sadece bos volume'da calisir.
set -euo pipefail

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres \
  -v assistant_pw="$ASSISTANT_DB_PASSWORD" -v retrieval_pw="$RETRIEVAL_DB_PASSWORD" <<'SQL'
CREATE ROLE assistant LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE PASSWORD :'assistant_pw';
CREATE ROLE retrieval LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE PASSWORD :'retrieval_pw';
CREATE DATABASE assistant OWNER assistant;
CREATE DATABASE retrieval OWNER retrieval;
REVOKE CONNECT ON DATABASE assistant FROM PUBLIC;
REVOKE CONNECT ON DATABASE retrieval FROM PUBLIC;
REVOKE CONNECT ON DATABASE postgres FROM PUBLIC;
SQL

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname retrieval \
  -c 'CREATE EXTENSION IF NOT EXISTS vector;'
