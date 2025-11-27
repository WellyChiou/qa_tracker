# HTTPS 設定持久化說明

## ✅ 好消息：HTTPS 設定一次即可！

**HTTPS 設定只需要做一次**，後續部署會自動保留。

## 🔄 自動保留機制

### 1. SSL 證書自動保留

- **證書位置**：`certbot/conf/live/wc-project.duckdns.org/`
- **保留方式**：使用 bind mount（`./certbot/conf`），不會被刪除
- **自動續期**：Certbot 容器會自動續期證書（每 12 小時檢查一次）

### 2. 部署腳本自動檢測

部署腳本（`deploy.sh`）會自動：
1. 檢查 SSL 證書是否存在
2. 如果存在，自動切換到 HTTPS 配置
3. 如果不存在，使用 HTTP 配置

### 3. 證書自動續期

Certbot 服務會自動續期證書：
- 檢查頻率：每 12 小時
- 續期時機：證書到期前 30 天
- 無需手動操作

## 📋 部署流程

### 首次部署（需要設置 HTTPS）

```bash
# 1. 執行一鍵部署
deploy-to-server.bat

# 2. 部署完成後，設置 HTTPS（僅首次）
ssh root@38.54.89.136
cd /root/project/work/docker-vue-java-mysql
./setup-https-on-server.sh
```

### 後續部署（自動保留 HTTPS）

```bash
# 只需要執行一鍵部署
deploy-to-server.bat
```

部署腳本會自動：
- ✅ 保留 SSL 證書
- ✅ 自動切換到 HTTPS 配置
- ✅ 重啟服務

## 🔍 工作原理

### 證書存儲

```
certbot/conf/
└── live/
    └── wc-project.duckdns.org/
        ├── fullchain.pem  ← SSL 證書
        └── privkey.pem    ← 私鑰
```

這些文件存儲在服務器上，不會被刪除。

### 部署腳本邏輯

```bash
# 檢查證書是否存在
if [ -f "certbot/conf/live/wc-project.duckdns.org/fullchain.pem" ]; then
    # 證書存在 → 使用 HTTPS 配置
    cp nginx/nginx-https.conf nginx/nginx.conf
else
    # 證書不存在 → 使用 HTTP 配置
    # 保持現有配置
fi
```

## ⚠️ 注意事項

### 1. 不要刪除證書目錄

**絕對不要**執行：
```bash
rm -rf certbot/conf  # ❌ 這會刪除證書！
```

### 2. 備份證書（可選）

如果需要備份證書：
```bash
# 備份證書
tar -czf certbot-backup.tar.gz certbot/conf/

# 恢復證書
tar -xzf certbot-backup.tar.gz
```

### 3. 證書過期

如果證書過期（90 天後），Certbot 會自動續期。如果自動續期失敗：
```bash
# 手動續期
docker-compose run --rm certbot renew
```

## 📝 檢查清單

### 首次部署後

- [ ] 執行 `setup-https-on-server.sh` 設置 HTTPS
- [ ] 確認證書申請成功
- [ ] 測試 HTTPS 訪問
- [ ] 在 LINE Console 設置 Webhook URL

### 後續部署

- [ ] 執行 `deploy-to-server.bat`
- [ ] 確認部署腳本自動檢測到證書
- [ ] 確認服務正常運行
- [ ] 測試 HTTPS 訪問

## 🎯 總結

| 項目 | 首次部署 | 後續部署 |
|------|----------|----------|
| 設置 HTTPS | ✅ 需要 | ❌ 不需要 |
| SSL 證書 | ✅ 申請一次 | ✅ 自動保留 |
| 證書續期 | - | ✅ 自動續期 |
| Nginx 配置 | ✅ 手動切換 | ✅ 自動切換 |
| 一鍵部署 | ✅ 可用 | ✅ 可用 |

**結論**：HTTPS 設定一次即可，後續部署會自動保留！🎉

