# API Response 統一化遷移注意事項

## 已遇到的問題和解決方案

### 1. 前端 API 調用問題
**問題**：前端頁面還在手動調用 `response.json()`，但 `apiRequest` 已經更新為自動返回解析後的資料。

**解決方案**：
- 移除所有手動的 `response.ok` 檢查
- 移除所有手動的 `response.json()` 調用
- 直接使用 `apiRequest` 的返回值
- 處理 `ApiResponse` 格式（`success` 和 `data` 欄位）
- 保持向後兼容（處理非 `ApiResponse` 格式的響應）

### 2. ApiResponse 格式處理
**問題**：有些 API 返回 `{ success: true, data: {...} }`，有些返回 `{ success: true }`（沒有 data 欄位）。

**解決方案**：
- 在 `apiRequest` 中：如果 `success` 為 true，返回 `apiResponse.data`（如果存在），否則返回整個 `apiResponse`
- 在前端調用中：處理多種可能的數據結構

### 3. 數組類型檢查
**問題**：API 返回的資料可能不是數組，導致 `.find()`、`.map()` 等方法失敗。

**解決方案**：
- 使用 `Array.isArray()` 檢查
- 如果不是數組，設為空數組 `[]`
- 在調用數組方法前先檢查

### 4. 空響應處理
**問題**：某些 API 返回 200 狀態碼但沒有 body（如 `ResponseEntity.ok().build()`），導致 JSON 解析失敗。

**解決方案**：
- 在 `apiRequest` 中：對於 200/201/204 狀態碼但 JSON 解析失敗的情況，返回空對象 `{}` 而不是拋出錯誤

### 5. 特殊情況處理
**問題**：下載檔案（返回 blob）不能使用 `apiRequest`。

**解決方案**：
- 直接使用 `fetch` API
- 手動設置 headers（包括 Authorization）
- 使用 `response.blob()` 獲取檔案

## 後端遷移指南

### 遷移步驟

1. **導入 ApiResponse 和 PageResponse**
```java
import com.example.helloworld.dto.common.ApiResponse;
import com.example.helloworld.dto.common.PageResponse;
```

2. **成功響應格式**
```java
// 單一對象
return ResponseEntity.ok(ApiResponse.ok(data));

// 列表
return ResponseEntity.ok(ApiResponse.ok(list));

// 分頁
PageResponse<T> pageResponse = new PageResponse<>(
    page.getContent(),
    page.getTotalElements(),
    page.getTotalPages(),
    page.getNumber(),
    page.getSize()
);
return ResponseEntity.ok(ApiResponse.ok(pageResponse));

// 簡單成功（無數據）
return ResponseEntity.ok(ApiResponse.ok(null));
```

3. **錯誤響應格式**
```java
return ResponseEntity.badRequest()
    .body(ApiResponse.fail("錯誤訊息"));
```

4. **HTTP 狀態碼**
- 成功：使用 `ResponseEntity.ok()`
- 業務邏輯錯誤：使用 `ResponseEntity.badRequest()` + `ApiResponse.fail()`
- 認證錯誤：使用 `ResponseEntity.status(HttpStatus.UNAUTHORIZED)` + `ApiResponse.fail()`
- 資源不存在：使用 `ResponseEntity.notFound()` + `ApiResponse.fail()`

### 需要遷移的 Controller 列表

#### 教會相關（Church）— 後端已全部完成
- [x] ChurchAuthController - 已完成
- [x] AboutInfoAdminController - 已完成
- [x] ChurchUserController - 已完成
- [x] SundayMessageAdminController - 已完成
- [x] AnnouncementAdminController - 已完成
- [x] ActivityAdminController - 已完成
- [x] PrayerRequestAdminController - 已完成
- [x] GroupController - 已完成（getGroupById 已使用 ApiResponse）
- [x] ServiceScheduleController - 已完成（已更新前端處理 PageResponse）
- [x] PositionManagementController - 已完成（已更新前端處理 PageResponse）
- [x] ChurchUrlPermissionController - 已完成（已更新前端處理 PageResponse）
- [x] ChurchMenuController - 已完成（管理 API 已遷移；公開 API /frontend、/admin、/dashboard 刻意保持不包 ApiResponse）
- [x] ChurchPermissionController - 已完成
- [x] ChurchRoleController - 已完成
- [x] ChurchInfoAdminController - 已完成
- [x] PersonManagementController - 已完成
- [x] AttendanceRateController - 已完成
- [x] ContactSubmissionAdminController - 已完成
- [x] ChurchScheduledJobController - 已完成（前端 api.js 排程函數已改為使用 apiRequest 返回值，不再使用 response.ok / response.json()）
- [x] BackupController - 已完成（列表/建立/刪除為 ApiResponse；下載為 blob，前端維持 fetch）
- [x] SystemSettingController - 已完成
- [x] FileUploadController - 已完成
- [x] FrontendDataController - 已完成
- [x] ContactSubmissionController - 已完成
- [x] AdminSessionController（checkin）- 已完成（Excel 匯出為 blob，維持 ResponseEntity.ok().body(bytes)）
- [x] AdminManualController（checkin）- 已完成（Excel 匯出為 blob，同上）
- [x] CheckinController - 已完成

#### 個人網站相關（Personal）— 後端已全部完成
- [x] AuthController - 已完成
- [x] UserController - 已完成
- [x] PermissionController - 已完成
- [x] RoleController - 已完成
- [x] MenuController - 已完成
- [x] UrlPermissionController - 已完成
- [x] RecordController - 已完成
- [x] AssetController - 已完成
- [x] ExpenseController - 已完成
- [x] ScheduledJobController - 已完成
- [x] SystemSettingController - 已完成
- [x] BackupController - 已完成（列表/建立/刪除為 ApiResponse；下載為 blob）
- [x] ExchangeRateController - 已完成
- [ ] LineBotController - 保留（LINE  webhook 需回傳純文字 "OK"，不包 ApiResponse）
- [ ] LineGroupManagementController - 未遷移（可選）
- [ ] PasswordHashController - 未遷移（工具用，可選）

#### 共用
- [ ] UploadController

## 前端遷移檢查清單

在遷移每個 Controller 後，需要檢查對應的前端頁面：

1. ✅ 移除 `response.ok` 檢查
2. ✅ 移除 `response.json()` 調用
3. ✅ 直接使用 `apiRequest` 返回值
4. ✅ 處理 `ApiResponse` 格式
5. ✅ 確保數組類型檢查
6. ✅ 處理分頁數據（`PageResponse`）
7. ✅ 處理錯誤情況

## 測試要點

1. 列表載入是否正常
2. 分頁是否正常
3. 新增/編輯/刪除是否正常
4. 錯誤處理是否正常
5. 特殊情況（空響應、blob 下載等）是否正常

## 前端遷移狀態（教會後台 frontend-church-admin）

- **api.js**：`apiRequest` 已統一處理 ApiResponse；**排程專用函數**（getChurchScheduledJobs、getChurchScheduledJobById 等）已改為直接使用 apiRequest 返回值，不再使用 `response.ok` / `response.json()`。
- **各 views/admin/*.vue**：Permissions、Roles、ChurchInfo、Positions、ScheduledJobs、Maintenance 等已使用 apiRequest 並處理 PageResponse / data 結構。
- **特殊保留**：Maintenance 備份下載、Login 健康檢查、CheckinView 公開簽到、SessionDetail/ManualView 的 Excel 匯出與錯誤解析等，因需 blob 或細部錯誤碼而維持使用 `fetch` 或 `response.json()`，屬預期行為。

## 前端遷移狀態（個人網站 frontend-personal）

- **useApi.js**：`request()` 已支援 ApiResponse（回傳 `apiResponse.data`）；**refreshAccessToken** 已改為解析 `{ success, data }`，可正確取得 `data.accessToken`。
- **各 views**：均透過 `apiService` 呼叫後端，因此會自動使用上述解析邏輯；**Maintenance.vue** 已調整為不依賴 `response.success`（如 refresh、deleteBackup、createSetting 等改為以「未拋錯即成功」或 `data != null` 判斷）。
- **特殊保留**：備份下載仍以 `fetch` + `response.blob()`；LINE 測試訊息等若仍用 `fetch` 可維持現狀，或後續改走 apiService。
