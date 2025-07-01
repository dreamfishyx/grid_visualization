package com.dreamfish.backend.component;

import com.dreamfish.backend.common.ErrorCode;
import com.dreamfish.backend.common.Result;
import com.dreamfish.backend.entity.CustomUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * @author Dream fish
 * @version 1.0
 * @description: JWT 认证过滤器:继承 OncePerRequestFilter, 确保每个请求只执行一次
 * @date 2025/4/5 16:37
 */

@Component
@AllArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    private final TokenBlacklist tokenBlacklist;

    @Override
    protected void doFilterInternal(@NotNull HttpServletRequest request,
                                    @NotNull HttpServletResponse response,
                                    @NotNull FilterChain filterChain)
            throws ServletException, IOException {
        // 从请求中获取 JWT
        final String jwt = jwtUtil.parseJwt(request);

        // 如果没有 JWT,直接放行,不进行认证
        if (jwt == null || jwt.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        // 检查 JWT 是否在黑名单中
        if (tokenBlacklist.isBlacklisted(jwt)) {
            log.warn("Token {} 已被加入黑名单，拒绝访问", jwt);
            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_OK);
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonResult = objectMapper.writeValueAsString(Result.error(HttpStatus.UNAUTHORIZED, ErrorCode.AUTH_UNAUTHORIZED));
            response.getWriter().write(jsonResult);
            return;
        }
        // 解析 JWT ,并设置认证信息到 SecurityContextHolder
        String username = jwtUtil.extractUsername(jwt);
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            // UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            CustomUser userDetails = (CustomUser) userDetailsService.loadUserByUsername(username);
            if (jwtUtil.validateToken(jwt) && userDetails != null) {
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(userDetails, null,
                                userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
        filterChain.doFilter(request, response);
    }
}