package com.example.helloworld.util;

import com.example.helloworld.service.church.ConfigurationRefreshService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {

    @Autowired
    private ConfigurationRefreshService configurationRefreshService;

    // 緩存 SecretKey 以提升效能（配置變更時會重新計算）
    private volatile SecretKey cachedSigningKey;
    private volatile String cachedSecret;

    private SecretKey getSigningKey() {
        // 從資料庫讀取最新的 secret
        String currentSecret = configurationRefreshService.getConfigValue(
            "jwt.secret", 
            "F/cPluFKK3/44X5iX9GdY6P7Ye+BIDdBTw6uljBTl9o="
        );
        
        // 如果 secret 沒有變更，使用緩存的 Key
        if (cachedSigningKey != null && currentSecret.equals(cachedSecret)) {
            return cachedSigningKey;
        }
        
        // 重新計算 SigningKey
        String key = currentSecret;
        if (key.length() < 32) {
            key = key + "01234567890123456789012345678901"; // 補足到至少 32 字符
        }
        SecretKey signingKey = Keys.hmacShaKeyFor(key.substring(0, 32).getBytes());
        
        // 更新緩存
        cachedSecret = currentSecret;
        cachedSigningKey = signingKey;
        
        return signingKey;
    }

    private Long getAccessTokenExpiration() {
        return configurationRefreshService.getConfigValueAsLong("jwt.access-token-expiration", 3600000L);
    }

    private Long getRefreshTokenExpiration() {
        return configurationRefreshService.getConfigValueAsLong("jwt.refresh-token-expiration", 604800000L);
    }

    private Boolean isRefreshTokenEnabled() {
        return configurationRefreshService.getConfigValueAsBoolean("jwt.refresh-token-enabled", true);
    }

    /**
     * 生成 Access Token（短效期）
     */
    public String generateAccessToken(String username, String system) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("system", system); // 標記系統類型：personal 或 church
        claims.put("type", "access"); // 標記 Token 類型
        return createToken(claims, username, getAccessTokenExpiration());
    }

    /**
     * 生成 Refresh Token（長效期）
     * 如果 Refresh Token 功能被禁用，返回 null
     */
    public String generateRefreshToken(String username, String system) {
        if (!isRefreshTokenEnabled()) {
            return null; // Refresh Token 功能已禁用
        }
        Map<String, Object> claims = new HashMap<>();
        claims.put("system", system); // 標記系統類型：personal 或 church
        claims.put("type", "refresh"); // 標記 Token 類型
        return createToken(claims, username, getRefreshTokenExpiration());
    }

    /**
     * 生成教會系統 Access Token
     */
    public String generateChurchAccessToken(String username) {
        return generateAccessToken(username, "church");
    }

    /**
     * 生成教會系統 Refresh Token
     */
    public String generateChurchRefreshToken(String username) {
        return generateRefreshToken(username, "church");
    }

    /**
     * 生成個人網站 Access Token
     */
    public String generatePersonalAccessToken(String username) {
        return generateAccessToken(username, "personal");
    }

    /**
     * 生成個人網站 Refresh Token
     */
    public String generatePersonalRefreshToken(String username) {
        return generateRefreshToken(username, "personal");
    }

    /**
     * 從 Refresh Token 生成新的 Access Token
     */
    public String refreshAccessToken(String refreshToken) {
        try {
            String username = extractUsername(refreshToken);
            String system = extractSystem(refreshToken);
            String tokenType = extractClaim(refreshToken, claims -> claims.get("type", String.class));
            
            // 驗證是 Refresh Token
            if (!"refresh".equals(tokenType)) {
                throw new IllegalArgumentException("無效的 Refresh Token");
            }
            
            // 生成新的 Access Token
            return generateAccessToken(username, system);
        } catch (Exception e) {
            throw new IllegalArgumentException("無法刷新 Token: " + e.getMessage());
        }
    }

    private String createToken(Map<String, Object> claims, String subject, Long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 從 Token 中提取用戶名
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * 從 Token 中提取系統類型
     */
    public String extractSystem(String token) {
        return extractClaim(token, claims -> claims.get("system", String.class));
    }

    /**
     * 從 Token 中提取過期時間
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 檢查 Token 是否過期
     */
    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * 驗證 Token（不檢查黑名單，由調用者檢查）
     */
    public Boolean validateToken(String token, String username, String expectedSystem, String expectedType) {
        try {
            final String tokenUsername = extractUsername(token);
            final String tokenSystem = extractSystem(token);
            final String tokenType = extractClaim(token, claims -> claims.get("type", String.class));
            
            return (tokenUsername.equals(username) && 
                    tokenSystem != null && 
                    tokenSystem.equals(expectedSystem) &&
                    tokenType != null &&
                    tokenType.equals(expectedType) &&
                    !isTokenExpired(token));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 驗證 Access Token
     */
    public Boolean validateAccessToken(String token, String username, String expectedSystem) {
        return validateToken(token, username, expectedSystem, "access");
    }

    /**
     * 驗證 Refresh Token
     */
    public Boolean validateRefreshToken(String token, String username, String expectedSystem) {
        return validateToken(token, username, expectedSystem, "refresh");
    }

    /**
     * 驗證教會系統 Access Token
     */
    public Boolean validateChurchAccessToken(String token, String username) {
        return validateAccessToken(token, username, "church");
    }

    /**
     * 驗證個人網站 Access Token
     */
    public Boolean validatePersonalAccessToken(String token, String username) {
        return validateAccessToken(token, username, "personal");
    }

    /**
     * 驗證教會系統 Refresh Token
     */
    public Boolean validateChurchRefreshToken(String token, String username) {
        return validateRefreshToken(token, username, "church");
    }

    /**
     * 驗證個人網站 Refresh Token
     */
    public Boolean validatePersonalRefreshToken(String token, String username) {
        return validateRefreshToken(token, username, "personal");
    }

    /**
     * 提取 Token 類型
     */
    public String extractTokenType(String token) {
        return extractClaim(token, claims -> claims.get("type", String.class));
    }
}

