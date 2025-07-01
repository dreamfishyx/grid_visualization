package com.dreamfish.backend.controller;

import com.dreamfish.backend.common.ErrorCode;
import com.dreamfish.backend.common.Result;
import com.dreamfish.backend.component.JwtUtil;
import com.dreamfish.backend.component.TokenBlacklist;
import com.dreamfish.backend.component.VerificationCodeUtil;
import com.dreamfish.backend.entity.CustomUser;
import com.dreamfish.backend.entity.User;
import com.dreamfish.backend.service.EmailService;
import com.dreamfish.backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Dream fish
 * @version 1.0
 * @description: 用户操作
 * @date 2025/3/5 09:37
 */
@Slf4j
@RestController
@Tag(
        name = "用户操作",
        description = "登录、注册、修改密码、忘记密码、退出登录、获取用户信息"
)
@RequestMapping("/app/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final TokenBlacklist tokenBlacklist;
    private final VerificationCodeUtil codeUtil;
    private final EmailService emailService;

    @Operation(
            summary = "发送验证码",
            description = "向指定邮箱发送验证码(异步),用于注册阶段",
            parameters = {
                    @Parameter(
                            name = "email",
                            description = "邮箱",
                            required = true
                    )
            }
    )
    @GetMapping("/send-code")
    public Result<?> sendCode(String email) {

        log.info("send-code accept email: {}", email);

        if (email == null) {
            return Result.error(HttpStatus.BAD_REQUEST, ErrorCode.EMAIL_ADDRESS_INVALID, "邮箱不能为空");
        }

        // 邮箱格式校验
        if (!email.matches("^[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) {
            return Result.error(HttpStatus.BAD_REQUEST, ErrorCode.EMAIL_ADDRESS_INVALID, "邮箱格式不正确");
        }

        // 判断邮箱是否已经注册
        if (userService.existsByEmail(email)) {
            return Result.error(HttpStatus.BAD_REQUEST, ErrorCode.EMAIL_EXISTS, "邮箱已经注册");
        }

        // 生成6位验证码并发送
        try {
            String code = codeUtil.generateCode(email);
            log.info("注册:正在发送验证码至邮箱: {}, 验证码: {}", email, code);
            // emailService.sendVerifyCode(email, code);
            emailService.asyncSendVerifyCode(email, code);
            return Result.success();
        } catch (Exception e) {
            log.error("邮件发送失败", e);
            return Result.error(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.EMAIL_ADDRESS_INVALID, "邮件发送失败，请检查邮箱地址");
        }
    }

    @Operation(
            summary = "发送重置密码验证码",
            description = "向指定邮箱发送验证码(异步),用于重置密码"
    )
    @SecurityRequirement(name = "BearerAuth")
    @GetMapping("/send-reset-code")
    public Result<?> sendResetCode(
            @AuthenticationPrincipal CustomUser customUser
    ) {
        // 从 SecurityContextHolder 获取已经认证的用户信息
        String email = customUser.getEmail();

        // 其实这里可以不用校验,注册时候已经校验过
        if (email == null) {
            return Result.error(HttpStatus.BAD_REQUEST, ErrorCode.EMAIL_ADDRESS_INVALID, "邮箱不能为空");
        }

        // 生成6位验证码并发送
        try {
            String code = codeUtil.generateCode(email);
            log.info("重置密码:正在发送验证码至邮箱: {}, 验证码: {}", email, code);
            emailService.asyncSendVerifyCode(email, code);
            return Result.success();
        } catch (Exception e) {
            log.error("邮件发送失败", e);
            return Result.error(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.EMAIL_ADDRESS_INVALID, "邮件发送失败，请检查邮箱地址");
        }
    }

    @Operation(
            summary = "用户注册",
            description = "用户注册,需要邮箱验证码、用户名、密码、邮箱。",
            parameters = {
                    @Parameter(
                            name = "email",
                            description = "邮箱",
                            required = true
                    ),
                    @Parameter(
                            name = "verifyCode",
                            description = "邮箱验证码",
                            required = true
                    ),
                    @Parameter(
                            name = "userName",
                            description = "用户名",
                            required = true
                    ),
                    @Parameter(
                            name = "password",
                            description = "密码",
                            required = true
                    )
            }
    )
    @PostMapping("/register")
    public Object register(@Validated @RequestBody User user) {
        // 其他校验交给 JSR303 处理

        //邮箱是否为空:主要是登录不需要邮箱，JSR303校验就没用@NotBlank
        if (user.getEmail() == null || user.getEmail().isEmpty()) {
            return Result.error(HttpStatus.BAD_REQUEST, ErrorCode.EMAIL_ADDRESS_INVALID, "邮箱不能为空");
        }

        // 验证码校验
        codeUtil.verifyCode(user.getEmail(), user.getVerifyCode());

        // 判断用户名是否已经注册
        if (userService.existsByName(user.getUserName())) {
            return Result.error(HttpStatus.BAD_REQUEST, ErrorCode.USERNAME_EXISTS);
        }

        // 密码加密
        String encodedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(encodedPassword);

        // 注册
        userService.register(user);

        // 返回成功
        return Result.success(null);
    }

    @Operation(
            summary = "用户登录",
            description = "用户登录,需要用户名、密码。",
            parameters = {
                    @Parameter(
                            name = "userName",
                            description = "用户名",
                            required = true
                    ),
                    @Parameter(
                            name = "password",
                            description = "密码",
                            required = true
                    )
            }
    )
    @PostMapping("/login")
    public Object login(@Validated @RequestBody User user) {
        // 其他校验交给 JSR303 处理

        if (user.getUserName() == null || user.getPassword() == null) {
            return Result.error(HttpStatus.BAD_REQUEST, ErrorCode.NAME_OR_PASSWORD_ERROR, "用户名或密码不能为空");
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        user.getUserName(),
                        user.getPassword()
                )
        );

        // 设置认证信息到 SecurityContextHolder
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 获取已经认证的用户信息
        CustomUser customUser = (CustomUser) authentication.getPrincipal();

        // 生成 JWT
        String accessToken = jwtUtil.generateAccessToken(customUser.getUsername(), customUser.getUserId());
        String refreshToken = jwtUtil.generateRefreshToken(customUser.getUsername(), customUser.getUserId());

        // 将 JWT 返回给前端
        HashMap<String, String> map = new HashMap<>();
        map.put("accessToken", accessToken);
        map.put("refreshToken", refreshToken);

        // 设置 Token 过期时间
        map.put("expiration", jwtUtil.getExpiration(accessToken).getTime() + "");

        // 设置用户信息
        map.put("userId", customUser.getUserId() + "");
        map.put("userName", customUser.getUsername());
        map.put("email", customUser.getEmail());
        return Result.success(map);
    }

    @Operation(
            summary = "刷新 Token",
            description = "刷新 Token,需要 Refresh Token。",
            parameters = {
                    @Parameter(
                            name = "refreshToken",
                            description = "Refresh Token",
                            required = true
                    )
            }
    )
    @PostMapping("/refresh-token")
    public Result<?> refreshToken(@RequestBody Map<String, String> request) {

        String refreshToken = request.get("refreshToken");
        if (!jwtUtil.validateToken(refreshToken) || tokenBlacklist.isBlacklisted(refreshToken)) {
            return Result.error(HttpStatus.UNAUTHORIZED, ErrorCode.AUTH_UNAUTHORIZED);
        }

        String username = jwtUtil.extractUsername(refreshToken);
        Integer userId = jwtUtil.extractUid(refreshToken);

        // 生成新的 Access Token
        String newAccessToken = jwtUtil.generateAccessToken(username, userId);
        String newRefreshToken = jwtUtil.generateRefreshToken(username, userId);

        // 将旧的 Refresh Token 加入黑名单
        tokenBlacklist.addToBlacklist(refreshToken);

        HashMap<String, String> map = new HashMap<>();
        map.put("accessToken", newAccessToken);
        map.put("refreshToken", newRefreshToken);
        map.put("expiration", jwtUtil.getExpiration(newAccessToken).getTime() + "");
        return Result.success(map);
    }

    @Operation(
            summary = "用户退出登录",
            description = "用户退出登录,需要 Token。"
    )
    @SecurityRequirement(name = "BearerAuth")
    @GetMapping("/logout")
    public Result<?> logout(HttpServletRequest request) {

        String token = jwtUtil.parseJwt(request);
        log.info("logout accept token: {}", token);

        if (token != null && jwtUtil.validateToken(token)) {
            tokenBlacklist.addToBlacklist(token);
        }

        // 清除当前请求的 Security 上下文
        SecurityContextHolder.clearContext();
        return Result.success(null);
    }

    @Operation(
            summary = "获取用户信息",
            description = "获取用户信息,需要 Token。"
    )
    @SecurityRequirement(name = "BearerAuth")
    @GetMapping("/info")
    public Result<?> userInfo(@AuthenticationPrincipal CustomUser customUser) {
        if (customUser == null) {
            return Result.error(HttpStatus.BAD_GATEWAY, ErrorCode.AUTH_PERMISSION_DENIED);
        }
        return Result.success(
                new User()
                        .setUserId(customUser.getUserId())
                        .setUserName(customUser.getUsername())
                        .setEmail(customUser.getEmail())
        );
    }

    @Operation(
            summary = "重置密码",
            description = "重置密码,需要 Token、新密码、验证码。",
            parameters = {
                    @Parameter(
                            name = "password",
                            description = "新密码",
                            required = true
                    ),
                    @Parameter(
                            name = "verifyCode",
                            description = "验证码",
                            required = true
                    )
            }
    )
    @SecurityRequirement(name = "BearerAuth")
    @PostMapping("/reset-password")
    public Result<?> resetPassword(
            @RequestBody User user,
            @AuthenticationPrincipal CustomUser customUser) {

        // 验证码校验
        codeUtil.verifyCode(customUser.getEmail(), user.getVerifyCode());

        // 密码加密
        String encodedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(encodedPassword);

        userService.resetPassword(user.setUserId(customUser.getUserId()));

        return Result.success(null);
    }
}
