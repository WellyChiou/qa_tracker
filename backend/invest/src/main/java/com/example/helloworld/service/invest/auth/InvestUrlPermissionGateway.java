package com.example.helloworld.service.invest.auth;

import com.example.helloworld.entity.invest.auth.InvestUrlPermission;
import com.example.helloworld.service.common.AuthDomain;
import com.example.helloworld.service.common.CommonUrlPermission;
import com.example.helloworld.service.common.UrlPermissionGateway;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InvestUrlPermissionGateway implements UrlPermissionGateway {

    private final InvestUrlPermissionService investUrlPermissionService;

    public InvestUrlPermissionGateway(InvestUrlPermissionService investUrlPermissionService) {
        this.investUrlPermissionService = investUrlPermissionService;
    }

    @Override
    public AuthDomain domain() {
        return AuthDomain.INVEST;
    }

    @Override
    public List<CommonUrlPermission> getActivePermissions() {
        return investUrlPermissionService.getAllActivePermissions().stream()
            .map(this::toCommonPermission)
            .toList();
    }

    private CommonUrlPermission toCommonPermission(InvestUrlPermission permission) {
        return new CommonUrlPermission(
            permission.getUrlPattern(),
            permission.getHttpMethod(),
            permission.getRequiredRole(),
            permission.getRequiredPermission(),
            permission.getIsPublic()
        );
    }
}
