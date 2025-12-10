---
name: 調整 JWT Access Token 過期時間為一小時
overview: 將 JWT Access Token 的過期時間從 15 分鐘（900000 毫秒）調整為 1 小時（3600000 毫秒）
todos: []
---

# 調整 JWT Access Token 過期時間為一小時

## 修改內容

將 `JWT_ACCESS_TOKEN_EXPIRATION` 從 `900000` 毫秒（15 分鐘）調整為 `3600000` 毫秒（1 小時）。

## 需要修改的文件

1. **`docker-compose.yml`**（第 55 行）
   - 將 `JWT_ACCESS_TOKEN_EXPIRATION: 900000` 改為 `JWT_ACCESS_TOKEN_EXPIRATION: 3600000`

2. **`backend/src/main/resources/application.properties`**（第 51 行）
   - 將預設值從 `900000` 改為 `3600000`
   - 更新為：`jwt.access-token-expiration=${JWT_ACCESS_TOKEN_EXPIRATION:3600000}`

3. **`backend/src/main/java/com/example/helloworld/util/JwtUtil.java`**（第 22 行）
   - 將預設值和註釋從 15 分鐘改為 1 小時
   - 更新為：`@Value("${jwt.access-token-expiration:3600000}") // 預設 1 小時（3600000 毫秒）`

## 注意事項

修改後需要重新啟動後端服務才能生效：
```bash
docker compose restart backend
```