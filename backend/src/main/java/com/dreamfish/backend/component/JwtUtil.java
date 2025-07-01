package com.dreamfish.backend.component;

import com.dreamfish.backend.common.ErrorCode;
import com.dreamfish.backend.exception.BusinessException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * @author Dream fish
 * @version 1.0
 * @description: JWT 工具类:生成、解析、校验、黑名单
 * @date 2025/4/9 18:33
 */
@Component
public class JwtUtil {
    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.accessTokenExpirationMin}")
    private long accessTokenExpirationMin;

    @Value("${jwt.refreshTokenExpirationMin}")
    private long refreshTokenExpirationMin;

    private SecretKey secretKey;

    // 项目启动时进行初始化
    @PostConstruct
    public void init() {
        // 验证密钥长度,HS512需要至少 512/64 字节
        if (secret.length() < 64) {
            throw new BusinessException(ErrorCode.SECRET_CONFIG_ERROR, "密钥长度过短,至少 512/64 字节");
        }

        // 安全生成密钥
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成 Access Token
     *
     * @param username 用户名
     * @param uid      用户 ID
     * @return Token
     */
    public String generateAccessToken(String username, Integer uid) {
        return Jwts.builder()
                .subject(username)
                .claim("uid", uid)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(accessTokenExpirationMin)))
                .signWith(secretKey, Jwts.SIG.HS512)
                .compact();
    }

    /**
     * 生成 Refresh Token
     *
     * @param username 用户名
     * @param uid      用户 ID
     * @return Refresh Token
     */
    public String generateRefreshToken(String username, Integer uid) {
        return Jwts.builder()
                .subject(username)
                .claim("uid", uid)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(refreshTokenExpirationMin)))
                .signWith(secretKey, Jwts.SIG.HS512)
                .compact();
    }

    /**
     * 从 Token 中获取用户 ID
     *
     * @param token Token
     * @return 用户 ID
     */
    public Integer extractUid(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.get("uid", Integer.class);
    }

    /**
     * 从 Token 中获取用户名
     *
     * @param token Token
     * @return 用户名
     */
    public String extractUsername(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
        } catch (ExpiredJwtException | MalformedJwtException | IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.AUTH_TOKEN_INVALID, "Token 过期或无效");
        }
    }

    /**
     * 校验 Token:尝试解析,如果过期或无效,返回 false
     *
     * @param token Token
     * @return 校验结果
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException | MalformedJwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 获取 Token 剩余时间
     *
     * @param token Token
     * @return 剩余时间(毫秒)
     */
    public Long getRemainingTime(String token) {
        Date expiration = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getExpiration();
        return expiration.getTime() - System.currentTimeMillis();
    }

    /**
     * 获取 Token 过期时间
     *
     * @param token Token
     * @return 过期时间
     */
    public Date getExpiration(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getExpiration();
    }

    /**
     * 从请求域中解析出 Token
     *
     * @param request 请求域
     * @return 解析结果
     */
    public String parseJwt(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            // 去掉 "Bearer " 前缀
            return header.substring(7);
        }
        return null;
    }
}
