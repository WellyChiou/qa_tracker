package com.example.helloworld.filter;

import com.example.helloworld.service.common.UrlPermissionGateway;
import com.example.helloworld.service.common.UrlPermissionPolicy;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UrlPermissionFilter extends AbstractUrlPermissionFilter {
    public UrlPermissionFilter(List<UrlPermissionGateway> urlPermissionGateways, UrlPermissionPolicy urlPermissionPolicy) {
        super(urlPermissionGateways, urlPermissionPolicy);
    }
}
