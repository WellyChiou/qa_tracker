package com.example.helloworld.util;

import com.example.helloworld.service.church.ConfigurationRefreshService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {

    @Autowired
    @Qualifier("personalConfigurationRefreshService")
    private com.example.helloworld.service.personal.ConfigurationRefreshService personalConfigurationRefreshService;

    @Autowired
    @Qualifier("churchConfigurationRefreshService")
    private com.example.helloworld.service.church.ConfigurationRefreshService churchConfigurationRefreshService;

    // 緩存 SecretKey 以提升效能（配置變更時會重新計算）
    // Personal 系統的緩存
    private volatile SecretKey cachedPersonalSigningKey;
    private volatile String cachedPersonalSecret;
    
    // Church 系統的緩存
    private volatile SecretKey cachedChurchSigningKey;
    private volatile String cachedChurchSecret;

    /**
     * 獲取 Personal 系統的 SigningKey
     */
    private SecretKey getPersonalSigningKey() {
        // 從資料庫讀取最新的 secret
        String currentSecret = personalConfigurationRefreshService.getConfigValue(
            "jwt.secret", 
            "F/cPluFKK3/44X5iX9GdY6P7Ye+BIDdBTw6uljBTl9o="
        );
        
        // 如果 secret 沒有變更，使用緩存的 Key
        if (cachedPersonalSigningKey != null && currentSecret.equals(cachedPersonalSecret)) {
            return cachedPersonalSigningKey;
        }
        
        // 重新計算 SigningKey
        String key = currentSecret;
        if (key.length() < 32) {
            key = key + "01234567890123456789012345678901"; // 補足到至少 32 字符
        }
        SecretKey signingKey = Keys.hmacShaKeyFor(key.substring(0, 32).getBytes());
        
        // 更新緩存
        cachedPersonalSecret = currentSecret;
        cachedPersonalSigningKey = signingKey;
        
        return signingKey;
    }

    /**
     * 獲取 Church 系統的 SigningKey
     */
    private SecretKey getChurchSigningKey() {
        // 從資料庫讀取最新的 secret
        String currentSecret = churchConfigurationRefreshService.getConfigValue(
            "jwt.secret", 
            "F/cPluFKK3/44X5iX9GdY6P7Ye+BIDdBTw6uljBTl9o="
        );
        
        // 如果 secret 沒有變更，使用緩存的 Key
        if (cachedChurchSigningKey != null && currentSecret.equals(cachedChurchSecret)) {
            return cachedChurchSigningKey;
        }
        
        // 重新計算 SigningKey
        String key = currentSecret;
        if (key.length() < 32) {
            key = key + "01234567890123456789012345678901"; // 補足到至少 32 字符
        }
        SecretKey signingKey = Keys.hmacShaKeyFor(key.substring(0, 32).getBytes());
        
        // 更新緩存
        cachedChurchSecret = currentSecret;
        cachedChurchSigningKey = signingKey;
        
        return signingKey;
    }

    /**
     * 根據系統類型獲取 SigningKey
     */
    private SecretKey getSigningKey(String system) {
        if ("church".equals(system)) {
            return getChurchSigningKey();
        } else {
            return getPersonalSigningKey();
        }
    }

    private SecretKey getSigningKey() {
        // 預設使用 Personal 系統（向後兼容）
        return getPersonalSigningKey();
    }

    /**
     * 獲取 Personal 系統的 Access Token 過期時間
     */
    private Long getPersonalAccessTokenExpiration() {
        return personalConfigurationRefreshService.getConfigValueAsLong("jwt.access-token-expiration", 3600000L);
    }

    /**
     * 獲取 Personal 系統的 Refresh Token 過期時間
     */
    private Long getPersonalRefreshTokenExpiration() {
        return personalConfigurationRefreshService.getConfigValueAsLong("jwt.refresh-token-expiration", 604800000L);
    }

    /**
     * 獲取 Personal 系統的 Refresh Token 是否啟用
     */
    private Boolean isPersonalRefreshTokenEnabled() {
        return personalConfigurationRefreshService.getConfigValueAsBoolean("jwt.refresh-token-enabled", true);
    }

    /**
     * 獲取 Church 系統的 Access Token 過期時間
     */
    private Long getChurchAccessTokenExpiration() {
        return churchConfigurationRefreshService.getConfigValueAsLong("jwt.access-token-expiration", 3600000L);
    }

    /**
     * 獲取 Church 系統的 Refresh Token 過期時間
     */
    private Long getChurchRefreshTokenExpiration() {
        return churchConfigurationRefreshService.getConfigValueAsLong("jwt.refresh-token-expiration", 604800000L);
    }

    /**
     * 獲取 Church 系統的 Refresh Token 是否啟用
     */
    private Boolean isChurchRefreshTokenEnabled() {
        return churchConfigurationRefreshService.getConfigValueAsBoolean("jwt.refresh-token-enabled", true);
    }

    // 向後兼容的方法（預設使用 Personal 系統）
    private Long getAccessTokenExpiration() {
        return getPersonalAccessTokenExpiration();
    }

    private Long getRefreshTokenExpiration() {
        return getPersonalRefreshTokenExpiration();
    }

    private Boolean isRefreshTokenEnabled() {
        return isPersonalRefreshTokenEnabled();
    }

    /**
     * 生成 Access Token（短效期）
     */
    public String generateAccessToken(String username, String system) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("system", system); // 標記系統類型：personal 或 church
        claims.put("type", "access"); // 標記 Token 類型
        
        Long expiration = "church".equals(system) ? getChurchAccessTokenExpiration() : getPersonalAccessTokenExpiration();
        return createToken(claims, username, expiration, system);
    }

    /**
     * 生成 Refresh Token（長效期）
     * 如果 Refresh Token 功能被禁用，返回 null
     */
    public String generateRefreshToken(String username, String system) {
        Boolean enabled = "church".equals(system) ? isChurchRefreshTokenEnabled() : isPersonalRefreshTokenEnabled();
        if (!enabled) {
            return null; // Refresh Token 功能已禁用
        }
        Map<String, Object> claims = new HashMap<>();
        claims.put("system", system); // 標記系統類型：personal 或 church
        claims.put("type", "refresh"); // 標記 Token 類型
        
        Long expiration = "church".equals(system) ? getChurchRefreshTokenExpiration() : getPersonalRefreshTokenExpiration();
        return createToken(claims, username, expiration, system);
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

    /**
     * 創建 Token（根據系統類型使用對應的 SigningKey）
     */
    private String createToken(Map<String, Object> claims, String subject, Long expiration, String system) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(system), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 創建 Token（向後兼容，預設使用 Personal 系統）
     */
    private String createToken(Map<String, Object> claims, String subject, Long expiration) {
        String system = (String) claims.get("system");
        if (system == null) {
            system = "personal"; // 預設使用 Personal 系統
        }
        return createToken(claims, subject, expiration, system);
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

    /**
     * 提取所有 Claims（根據 Token 中的 system 標記選擇對應的 SigningKey）
     */
    private Claims extractAllClaims(String token) {
        try {
            // 先嘗試使用 Personal SigningKey 解析
            return Jwts.parserBuilder()
                    .setSigningKey(getPersonalSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            // 如果失敗，嘗試使用 Church SigningKey
            try {
                return Jwts.parserBuilder()
                        .setSigningKey(getChurchSigningKey())
                        .build()
                        .parseClaimsJws(token)
                        .getBody();
            } catch (Exception e2) {
                // 如果都失敗，拋出原始異常
                throw e;
            }
        }
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

