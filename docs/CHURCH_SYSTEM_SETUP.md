# 教會系統設定指南

## 概述

教會系統已經建立為完全獨立的系統，擁有自己的權限管理、用戶系統和菜單管理。

## 架構說明

### 資料庫結構

- **qa_tracker**：個人系統資料庫
- **church**：教會系統資料庫（獨立）

### 系統分離

- **前端**：`frontend-church/` - 教會網站（前台 + 後台）
- **後端**：`backend/` - 共用後端，通過 package 結構分離（`controller/church/`, `service/church/`）

## 設定步驟

### 1. 執行 SQL 腳本

#### 步驟 1：建立權限表
```bash
mysql -u root -p church < mysql/church-security-tables.sql
```

#### 步驟 2：建立預設管理員
```bash
mysql -u root -p church < mysql/create-church-admin.sql
```

### 2. 預設帳號

- **用戶名**：`admin`
- **密碼**：`admin123`
- **角色**：`ROLE_CHURCH_ADMIN`（擁有所有權限）

**重要**：請在首次登入後立即修改密碼！

### 3. 權限說明

#### 預設角色

1. **ROLE_CHURCH_ADMIN**（教會系統管理員）
   - 擁有所有權限
   - 可以管理用戶、角色、權限、菜單

2. **ROLE_CHURCH_EDITOR**（教會系統編輯者）
   - 可以查看和編輯服事表、人員、崗位

3. **ROLE_CHURCH_VIEWER**（教會系統查看者）
   - 只能查看，不能編輯

#### 預設權限

- `SERVICE_SCHEDULE_READ` - 查看服事表
- `SERVICE_SCHEDULE_EDIT` - 編輯服事表
- `PERSON_READ` - 查看人員
- `PERSON_EDIT` - 編輯人員
- `POSITION_READ` - 查看崗位
- `POSITION_EDIT` - 編輯崗位
- `CHURCH_ADMIN` - 教會管理

### 4. 菜單系統

#### 前台菜單（公開訪問）
- 首頁
- 關於我們
- 活動
- 服事安排
- 聯絡我們

#### 後台菜單（需要登入）
- 儀表板
- 服事表管理
- 人員管理
- 崗位管理
- 系統設定（子菜單：用戶管理、角色管理、權限管理、菜單管理）

## 使用說明

### 前台使用

1. 訪問教會網站
2. 菜單會自動從資料庫載入
3. 所有人都可以查看服事表、人員、崗位資訊

### 後台使用

1. 點擊導航欄的「登入」按鈕
2. 使用管理員帳號登入
3. 登入後會看到「管理後台」連結
4. 進入後台可以管理服事表、人員、崗位等

### 管理功能

#### 服事表管理
- 新增、編輯、刪除服事表
- 需要 `SERVICE_SCHEDULE_EDIT` 權限

#### 人員管理
- 新增、編輯、刪除人員
- 需要 `PERSON_EDIT` 權限

#### 崗位管理
- 新增、編輯、刪除崗位
- 需要 `POSITION_EDIT` 權限

#### 系統管理（需要 CHURCH_ADMIN 權限）
- 用戶管理：管理教會系統用戶
- 角色管理：管理角色和權限分配
- 權限管理：管理權限定義
- 菜單管理：管理前台和後台菜單

## 技術細節

### API 端點

#### 認證相關
- `POST /api/church/auth/login` - 登入
- `POST /api/church/auth/logout` - 登出
- `GET /api/church/auth/current-user` - 獲取當前用戶

#### 菜單相關
- `GET /api/church/menus/frontend` - 獲取前台菜單（公開）
- `GET /api/church/menus/admin` - 獲取後台菜單（需要登入）

#### 服事表相關
- `GET /api/church/service-schedules` - 獲取服事表列表（公開）
- `POST /api/church/service-schedules` - 新增服事表（需要權限）
- `PUT /api/church/service-schedules/{id}` - 修改服事表（需要權限）
- `DELETE /api/church/service-schedules/{id}` - 刪除服事表（需要權限）

### 資料庫表結構

#### 權限相關表（church 資料庫）
- `users` - 教會系統用戶
- `roles` - 角色
- `permissions` - 權限
- `user_roles` - 用戶角色關聯
- `role_permissions` - 角色權限關聯
- `user_permissions` - 用戶權限關聯
- `menu_items` - 菜單項（包含 menu_type 欄位區分前台/後台）
- `url_permissions` - URL 權限配置

## 後續工作

1. ✅ 建立權限表
2. ✅ 建立預設角色和權限
3. ✅ 建立後端實體、Repository、Service
4. ✅ 建立認證 Controller
5. ✅ 建立菜單管理
6. ✅ 建立前端登入頁面
7. ✅ 建立後台管理頁面
8. ✅ 建立動態菜單載入
9. ⏳ 建立用戶管理頁面（後續）
10. ⏳ 建立角色和權限管理頁面（後續）
11. ⏳ 建立菜單管理頁面（後續）
12. ⏳ 移除前台編輯功能（等後台完成後）

## 注意事項

1. **資料庫獨立**：教會系統和個人系統使用不同的資料庫，完全分離
2. **用戶獨立**：教會系統的用戶和個人系統的用戶是分開的
3. **權限獨立**：教會系統有自己的權限管理系統
4. **菜單動態**：前台和後台菜單都從資料庫動態載入
5. **暫時保留**：前台的編輯功能暫時保留，等後台完成後再移除

