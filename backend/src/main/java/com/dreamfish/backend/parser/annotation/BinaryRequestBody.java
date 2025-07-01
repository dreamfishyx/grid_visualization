package com.dreamfish.backend.parser.annotation;

import com.dreamfish.backend.parser.group.Default;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Dream fish
 * @version 1.0
 * @description: 参数注解，标注请求体为二进制数据
 * @date 2025/4/3 16:14
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface BinaryRequestBody {
    /**
     * 字节序列化顺序:BIG_ENDIAN 或 LITTLE_ENDIAN
     *
     * @return int
     */
    ByteOrder byteOrder() default ByteOrder.BIG_ENDIAN;

    /**
     * 指定校验的分组,默认分组为 Default.class
     *
     * @return Class<?>
     */
    Class<?> group() default Default.class;

    // int length() default -1; pass(懒得考虑)
    enum ByteOrder {
        BIG_ENDIAN,
        LITTLE_ENDIAN
    }
}