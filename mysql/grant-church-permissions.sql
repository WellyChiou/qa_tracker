-- 授予 appuser 對 church 資料庫的權限
-- 執行方法：docker compose exec mysql mysql -uroot -prootpassword < mysql/grant-church-permissions.sql

GRANT ALL PRIVILEGES ON church.* TO 'appuser'@'%';
FLUSH PRIVILEGES;

-- 驗證權限
SHOW GRANTS FOR 'appuser'@'%';

