package com.example.helloworld.service.personal;

import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Token 黑名單服務
 * 用於管理已撤銷的 Token
 */
@Service
public class TokenBlacklistService {

    // 使用 ConcurrentHashMap 存儲黑名單 Token（Key: token, Value: 過期時間戳）
    private final Set<String> blacklist = ConcurrentHashMap.newKeySet();
    
    // 定期清理過期的黑名單條目
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public TokenBlacklistService() {
        // 每小時清理一次過期的黑名單條目
        scheduler.scheduleAtFixedRate(this::cleanupExpiredTokens, 1, 1, TimeUnit.HOURS);
    }

    /**
     * 將 Token 加入黑名單
     */
    public void addToBlacklist(String token) {
        blacklist.add(token);
    }

    /**
     * 檢查 Token 是否在黑名單中
     */
    public boolean isBlacklisted(String token) {
        return blacklist.contains(token);
    }

    /**
     * 清理過期的黑名單條目（這裡簡化處理，實際可以根據 Token 的過期時間來清理）
     */
    private void cleanupExpiredTokens() {
        // 簡化實現：如果黑名單太大（超過 10000 條），清空它
        // 實際應用中可以根據 Token 的過期時間來清理
        if (blacklist.size() > 10000) {
            blacklist.clear();
        }
    }

    /**
     * 清除所有黑名單條目（用於測試或重置）
     */
    public void clearBlacklist() {
        blacklist.clear();
    }
}
