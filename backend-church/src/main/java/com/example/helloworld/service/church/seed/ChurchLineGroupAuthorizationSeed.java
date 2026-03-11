package com.example.helloworld.service.church.seed;

import com.example.helloworld.entity.church.ChurchPermission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ChurchLineGroupAuthorizationSeed implements ChurchAuthorizationSeedModule {
    private static final Logger log = LoggerFactory.getLogger(ChurchLineGroupAuthorizationSeed.class);
    private static final String READ_PERMISSION = "LINE_GROUP_READ";
    private static final String EDIT_PERMISSION = "LINE_GROUP_EDIT";

    private final ChurchAuthorizationSeedSupport seedSupport;

    public ChurchLineGroupAuthorizationSeed(ChurchAuthorizationSeedSupport seedSupport) {
        this.seedSupport = seedSupport;
    }

    @Override
    public String moduleName() {
        return "line-groups";
    }

    public void initialize() {
        seed();
    }

    @Override
    public void seed() {
        ChurchPermission readPermission = ensurePermission(
                READ_PERMISSION,
                "LINE 群組檢視",
                "line_group",
                "read",
                "檢視教會 LINE 群組與成員"
        );
        ChurchPermission editPermission = ensurePermission(
                EDIT_PERMISSION,
                "LINE 群組管理",
                "line_group",
                "edit",
                "建立、修改與停用教會 LINE 群組與成員"
        );

        Long settingsMenuId = seedSupport.findMenuId("ADMIN_SETTINGS");

        ensureMenu(
                "ADMIN_LINE_GROUPS",
                "LINE 群組",
                "💬",
                "/line-groups",
                settingsMenuId,
                7,
                READ_PERMISSION,
                "管理教會 LINE 群組與成員"
        );

        ensureUrlPermission("/api/church/admin/line-groups", "GET", READ_PERMISSION, null, false, 266, true, "取得 LINE 群組列表");
        ensureUrlPermission("/api/church/admin/line-groups/*", "GET", READ_PERMISSION, null, false, 267, true, "取得單一 LINE 群組");
        ensureUrlPermission("/api/church/admin/line-groups", "POST", EDIT_PERMISSION, null, false, 268, true, "建立 LINE 群組");
        ensureUrlPermission("/api/church/admin/line-groups/*", "PUT", EDIT_PERMISSION, null, false, 269, true, "更新 LINE 群組");
        ensureUrlPermission("/api/church/admin/line-groups/*", "DELETE", EDIT_PERMISSION, null, false, 270, true, "刪除 LINE 群組");
        ensureUrlPermission("/api/church/admin/line-groups/*/members", "GET", READ_PERMISSION, null, false, 271, true, "取得 LINE 群組成員");
        ensureUrlPermission("/api/church/admin/line-groups/*/members", "POST", EDIT_PERMISSION, null, false, 272, true, "新增 LINE 群組成員");
        ensureUrlPermission("/api/church/admin/line-groups/*/members/*", "PUT", EDIT_PERMISSION, null, false, 273, true, "更新 LINE 群組成員");
        ensureUrlPermission("/api/church/admin/line-groups/*/members/*", "DELETE", EDIT_PERMISSION, null, false, 274, true, "停用 LINE 群組成員");

        seedSupport.assignPermissionsToRole("ROLE_CHURCH_ADMIN", readPermission, editPermission);
        log.info("✅ 已確認 church LINE 群組後台所需的 permission/menu/url_permission");
    }

    private ChurchPermission ensurePermission(String code, String name, String resource, String action, String description) {
        return seedSupport.ensurePermission(code, name, resource, action, description);
    }

    private void ensureMenu(
            String menuCode,
            String menuName,
            String icon,
            String url,
            Long parentId,
            int orderIndex,
            String requiredPermission,
            String description) {
        seedSupport.ensureMenu(menuCode, menuName, icon, url, parentId, orderIndex, requiredPermission, description);
    }

    private void ensureUrlPermission(
            String urlPattern,
            String httpMethod,
            String requiredPermission,
            String requiredRole,
            boolean isPublic,
            int orderIndex,
            boolean isActive,
            String description) {
        seedSupport.ensureUrlPermission(urlPattern, httpMethod, requiredPermission, requiredRole, isPublic, orderIndex, isActive, description);
    }
}
