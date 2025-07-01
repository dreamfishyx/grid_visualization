package com.dreamfish.backend.parser;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Set;

/**
 * @author Dream fish
 * @version 1.0
 * @description: 通用字节转换器(暂不考虑无符号数)
 * @date 2025/4/3 18:44
 */
public class UniversalByteConverter {
    public static final Set<Class<?>> ALLOW_TYPE = Set.of(
            Byte.class, byte.class,
            Boolean.class, boolean.class,
            Short.class, short.class,
            Integer.class, int.class,
            Long.class, long.class,
            Float.class, float.class,
            Double.class, double.class,
            BigDecimal.class,
            String.class
    );

    /**
     * 将字节数组填充到指定长度,如果字节长度大于等于目标长度,直接截取,否则进行填充。用于动态 byte[] 转换
     *
     * @param bytes        需要转换的字节数组
     * @param order        字节序
     * @param targetLength 目标长度
     * @return 转换后的字节数组
     */
    private static byte[] padBytes(byte[] bytes, int targetLength, ByteOrder order) {
        if (bytes.length >= targetLength) {
            // 如果字节长度大于等于目标长度，直接截取
            return order == ByteOrder.BIG_ENDIAN ?
                    Arrays.copyOfRange(bytes, 0, targetLength) :
                    Arrays.copyOfRange(bytes, bytes.length - targetLength, bytes.length);
        }
        
        // 确定符号扩展字节 (0x00 或 0xFF)
        byte padByte = (bytes.length > 0) ?
                ((order == ByteOrder.BIG_ENDIAN) ?
                        ((bytes[0] < 0) ? (byte) 0xFF : 0x00) :
                        ((bytes[bytes.length - 1] < 0) ? (byte) 0xFF : 0x00)
                ) : 0x00;  // 空数组默认补0

        byte[] padded = new byte[targetLength];
        // 全部填充符号位
        Arrays.fill(padded, padByte);

        if (order == ByteOrder.BIG_ENDIAN) {
            // 大端: 原数据复制到右侧(低位)
            System.arraycopy(bytes, 0, padded, targetLength - bytes.length, bytes.length);
        } else {
            // 小端: 原数据复制到左侧(低位)
            System.arraycopy(bytes, 0, padded, 0, bytes.length);
        }
        return padded;
    }

    /**
     * 将字节数组反转
     *
     * @param bytes 待处理字节数组
     * @return 反转后的字节数组
     */
    private static byte[] reverseBytes(byte[] bytes) {
        byte[] reversed = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            reversed[i] = bytes[bytes.length - 1 - i];
        }
        return reversed;
    }

    /**
     * 将字节数组转换为字节
     *
     * @param bytes 待处理字节数组
     * @param order 字节序
     * @return 字节
     */
    public static byte converterToByte(byte[] bytes, ByteOrder order) {
        byte[] adjusted = padBytes(bytes, Byte.BYTES, order);
        return ByteBuffer.wrap(adjusted).order(order).get();
    }

    /**
     * 将字节数组转换为短整数
     *
     * @param bytes 待处理字节数组
     * @param order 字节序
     * @return 短整数
     */
    public static short converterToShort(byte[] bytes, ByteOrder order) {
        byte[] adjusted = padBytes(bytes, Short.BYTES, order);
        return ByteBuffer.wrap(adjusted).order(order).getShort();
    }

    /**
     * 将字节数组转换为整数
     *
     * @param bytes 待处理字节数组
     * @param order 字节序
     * @return 整数
     */
    public static int converterToInt(byte[] bytes, ByteOrder order) {
        byte[] adjusted = padBytes(bytes, Integer.BYTES, order);
        return ByteBuffer.wrap(adjusted).order(order).getInt();
    }

    /**
     * 将字节数组转换为长整数
     *
     * @param bytes 待处理字节数组
     * @param order 字节序
     * @return 长整数
     */
    public static long converterToLong(byte[] bytes, ByteOrder order) {
        byte[] adjusted = padBytes(bytes, Long.BYTES, order);
        return ByteBuffer.wrap(adjusted).order(order).getLong();
    }

    /**
     * 将字节数组转换为单精度浮点
     *
     * @param bytes 待处理字节数组
     * @param order 字节序
     * @param scale 缩放因子
     * @return 单精度浮点
     */
    public static float converterToFloat(byte[] bytes, ByteOrder order, int scale) {
        BigDecimal value = converterToScaledValue(bytes, order, scale);
        if (value.compareTo(BigDecimal.valueOf(Float.MAX_VALUE)) > 0 ||
                value.compareTo(BigDecimal.valueOf(-Float.MAX_VALUE)) < 0) {
            throw new ArithmeticException("Value exceeds float range");
        }
        return value.floatValue();
    }

    /**
     * 将字节数组转换为双精度浮点
     *
     * @param bytes 待处理字节数组
     * @param order 字节序
     * @param scale 缩放因子
     * @return 双精度浮点
     */
    public static double converterToDouble(byte[] bytes, ByteOrder order, int scale) {
        BigDecimal value = converterToScaledValue(bytes, order, scale);
        if (value.compareTo(BigDecimal.valueOf(Double.MAX_VALUE)) > 0 ||
                value.compareTo(BigDecimal.valueOf(-Double.MAX_VALUE)) < 0) {
            throw new ArithmeticException("Value exceeds double range");
        }
        return value.doubleValue();
    }

    /**
     * 将字节数组转换为 BigDecimal
     *
     * @param bytes 待处理字节数组
     * @param order 字节序
     * @param scale 缩放因子
     * @return BigDecimal
     */
    public static BigDecimal converterToBigDecimal(byte[] bytes, ByteOrder order, int scale) {
        return converterToScaledValue(bytes, order, scale);
    }

    /**
     * 通用缩放值转换核心方法, 根据字节长度转换为对应的整数类型, 再根据 scale 进行缩放转为 BigDecimal
     *
     * @param bytes 原始字节
     * @param order 字节序
     * @param scale 小数位数（缩放因子为10^scale）
     */
    private static BigDecimal converterToScaledValue(byte[] bytes, ByteOrder order, int scale) {
        // 根据字节长度转换为对应的整数类型
        final int length = bytes.length;
        BigInteger unscaled;
        if (length <= Byte.BYTES) {
            unscaled = BigInteger.valueOf(converterToByte(bytes, order));
        } else if (length == Short.BYTES) {
            unscaled = BigInteger.valueOf(converterToShort(bytes, order));
        } else if (length <= Integer.BYTES) {
            unscaled = BigInteger.valueOf(converterToInt(bytes, order));
        } else if (length <= Long.BYTES) {
            unscaled = BigInteger.valueOf(converterToLong(bytes, order));
        } else {
            unscaled = new BigInteger(order == ByteOrder.LITTLE_ENDIAN ?
                    reverseBytes(bytes) : bytes);
        }
        // 根据 scale 还原
        return new BigDecimal(unscaled, scale);
    }

    /**
     * 将字节数组转换为字符串,默认 UTF-8。不需要关注字节序，Charset 会自动处理字节序和字符
     *
     * @param bytes   待处理字节数组
     * @param charset 字符集
     * @return 字符串
     */
    public static String converterToString(byte[] bytes, Charset charset) {
        return new String(bytes, charset).trim();
    }

    /**
     * 将字节数组转换为布尔值
     *
     * @param bytes 待处理字节数组
     * @param order 字节序
     * @return 布尔值
     */
    public static boolean converterToBoolean(byte[] bytes, ByteOrder order) {
        return converterToByte(bytes, order) != 0;
    }
}

