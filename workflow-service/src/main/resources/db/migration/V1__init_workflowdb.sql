-- Camunda autogenerates its own schema (camunda.bpm.database.schema-update=true).
-- This file ensures Postgres has any extensions Camunda likes.
CREATE EXTENSION IF NOT EXISTS pgcrypto;
