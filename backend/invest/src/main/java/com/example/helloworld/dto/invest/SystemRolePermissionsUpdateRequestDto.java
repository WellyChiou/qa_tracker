package com.example.helloworld.dto.invest;

import java.util.List;

public class SystemRolePermissionsUpdateRequestDto {
    private List<Long> permissionIds;

    public List<Long> getPermissionIds() {
        return permissionIds;
    }

    public void setPermissionIds(List<Long> permissionIds) {
        this.permissionIds = permissionIds;
    }
}
