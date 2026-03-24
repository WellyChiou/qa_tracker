package com.example.helloworld.service.invest.auth;

import com.example.helloworld.entity.invest.auth.InvestUrlPermission;
import com.example.helloworld.repository.invest.auth.InvestUrlPermissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class InvestUrlPermissionService {

    private final InvestUrlPermissionRepository investUrlPermissionRepository;

    public InvestUrlPermissionService(InvestUrlPermissionRepository investUrlPermissionRepository) {
        this.investUrlPermissionRepository = investUrlPermissionRepository;
    }

    @Transactional(transactionManager = "investTransactionManager", readOnly = true)
    public List<InvestUrlPermission> getAllActivePermissions() {
        return investUrlPermissionRepository.findActivePermissionsOrdered();
    }
}
