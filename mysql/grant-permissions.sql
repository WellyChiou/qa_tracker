-- 授予 appuser 對 qa_tracker 資料庫的權限

-- 授予所有權限
GRANT ALL PRIVILEGES ON qa_tracker.* TO 'appuser'@'%';

-- 重新載入權限
FLUSH PRIVILEGES;

-- 驗證權限
SHOW GRANTS FOR 'appuser'@'%';

