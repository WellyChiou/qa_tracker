# 崗位人員配置初始化說明

## 概述

崗位人員配置現在已從資料庫載入，而不是寫死在代碼中。系統會在首次啟動時自動初始化默認配置。

## 自動初始化

如果使用 Docker Compose，系統會在 MySQL 容器首次啟動時自動執行初始化腳本：
- `mysql/church-schema.sql` - 創建資料庫和表結構
- `mysql/init-position-config.sql` - 插入默認崗位配置

## 手動初始化（如果需要）

如果資料庫已經存在，需要手動執行初始化腳本：

```bash
# 進入 MySQL 容器
docker compose exec mysql bash

# 執行初始化腳本
mysql -uroot -prootpassword < /docker-entrypoint-initdb.d/03-init-position-config.sql

# 或者直接執行 SQL
mysql -uroot -prootpassword church < /path/to/init-position-config.sql
```

或者使用 MySQL 客戶端連接：

```bash
docker compose exec mysql mysql -uroot -prootpassword church
```

然後執行：

```sql
USE church;

INSERT INTO position_config (config_name, config_data, is_default) 
VALUES (
    'default',
    JSON_OBJECT(
        'computer', JSON_OBJECT(
            'saturday', JSON_ARRAY('世維', '佑庭', '朝翔'),
            'sunday', JSON_ARRAY('君豪', '家偉', '佑庭')
        ),
        'sound', JSON_OBJECT(
            'saturday', JSON_ARRAY('斌浩', '世維', '佑庭', '桓瑜', '朝翔'),
            'sunday', JSON_ARRAY('朝翔', '君豪', '家偉', '佑庭')
        ),
        'light', JSON_OBJECT(
            'saturday', JSON_ARRAY('阿皮', '佑庭', '桓瑜'),
            'sunday', JSON_ARRAY('佳佳', '鈞顯', '曹格', '佑庭', '榮一')
        )
    ),
    1
) ON DUPLICATE KEY UPDATE
    config_data = VALUES(config_data),
    updated_at = CURRENT_TIMESTAMP;
```

## 功能說明

### 前端功能

1. **自動載入配置**：頁面載入時自動從資料庫載入默認配置
2. **修改配置**：可以在界面上添加/移除人員
3. **保存配置**：點擊「保存配置」按鈕將當前配置保存到資料庫

### 後端 API

- `GET /api/church/position-config/default` - 獲取默認配置
- `POST /api/church/position-config/default` - 保存默認配置
- `GET /api/church/position-config/{configName}` - 根據名稱獲取配置
- `POST /api/church/position-config` - 保存或更新配置（支持自定義名稱）

## 注意事項

1. 如果資料庫中沒有配置，前端會使用空配置（用戶可以手動添加）
2. 保存配置時會覆蓋現有的默認配置
3. 配置數據以 JSON 格式存儲在資料庫中

