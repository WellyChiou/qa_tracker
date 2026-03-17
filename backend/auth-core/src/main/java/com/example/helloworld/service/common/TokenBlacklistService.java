package com.example.helloworld.service.common;

import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Token 黑名單服務
 * 供 personal/church 共用，用於管理已撤銷的 Token。
 */
public class TokenBlacklistService {

    private final Set<String> blacklist = ConcurrentHashMap.newKeySet();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public TokenBlacklistService() {
        scheduler.scheduleAtFixedRate(this::cleanupExpiredTokens, 1, 1, TimeUnit.HOURS);
    }

    public void addToBlacklist(String token) {
        blacklist.add(token);
    }

    public boolean isBlacklisted(String token) {
        return blacklist.contains(token);
    }

    private void cleanupExpiredTokens() {
        if (blacklist.size() > 10000) {
            blacklist.clear();
        }
    }

    public void clearBlacklist() {
        blacklist.clear();
    }
}
