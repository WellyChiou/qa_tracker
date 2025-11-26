-- 修改 MySQL 認證方式為 mysql_native_password
-- 解決 "Public Key Retrieval is not allowed" 問題

-- 修改 root 使用者（遠端連線）
ALTER USER 'root'@'%' IDENTIFIED WITH mysql_native_password BY 'rootpassword';

-- 修改 root 使用者（本地連線）
ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY 'rootpassword';

-- 修改 appuser 使用者
ALTER USER 'appuser'@'%' IDENTIFIED WITH mysql_native_password BY 'apppassword';

-- 重新載入權限
FLUSH PRIVILEGES;

-- 驗證修改結果
SELECT user, host, plugin FROM mysql.user WHERE user IN ('root', 'appuser');

