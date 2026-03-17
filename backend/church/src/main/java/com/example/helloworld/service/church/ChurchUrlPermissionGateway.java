package com.example.helloworld.service.church;

import com.example.helloworld.entity.church.ChurchUrlPermission;
import com.example.helloworld.service.common.AuthDomain;
import com.example.helloworld.service.common.CommonUrlPermission;
import com.example.helloworld.service.common.UrlPermissionGateway;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChurchUrlPermissionGateway implements UrlPermissionGateway {
    private final ChurchUrlPermissionService churchUrlPermissionService;

    public ChurchUrlPermissionGateway(ChurchUrlPermissionService churchUrlPermissionService) {
        this.churchUrlPermissionService = churchUrlPermissionService;
    }

    @Override
    public AuthDomain domain() {
        return AuthDomain.CHURCH;
    }

    @Override
    public List<CommonUrlPermission> getActivePermissions() {
        return churchUrlPermissionService.getAllActivePermissions().stream()
                .map(this::toCommonPermission)
                .toList();
    }

    private CommonUrlPermission toCommonPermission(ChurchUrlPermission permission) {
        return new CommonUrlPermission(
                permission.getUrlPattern(),
                permission.getHttpMethod(),
                permission.getRequiredRole(),
                permission.getRequiredPermission(),
                permission.getIsPublic()
        );
    }
}
