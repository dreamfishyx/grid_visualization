package com.dreamfish.sea.oldbook.util;

import lombok.extern.slf4j.Slf4j;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @Author: Dream fish
 * @Date: 2023/9/26 17:05
 * @Description: MD5加密工具类
 * @Version: 1.0
 **/
@Slf4j
public class MD5 {
    private static final String salt = "Q3RyaXZlUGx1cw=="; // 盐值(一般不直接写死，但是麻烦)

    public static String encrypt(String originalString) {
        String result = null;
        try {
            // 获取MD5实例
            MessageDigest md = MessageDigest.getInstance("MD5");
            // 添加盐
            originalString += salt;
            // 计算摘要
            md.update(originalString.getBytes());
            byte[] digest = md.digest();
            // 转换为16进制字符串
            result = toHexString(digest);
        } catch (NoSuchAlgorithmException e) {
            log.error("MD5加密失败", e);
//            e.printStackTrace();
        }
        return result;
    }

    private static String toHexString(byte[] bytes) {  // 转换为16进制字符串
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));  // 02x表示两位16进制数，前面不足补0
        }
        return sb.toString();
    }


}
