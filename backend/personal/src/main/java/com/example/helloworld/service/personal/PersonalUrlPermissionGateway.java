package com.example.helloworld.service.personal;

import com.example.helloworld.entity.personal.UrlPermission;
import com.example.helloworld.service.common.AuthDomain;
import com.example.helloworld.service.common.CommonUrlPermission;
import com.example.helloworld.service.common.UrlPermissionGateway;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PersonalUrlPermissionGateway implements UrlPermissionGateway {
    private final UrlPermissionService urlPermissionService;

    public PersonalUrlPermissionGateway(UrlPermissionService urlPermissionService) {
        this.urlPermissionService = urlPermissionService;
    }

    @Override
    public AuthDomain domain() {
        return AuthDomain.PERSONAL;
    }

    @Override
    public List<CommonUrlPermission> getActivePermissions() {
        return urlPermissionService.getAllActivePermissions().stream()
                .map(this::toCommonPermission)
                .toList();
    }

    private CommonUrlPermission toCommonPermission(UrlPermission permission) {
        return new CommonUrlPermission(
                permission.getUrlPattern(),
                permission.getHttpMethod(),
                permission.getRequiredRole(),
                permission.getRequiredPermission(),
                permission.getIsPublic()
        );
    }
}
