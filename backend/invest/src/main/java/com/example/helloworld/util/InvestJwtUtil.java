package com.example.helloworld.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class InvestJwtUtil {

    @Value("${invest.jwt.secret:F/cPluFKK3/44X5iX9GdY6P7Ye+BIDdBTw6uljBTl9o=}")
    private String jwtSecret;

    @Value("${invest.jwt.access-token-expiration:3600000}")
    private long accessTokenExpiration;

    @Value("${invest.jwt.refresh-token-expiration:604800000}")
    private long refreshTokenExpiration;

    @Value("${invest.jwt.refresh-token-enabled:true}")
    private boolean refreshTokenEnabled;

    private volatile SecretKey cachedSigningKey;
    private volatile String cachedSecret;

    private SecretKey getSigningKey() {
        String currentSecret = jwtSecret;
        if (currentSecret == null || currentSecret.isBlank()) {
            currentSecret = "F/cPluFKK3/44X5iX9GdY6P7Ye+BIDdBTw6uljBTl9o=";
        }

        if (cachedSigningKey != null && currentSecret.equals(cachedSecret)) {
            return cachedSigningKey;
        }

        String key = currentSecret;
        if (key.length() < 32) {
            key = key + "01234567890123456789012345678901";
        }

        SecretKey signingKey = Keys.hmacShaKeyFor(key.substring(0, 32).getBytes());
        cachedSecret = currentSecret;
        cachedSigningKey = signingKey;
        return signingKey;
    }

    public String generateInvestAccessToken(String username) {
        return generateAccessToken(username, "invest");
    }

    public String generateInvestRefreshToken(String username) {
        return generateRefreshToken(username, "invest");
    }

    public String generateAccessToken(String username, String system) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("system", system);
        claims.put("type", "access");
        return createToken(claims, username, accessTokenExpiration);
    }

    public String generateRefreshToken(String username, String system) {
        if (!refreshTokenEnabled) {
            return null;
        }

        Map<String, Object> claims = new HashMap<>();
        claims.put("system", system);
        claims.put("type", "refresh");
        return createToken(claims, username, refreshTokenExpiration);
    }

    private String createToken(Map<String, Object> claims, String subject, long expiration) {
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

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractSystem(String token) {
        return extractClaim(token, claims -> claims.get("system", String.class));
    }

    public String extractTokenType(String token) {
        return extractClaim(token, claims -> claims.get("type", String.class));
    }

    public Boolean validateInvestAccessToken(String token, String username) {
        return validateToken(token, username, "invest", "access");
    }

    public Boolean validateInvestRefreshToken(String token, String username) {
        return validateToken(token, username, "invest", "refresh");
    }

    public Boolean validateToken(String token, String username, String expectedSystem, String expectedType) {
        try {
            String tokenUsername = extractUsername(token);
            String tokenSystem = extractSystem(token);
            String tokenType = extractTokenType(token);

            return tokenUsername.equals(username)
                && expectedSystem.equals(tokenSystem)
                && expectedType.equals(tokenType)
                && !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(getSigningKey())
            .build()
            .parseClaimsJws(token)
            .getBody();
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }
}
