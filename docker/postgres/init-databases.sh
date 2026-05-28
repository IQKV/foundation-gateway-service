#!/bin/bash
set -e

# Create Billing user and database
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE USER svc_billing_dba WITH PASSWORD 'svc_billing_dba';
    CREATE DATABASE billingservice;
    GRANT ALL PRIVILEGES ON DATABASE billingservice TO svc_billing_dba;
EOSQL

# Run IAM Service initialization
echo "Running initialization for 'iamservice' database..."
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "iamservice" -f /docker-entrypoint-initdb.d/init-iam.sql

# Run Billing Service initialization
echo "Running initialization for 'billingservice' database..."
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "billingservice" -f /docker-entrypoint-initdb.d/init-billing.sql
