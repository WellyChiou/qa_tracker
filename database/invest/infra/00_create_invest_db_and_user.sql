-- Infra initialization (DBA scope)
-- Purpose:
-- 1) ensure invest database exists
-- 2) ensure app user has invest database privileges
--
-- This script should be executed in infrastructure bootstrap phase.
-- Application migrations must not include CREATE DATABASE / GRANT.

CREATE DATABASE IF NOT EXISTS invest
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

GRANT ALL PRIVILEGES ON invest.* TO 'appuser'@'%';
FLUSH PRIVILEGES;
