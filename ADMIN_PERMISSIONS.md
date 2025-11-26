# ADMIN 角色權限配置說明

## 📋 當前配置

### ADMIN 角色可以訪問的端點

1. **管理端點（僅限 ADMIN）**：
   - `/api/users/**` - 用戶管理
   - `/api/roles/**` - 角色管理
   - `/api/permissions/**` - 權限管理
   - `/api/admin/**` - 其他管理功能

2. **所有需要認證的 API 端點**：
   - `/api/menus/**` - 菜單
   - `/api/records/**` - 記錄
   - `/api/expenses/**` - 記帳
   - `/api/assets/**` - 資產
   - `/api/exchange-rates/**` - 匯率
   - `/api/config/**` - 配置
   - `/api/**` - 所有其他 API 端點

3. **所有 HTML 頁面**：
   - `/*.html` - 所有 HTML 文件（除了已配置為公開的）

## 🔧 如何添加新的 ADMIN 專屬端點

如果您需要添加新的管理端點，只需要在 `SecurityConfig.java` 的 `authorizeHttpRequests` 中添加新的規則：

```java
.requestMatchers("/api/your-new-admin-endpoint/**").hasRole("ADMIN")
```

**重要**：這個規則必須放在通用規則（如 `/api/**`）之前，這樣才會優先匹配。

## 🔧 如何讓 ADMIN 完全繞過所有權限檢查

如果您想讓 ADMIN 完全繞過所有權限檢查，可以在配置中添加：

```java
.requestMatchers("/**").access("hasRole('ADMIN') or isAuthenticated()")
```

或者使用自定義的 AccessDecisionVoter。

## 📝 注意事項

1. **規則順序很重要**：Spring Security 按照配置的順序匹配規則，一旦匹配就不再檢查後續規則
2. **具體規則優先**：更具體的規則（如 `/api/users/**`）應該放在通用規則（如 `/api/**`）之前
3. **角色名稱**：Spring Security 會自動在角色名稱前添加 `ROLE_` 前綴，所以在配置中使用 `hasRole("ADMIN")` 實際檢查的是 `ROLE_ADMIN`

## 🎯 當前權限層級

```
公開端點（permitAll）
  ↓
管理端點（hasRole("ADMIN")）
  ↓
需要認證的 API（authenticated）
  ↓
其他 HTML 頁面（authenticated）
  ↓
所有其他請求（authenticated）
```

## 🔐 ADMIN 角色特權

ADMIN 角色通過以下方式獲得所有權限：

1. **角色層級**：ADMIN 角色在數據庫中被分配了所有權限
2. **URL 訪問**：ADMIN 可以訪問所有需要認證的 URL
3. **管理功能**：ADMIN 是唯一可以訪問管理端點的角色

## ✅ 驗證 ADMIN 權限

登入後，ADMIN 用戶應該能夠：
- ✅ 訪問所有 API 端點
- ✅ 訪問所有 HTML 頁面
- ✅ 訪問管理端點（用戶、角色、權限管理）
- ✅ 執行所有 CRUD 操作

