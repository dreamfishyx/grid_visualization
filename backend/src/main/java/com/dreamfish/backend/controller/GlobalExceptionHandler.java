package com.dreamfish.backend.controller;

import com.dreamfish.backend.common.ErrorCode;
import com.dreamfish.backend.common.Result;
import com.dreamfish.backend.exception.*;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;

import java.io.IOException;

/**
 * @author Dream fish
 * @version 1.0
 * @description: 全局异常处理: 控制层反回 Result,其他层直接异常抛出
 * @date 2025/4/9 16:04
 */
@RestControllerAdvice // @ControllerAdvice 的 rest 版本
@Slf4j
public class GlobalExceptionHandler {

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @ExceptionHandler(IOException.class)
    public void handleIoException(IOException ex, HttpServletResponse response) {
        // 识别SSE连接关闭造成的 AsyncRequestNotUsableException
        boolean isSseError = ex.getCause() != null && ex.getCause() instanceof AsyncRequestNotUsableException;

        if (isSseError) {
            log.debug("SSE连接正常关闭: {}", ex.getMessage());
        } else {
            log.error("IO异常: {}", ex.getMessage());
        }
        response.setStatus(HttpStatus.OK.value());
    }

    // 处理参数校验异常
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleValidationException(MethodArgumentNotValidException ex) {
//        HashMap<String, String> errorMap = new HashMap<>();
//        ex.getBindingResult().getFieldErrors().forEach(error -> {
//            log.info("参数校验失败,{}:{}", error.getField(), error.getDefaultMessage());
//            errorMap.put(error.getField(), error.getDefaultMessage());
//        });
        // 返回第一个信息
        FieldError first = ex.getBindingResult().getFieldErrors().getFirst();
        return Result.result(HttpStatus.BAD_REQUEST, ErrorCode.PARAMETER_ILLEGAL, first.getDefaultMessage());
    }

    // 处理自定义参数校验异常
    @ExceptionHandler(ParameterValidationException.class)
    public Result<?> handleParameterValidationException(ParameterValidationException ex) {
        log.warn("参数校验失败: {}", ex.getMessage());
        return Result.error(HttpStatus.BAD_REQUEST, ErrorCode.PARAMETER_ILLEGAL, ex.getMessage());
    }

    // GlobalExceptionHandler.java
    @ExceptionHandler(AuthenticationException.class)
    public Result<?> handleAuthenticationException(AuthenticationException ex) {
        return Result.error(HttpStatus.UNAUTHORIZED, ErrorCode.AUTH_UNAUTHORIZED, "认证失败");
    }

    // 处理业务异常
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BaseException ex) {
        log.warn("业务异常: {}", ex.getMessage());
        return Result.error(HttpStatus.BAD_REQUEST, ex.getErrorCode(), ex.getMessage());
    }

    // 处理设备异常
    @ExceptionHandler(DeviceException.class)
    public Result<?> handleDeviceException(DeviceException e) {
        log.warn("设备异常: {}", e.getMessage());
        return Result.error(HttpStatus.BAD_REQUEST, e.getErrorCode(), e.getMessage());
    }

    // 处理数据库操作异常
    @ExceptionHandler(DbOperationFailedException.class)
    public Result<?> handleDbOperationFailedException(DbOperationFailedException e) {
        log.error("数据库操作异常: {}", e.getMessage(), e);
        return Result.error(HttpStatus.INTERNAL_SERVER_ERROR, e.getErrorCode(), e.getMessage());
    }

    // 处理所有未捕获异常
//    @ExceptionHandler({Exception.class, BaseException.class})
//    public Result<?> handleAll(Exception ex, WebRequest request) {
//        if (isSseRequest(request)) {
//            log.error("SSE请求异常，已忽略响应", ex);
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
//        }
//
//        log.error("系统异常:", ex);
//        // 生产环境不返回详细错误信息,开发环境返回详细错误信息
//        if (isProduction()) {
//            return Result.error(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.SYSTEM_ERROR);
//        } else {
//            // 为 BaseException,使用具体的错误码
//            if (ex instanceof BaseException) {
//                return Result.error(HttpStatus.INTERNAL_SERVER_ERROR, ((BaseException) ex).getErrorCode(), ex.getMessage());
//            }
//            return Result.error(
//                    HttpStatus.INTERNAL_SERVER_ERROR,
//                    ErrorCode.SYSTEM_ERROR,
//                    ex.getMessage()
//            );
//        }
//    }
    // 处理所有未捕获异常(包含sse)
    @ExceptionHandler({Exception.class, BaseException.class})
    public ResponseEntity<?> handleAll(Exception ex, WebRequest request) { // 修改返回类型
//        if (isSseRequest(request)) {
//            log.error("SSE请求异常，已忽略响应", ex);
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
//        }
        Result<?> result;
        log.error("系统异常:", ex);
        if (isProduction()) {
            result = Result.error(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.SYSTEM_ERROR);
        } else {
            if (ex instanceof BaseException) {
                result = Result.error(HttpStatus.INTERNAL_SERVER_ERROR, ((BaseException) ex).getErrorCode(), ex.getMessage());
            } else {
                result = Result.error(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.SYSTEM_ERROR, ex.getMessage());
            }
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
    }

    private boolean isProduction() {
        return "prod".equals(activeProfile);
    }

    private boolean isSseRequest(WebRequest request) {
        if (request instanceof ServletWebRequest servletRequest) {
            // 优先检查请求头的Accept字段
            String acceptHeader = servletRequest.getRequest().getHeader("Accept");
            if (acceptHeader != null && acceptHeader.contains("text/event-stream")) {
                return true;
            }
            // 其次检查响应类型
            if (servletRequest.getResponse() != null) {
                String contentType = servletRequest.getResponse().getContentType();
                return contentType != null && contentType.contains("text/event-stream");
            }
        }
        return false;
    }
}