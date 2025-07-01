package com.dreamfish.backend.common;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.http.HttpStatus;

/**
 * @author Dream fish
 * @version 1.0
 * @description: 统一返回结果类
 * @date 2025/4/9 15:38
 */
@NoArgsConstructor
@Accessors(chain = true)
@Data
public class Result<T> {
    // HTTP协议状态码: import org.springframework.http.HttpStatus;
    private int status;
    // 业务维度状态码
    private String code;
    // 业务维度状态码描述
    private String message;
    // 返回数据
    private T data;

    /**
     * 构建一个成功结果,不返回额外data
     *
     * @return 成功结果
     */
    public static Result<?> success() {
        return new Result<>()
                .setStatus(HttpStatus.OK.value())
                .setCode(ErrorCode.SUCCESS.getCode())
                .setMessage(ErrorCode.SUCCESS.getMessage());
    }

    /**
     * 构建一个成功结果,返回额外data
     *
     * @param data 额外返回的数据
     * @param <T>  数据类型
     * @return 成功结果
     */
    public static <T> Result<T> success(T data) {
        return new Result<T>()
                .setStatus(HttpStatus.OK.value())
                .setCode(ErrorCode.SUCCESS.getCode())
                .setMessage(ErrorCode.SUCCESS.getMessage())
                .setData(data);
    }

    /**
     * 构建一个失败结果,不返回额外data
     *
     * @param httpStatus HTTP协议状态码
     * @param errorCode  业务维度状态码
     * @return 失败结果
     */
    public static Result<?> error(HttpStatus httpStatus, ErrorCode errorCode) {
        return new Result<>()
                .setStatus(httpStatus.value())
                .setCode(errorCode.getCode())
                .setMessage(errorCode.getMessage());
    }

    /**
     * 构建一个失败结果,返回额外data, 不返回业务维度状态码的描述而使用自定义message
     *
     * @param httpStatus HTTP协议状态码
     * @param errorCode  业务维度状态码
     * @param message    自定义message
     * @return 失败结果
     */
    public static Result<?> error(HttpStatus httpStatus, ErrorCode errorCode, String message) {
        return new Result<>()
                .setStatus(httpStatus.value())
                .setCode(errorCode.getCode())
                .setMessage(message);
    }

    /**
     * 构建一个失败结果,返回额外data
     *
     * @param httpStatus HTTP协议状态码
     * @param errorCode  业务维度状态码
     * @param data       额外返回的数据
     * @param <T>        数据类型
     * @return 失败结果
     */
    public static <T> Result<T> result(HttpStatus httpStatus, ErrorCode errorCode, T data) {
        return new Result<T>()
                .setStatus(httpStatus.value())
                .setCode(errorCode.getCode())
                .setMessage(errorCode.getMessage())
                .setData(data);
    }
}

