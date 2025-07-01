package com.dreamfish.backend.component;

import com.dreamfish.backend.common.ErrorCode;
import com.dreamfish.backend.common.RedisKeyPrefix;
import com.dreamfish.backend.exception.ParameterValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * @author Dream fish
 * @version 1.0
 * @description: 验证码工具类
 * @date 2025/4/11 13:31
 */
@Component
@RequiredArgsConstructor
public class VerificationCodeUtil {
    private final StringRedisTemplate redisTemplate;

    @Value("${verification.code.expiration}")
    private int expiration;

    /**
     * 生成验证码(6位随机数)
     *
     * @param email 邮箱
     * @return 验证码
     */
    public String generateCode(String email) {
        Random random = new Random();
        String code = String.format("%06d", random.nextInt(999999));

        // 存储验证码
        saveCode(email, code);

        return code;
    }

    /**
     * 存储验证码
     *
     * @param email 邮箱
     * @param code  验证码
     */
    public void saveCode(String email, String code) {
        redisTemplate.opsForValue().set(RedisKeyPrefix.VERIFICATION_CODE.generateFullKey(email), code, expiration, TimeUnit.MINUTES);
    }

    /**
     * 验证验证码
     *
     * @param email     邮箱
     * @param userInput 用户输入的验证码
     */
    public void verifyCode(String email, String userInput) {
        // 判断验证码是否存在
        String savedCode = redisTemplate.opsForValue().get(RedisKeyPrefix.VERIFICATION_CODE.generateFullKey(email));
        if (savedCode == null) {
            throw new ParameterValidationException(ErrorCode.PARAMETER_ILLEGAL, "验证码已过期");
        }

        // 验证
        if (!savedCode.equals(userInput)) {
            throw new ParameterValidationException(ErrorCode.PARAMETER_ILLEGAL, "验证码错误");
        }

        // 清除验证码
        redisTemplate.delete(RedisKeyPrefix.VERIFICATION_CODE.generateFullKey(email));
    }

}
