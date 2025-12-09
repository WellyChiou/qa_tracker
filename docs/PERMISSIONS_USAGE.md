# Permissions 表的實際應用說明

## 概述

`permissions` 表定義了業務邏輯層面的權限，用於控制用戶可以做什麼操作。這些權限在系統中的多個層面被使用。

## 實際應用場景

### 1. **用戶登入時載入權限**

當用戶登入時，系統會從資料庫載入用戶的權限（包括角色權限和直接分配的權限），並轉換為 Spring Security 的 `GrantedAuthority`。

**位置**：`ChurchUserDetailsService.java`

```java
// 添加角色擁有的權限
Set<ChurchPermission> rolePermissions = role.getPermissions();
for (ChurchPermission permission : rolePermissions) {
    authorities.add(new SimpleGrantedAuthority("PERM_" + permission.getPermissionCode()));
}

// 添加用戶直接分配的權限
Set<ChurchPermission> userPermissions = user.getPermissions();
for (ChurchPermission permission : userPermissions) {
    authorities.add(new SimpleGrantedAuthority("PERM_" + permission.getPermissionCode()));
}
```

**格式**：權限代碼會加上 `PERM_` 前綴，例如 `SERVICE_SCHEDULE_READ` 會變成 `PERM_SERVICE_SCHEDULE_READ`

---

### 2. **Controller 層面的權限檢查**

在 Controller 方法上使用 `@PreAuthorize` 註解來檢查用戶是否有特定權限。

**位置**：各種 Controller 類別

**範例**：
```java
@GetMapping
@PreAuthorize("hasAuthority('PERM_CHURCH_ADMIN')")
public ResponseEntity<Map<String, Object>> getAllUsers() {
    // 只有擁有 PERM_CHURCH_ADMIN 權限的用戶才能訪問
}
```

**使用的 Controller**：
- `ChurchUserController` - 用戶管理需要 `PERM_CHURCH_ADMIN`
- `ChurchRoleController` - 角色管理需要 `PERM_CHURCH_ADMIN`
- `ChurchPermissionController` - 權限管理需要 `PERM_CHURCH_ADMIN`
- `ChurchUrlPermissionController` - URL 權限管理需要 `PERM_CHURCH_ADMIN`
- `ChurchMenuController` - 菜單管理需要 `PERM_CHURCH_ADMIN`

---

### 3. **菜單權限檢查**

後台菜單會根據 `menu_items` 表中的 `required_permission` 欄位來決定是否顯示給用戶。

**位置**：`ChurchMenuService.java`

```java
private boolean hasPermission(ChurchMenuItem menuItem, Set<String> userPermissions) {
    // 如果菜單不需要權限，任何人都可以查看
    if (menuItem.getRequiredPermission() == null || menuItem.getRequiredPermission().isEmpty()) {
        return true;
    }
    
    // 檢查用戶是否有對應的權限
    String requiredPermission = "PERM_" + menuItem.getRequiredPermission();
    return userPermissions.contains(requiredPermission);
}
```

**應用**：
- 如果菜單項的 `required_permission` 為 `SERVICE_SCHEDULE_READ`，只有擁有該權限的用戶才能看到這個菜單
- 如果 `required_permission` 為空，所有登入用戶都可以看到

---

### 4. **URL 權限檢查**

在 `url_permissions` 表中，可以設定某個 URL 需要特定的權限（通過 `required_permission` 欄位）。

**位置**：`UrlPermissionFilter.java`

```java
// 檢查權限（如果需要）
if (cp.getRequiredPermission() != null && !cp.getRequiredPermission().isEmpty()) {
    Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
    String permissionCode = "PERM_" + cp.getRequiredPermission();
    boolean hasPermission = authorities.stream()
        .anyMatch(auth -> auth.getAuthority().equals(permissionCode));
    
    if (!hasPermission) {
        sendForbiddenResponse(request, response);
        return;
    }
}
```

**應用**：
- 在 `url_permissions` 表中設定 `/api/church/service-schedules` POST 方法需要 `SERVICE_SCHEDULE_EDIT` 權限
- 當用戶嘗試新增服事表時，系統會檢查用戶是否有 `PERM_SERVICE_SCHEDULE_EDIT` 權限

---

## 權限的分配方式

### 1. **通過角色分配**
- 在 `roles` 表中定義角色（如 `ROLE_CHURCH_ADMIN`）
- 在 `role_permissions` 表中將權限分配給角色
- 在 `user_roles` 表中將角色分配給用戶
- 用戶會自動獲得角色擁有的所有權限

### 2. **直接分配給用戶**
- 在 `user_permissions` 表中直接將權限分配給用戶
- 這種方式可以給特定用戶額外的權限，而不需要修改角色

---

## 權限代碼格式

- 資料庫中：`SERVICE_SCHEDULE_READ`、`PERSON_EDIT`、`CHURCH_ADMIN` 等
- Spring Security 中：會加上 `PERM_` 前綴，變成 `PERM_SERVICE_SCHEDULE_READ`、`PERM_PERSON_EDIT`、`PERM_CHURCH_ADMIN`

---

## 預設權限

根據 `church-security-tables.sql`，系統預設有以下權限：

1. `SERVICE_SCHEDULE_READ` - 查看服事表
2. `SERVICE_SCHEDULE_EDIT` - 編輯服事表（新增、修改、刪除）
3. `PERSON_READ` - 查看人員
4. `PERSON_EDIT` - 編輯人員（新增、修改、刪除）
5. `POSITION_READ` - 查看崗位
6. `POSITION_EDIT` - 編輯崗位（新增、修改、刪除）
7. `CHURCH_ADMIN` - 教會管理（可以存取所有教會管理功能）

---

## 總結

`permissions` 表是權限系統的核心，它：
1. 定義了系統中所有可用的權限
2. 通過角色或直接分配的方式給用戶
3. 在 Controller、菜單、URL 等多個層面進行權限檢查
4. 確保只有有權限的用戶才能執行相應的操作

