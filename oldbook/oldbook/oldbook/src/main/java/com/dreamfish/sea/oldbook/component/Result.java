package com.dreamfish.sea.oldbook.component;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: Dream fish
 * @Date: 2023/9/26 18:26
 * @Description: TODO
 * @Version: 1.0
 **/
@Data
@NoArgsConstructor
public class Result<T> {
    private int code; // 状态码
    private String msg; // 状态信息
    private T data; // 数据

    public static <T> Result<T> build(int code, String msg, T data) { // 用于构建Result对象
        Result<T> result = new Result<T>();
        result.setCode(code);
        result.setMsg(msg);
        result.setData(data);
        return result;
    }

    public static <T> Result<T> success(T data) { // 用于构建成功的Result对象
        return build(200, "success", data);
    }

    public static <T> Result<T> success() {  // 用于构建成功的Result对象
        return success(null);
    }

    public static <T> Result<T> error(T data) {  // 用于构建失败的Result对象
        return build(400, "error", data);
    }

    public static <T> Result<T> error() {  // 用于构建失败的Result对象
        return error(null);
    }
}