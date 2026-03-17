-- ============================================================
-- Batch B1: Fix personal url_permissions path drift
-- Date: 2026-03-17
-- Scope:
--   1) /api/scheduled-jobs* -> /api/personal/scheduled-jobs*
--   2) /api/personal/admin/* -> /api/personal/*
--      (exclude catch-all wildcard: /api/personal/admin/**)
-- ============================================================

-- =========================
-- 0) PREVIEW SQL (read-only)
-- =========================

-- Confirm target schema exists
SELECT SCHEMA_NAME
FROM INFORMATION_SCHEMA.SCHEMATA
WHERE SCHEMA_NAME = 'qa_tracker';

-- Rows before update
SELECT
  id,
  url_pattern,
  http_method,
  required_role,
  required_permission,
  is_public,
  is_active,
  order_index,
  description
FROM qa_tracker.url_permissions
WHERE url_pattern LIKE '/api/scheduled-jobs%'
   OR url_pattern LIKE '/api/personal/admin/%'
ORDER BY order_index, id;

-- Before/after preview mapping (no write)
SELECT
  id,
  url_pattern AS before_pattern,
  CASE
    WHEN url_pattern LIKE '/api/scheduled-jobs%'
      THEN CONCAT('/api/personal', url_pattern)
    WHEN url_pattern LIKE '/api/personal/admin/%'
         AND url_pattern NOT LIKE '/api/personal/admin/**'
      THEN REPLACE(url_pattern, '/api/personal/admin/', '/api/personal/')
    ELSE url_pattern
  END AS after_pattern,
  http_method
FROM qa_tracker.url_permissions
WHERE url_pattern LIKE '/api/scheduled-jobs%'
   OR url_pattern LIKE '/api/personal/admin/%'
ORDER BY id;

-- Duplicate risk after transformation: (url_pattern, http_method)
WITH transformed AS (
  SELECT
    id,
    CASE
      WHEN url_pattern LIKE '/api/scheduled-jobs%'
        THEN CONCAT('/api/personal', url_pattern)
      WHEN url_pattern LIKE '/api/personal/admin/%'
           AND url_pattern NOT LIKE '/api/personal/admin/**'
        THEN REPLACE(url_pattern, '/api/personal/admin/', '/api/personal/')
      ELSE url_pattern
    END AS next_url_pattern,
    COALESCE(http_method, 'ANY') AS next_http_method
  FROM qa_tracker.url_permissions
)
SELECT
  next_url_pattern,
  next_http_method,
  COUNT(*) AS dup_count,
  GROUP_CONCAT(id ORDER BY id) AS ids
FROM transformed
GROUP BY next_url_pattern, next_http_method
HAVING COUNT(*) > 1
ORDER BY dup_count DESC, next_url_pattern;

-- Affected row counts
SELECT COUNT(*) AS all_candidates
FROM qa_tracker.url_permissions
WHERE url_pattern LIKE '/api/scheduled-jobs%'
   OR url_pattern LIKE '/api/personal/admin/%';

SELECT COUNT(*) AS safe_candidates_excluding_admin_wildcard
FROM qa_tracker.url_permissions
WHERE url_pattern LIKE '/api/scheduled-jobs%'
   OR (url_pattern LIKE '/api/personal/admin/%'
       AND url_pattern NOT LIKE '/api/personal/admin/**');


-- ====================
-- 1) MIGRATION SQL
-- ====================

START TRANSACTION;

-- Backup table (full structure)
CREATE TABLE IF NOT EXISTS qa_tracker.url_permissions_bak_b1_20260317
LIKE qa_tracker.url_permissions;

-- Backup rows to be changed
INSERT IGNORE INTO qa_tracker.url_permissions_bak_b1_20260317
SELECT *
FROM qa_tracker.url_permissions
WHERE url_pattern LIKE '/api/scheduled-jobs%'
   OR (url_pattern LIKE '/api/personal/admin/%'
       AND url_pattern NOT LIKE '/api/personal/admin/**');

-- Step 1: scheduled-jobs path prefix
UPDATE qa_tracker.url_permissions
SET url_pattern = CONCAT('/api/personal', url_pattern),
    updated_at = NOW()
WHERE url_pattern LIKE '/api/scheduled-jobs%';

-- Step 2: remove /admin segment (exclude wildcard catch-all)
UPDATE qa_tracker.url_permissions
SET url_pattern = REPLACE(url_pattern, '/api/personal/admin/', '/api/personal/'),
    updated_at = NOW()
WHERE url_pattern LIKE '/api/personal/admin/%'
  AND url_pattern NOT LIKE '/api/personal/admin/**';

COMMIT;


-- ====================
-- 2) ROLLBACK SQL
-- ====================

START TRANSACTION;

UPDATE qa_tracker.url_permissions u
JOIN qa_tracker.url_permissions_bak_b1_20260317 b
  ON b.id = u.id
SET u.url_pattern = b.url_pattern,
    u.http_method = b.http_method,
    u.required_role = b.required_role,
    u.required_permission = b.required_permission,
    u.is_public = b.is_public,
    u.order_index = b.order_index,
    u.is_active = b.is_active,
    u.description = b.description,
    u.created_at = b.created_at,
    u.updated_at = b.updated_at;

COMMIT;
