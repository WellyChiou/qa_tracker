package com.example.helloworld.service.common;

import java.util.List;

public interface UrlPermissionGateway {
    AuthDomain domain();

    List<CommonUrlPermission> getActivePermissions();
}
