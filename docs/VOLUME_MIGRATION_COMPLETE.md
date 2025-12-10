# MySQL Volume 遷移完成確認

## ✅ 遷移狀態

是的，遷移已經完成！資料已經從 Docker named volume 遷移到主機目錄 `/root/project/work/mysql`。

## 📍 資料存儲位置

**是的，之後所有資料都會放在 `/root/project/work/mysql`**

這是因為 `docker-compose.yml` 已經配置了 volume 映射：

```yaml
volumes:
  - /root/project/work/mysql:/var/lib/mysql
```

這意味著：
- MySQL 容器內的 `/var/lib/mysql` 目錄
- 映射到主機的 `/root/project/work/mysql` 目錄
- 所有資料庫文件都會存儲在主機目錄中

## ✅ 驗證遷移完成

執行以下命令確認：

```bash
# 1. 確認 docker-compose.yml 配置正確
grep -A 1 "volumes:" docker-compose.yml | grep mysql

# 2. 確認資料目錄存在且有資料
ls -lh /root/project/work/mysql | head -10
du -sh /root/project/work/mysql

# 3. 確認 MySQL 正在使用這個目錄
docker compose exec mysql mysql -u root -prootpassword -e "SHOW DATABASES;"
docker compose exec mysql mysql -u root -prootpassword qa_tracker -e "SELECT COUNT(*) FROM users;"

# 4. 測試寫入（創建一個測試資料庫）
docker compose exec mysql mysql -u root -prootpassword -e "CREATE DATABASE IF NOT EXISTS test_migration;"
docker compose exec mysql mysql -u root -prootpassword -e "DROP DATABASE test_migration;"

# 5. 確認文件在主機上
ls -la /root/project/work/mysql/ | grep -E "(test_migration|qa_tracker)"
```

## 🧹 清理舊 Volumes（可選）

遷移完成並驗證無誤後，可以刪除舊的 volumes 釋放空間：

```bash
# 列出所有 volumes
docker volume ls | grep mysql

# 刪除不再使用的 volumes（請確認資料已成功遷移！）
docker volume rm docker-vue-java-mysql_backup_mysql_data
docker volume rm qa_tracker_mysql_data
docker volume rm work_mysql_data

# 保留 docker-vue-java-mysql_mysql_data（作為備份，可選）
```

**⚠️ 警告：** 刪除 volumes 前請確認：
1. 資料已成功遷移到 `/root/project/work/mysql`
2. MySQL 可以正常啟動和運行
3. 資料庫查詢正常
4. 已經備份了重要資料

## 📊 資料存儲結構

```
/root/project/work/
├── mysql/                    # MySQL 資料庫文件
│   ├── ibdata1              # InnoDB 資料文件
│   ├── ib_logfile0          # InnoDB 日誌文件
│   ├── qa_tracker/          # qa_tracker 資料庫
│   ├── church/              # church 資料庫（如果存在）
│   └── ...
└── uploads/                  # 上傳的文件
    └── church/
        ├── activities/
        └── sunday-messages/
```

## 🔄 之後的操作

### 備份資料

```bash
# 備份整個 mysql 目錄
tar -czf /root/project/work/mysql_backup_$(date +%Y%m%d_%H%M%S).tar.gz -C /root/project/work mysql

# 或使用 mysqldump
docker compose exec mysql mysqldump -u root -prootpassword --all-databases > /root/project/work/mysql_backup_$(date +%Y%m%d_%H%M%S).sql
```

### 重新部署

當您執行 `docker compose down` 和 `docker compose up -d` 時：
- ✅ 資料會保留在 `/root/project/work/mysql`
- ✅ 不會丟失任何資料
- ✅ MySQL 會自動使用現有資料

### 遷移到其他服務器

如果需要遷移到其他服務器：
1. 複製 `/root/project/work/mysql` 目錄
2. 在新服務器上設置相同的 volume 映射
3. 設置正確的權限（999:999）
4. 啟動 MySQL

## ✅ 遷移完成檢查清單

- [x] 資料已從 volume 遷移到主機目錄
- [x] docker-compose.yml 已配置 volume 映射
- [x] MySQL 可以正常啟動
- [x] 資料庫可以正常查詢
- [ ] 驗證資料完整性（檢查資料數量）
- [ ] 測試寫入操作（確認可以正常寫入）
- [ ] 清理舊 volumes（可選）
- [ ] 設置定期備份（建議）

## 🎉 總結

**是的，遷移已經完成！**

- ✅ 資料現在存儲在 `/root/project/work/mysql`
- ✅ 之後所有資料都會自動存儲在這裡
- ✅ Docker 重新部署不會影響資料
- ✅ 資料持久化在主機文件系統
- ✅ 容易備份和管理

