package com.dreamfish.backend.entity.status;

import com.dreamfish.backend.common.ErrorCode;
import com.dreamfish.backend.exception.BaseException;

/**
 * @author Dream fish
 * @version 1.0
 * @description: 通用状态接口, 枚举类没法多继承只好用接口
 * @date 2025/4/12 11:09
 */
public interface BaseStatus {
    int getCode();

    String getDescription();

    /**
     * 根据code获取枚举实例的通用方法
     *
     * @param enumClass 枚举类
     * @param code      状态码
     * @param <E>       枚举类型
     * @return 对应的枚举实例
     * @throws BaseException 如果找不到对应的枚举值
     */
    static <E extends Enum<E> & BaseStatus> E fromCode(Class<E> enumClass, int code) {
        for (E e : enumClass.getEnumConstants()) {
            if (e.getCode() == code) {
                return e;
            }
        }
        throw new BaseException(ErrorCode.UNKNOWN_STATUS_CODE, "未知的状态码: " + code + " for enum " + enumClass.getSimpleName());
    }
}