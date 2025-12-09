package com.example.helloworld.service.personal;

import com.example.helloworld.entity.personal.Asset;
import com.example.helloworld.entity.personal.User;
import com.example.helloworld.repository.personal.AssetRepository;
import com.example.helloworld.repository.personal.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AssetService {

    @Autowired
    private AssetRepository assetRepository;

    @Autowired
    private UserRepository userRepository;

    public List<Asset> getAllAssets() {
        return assetRepository.findAllOrdered();
    }

    public Optional<Asset> getAssetById(Long id) {
        return assetRepository.findById(id);
    }

    public Asset saveAsset(Asset asset) {
        // 設置用戶 ID
        String currentUserUid = getCurrentUserUid();
        if (asset.getCreatedByUid() == null) {
            asset.setCreatedByUid(currentUserUid);
        }
        asset.setUpdatedByUid(currentUserUid);

        return assetRepository.save(asset);
    }

    /**
     * 獲取當前登入用戶的 UID
     */
    private String getCurrentUserUid() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() &&
            !authentication.getPrincipal().equals("anonymousUser")) {
            String username = authentication.getName();
            // 根據 username 查找對應的 uid
            return userRepository.findByUsername(username)
                    .map(User::getUid)
                    .orElse(null);
        }
        return null;
    }

    public void deleteAsset(Long id) {
        assetRepository.deleteById(id);
    }
}
