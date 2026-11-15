-- hlm-backend 4.x stores its tables in the "healenium" schema.
-- The schema must exist before the backend's Liquibase migration runs.
CREATE SCHEMA IF NOT EXISTS healenium;
GRANT ALL ON SCHEMA healenium TO healenium_user;
