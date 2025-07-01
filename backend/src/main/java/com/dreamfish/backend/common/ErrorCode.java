package com.dreamfish.backend.common;

import lombok.AllArgsConstructor;
import lombok.Getter;


/**
 * @author Dream fish
 * @version 1.0
 * @description: 错误码枚举类:A开头的为用户,B开头的为系统,C开头的为第三方服务,G开头的为通用
 * @date 2025/4/9 15:39
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {

    // 全局错误码:G
    SUCCESS("G200", "一切 ok"),

    // 用户操作错误码:A
    EMAIL_ADDRESS_INVALID("A411", "邮箱不合法"),
    EMAIL_EXISTS("A412", "邮箱已存在"),
    WORKER_NOT_FIND("A417", "维修人员没有找到"),
    AUTH_TOKEN_INVALID("A406", "凭证无效或者已经过期"),
    AUTH_UNAUTHORIZED("A402", "未认证"),


    USERNAME_EXISTS("A400", "用户名已存在"),
    AUTH_PERMISSION_DENIED("A403", "无访问权限"),
    NAME_OR_PASSWORD_ERROR("A404", "用户名或密码错误"),
    NAME_OR_PASSWORD_NULL("A405", "用户名或密码不能为空"),
    DEVICE_REGISTER_ILLEGAL("A407", "非法设备注册"),
    PARAMETER_ILLEGAL("A408", "参数不合法"),
    DEVICE_NOT_FOUND("A409", "设备不存在"),
    DEVICE_INVALID("A410", "设备无效"),
    VERIFICATION_CODE_INVALID("A413", "验证码已过期"),
    TYPE_NOT_MATCH("A414", "类型不匹配"),
    USER_NOT_FOUND("A415", "用户不存在"),
    DB_OPERATION_FAILED("A416", "数据库操作失败"),

    // 系统错误码:B
    SYSTEM_ERROR("B001", "系统异常"),
    SECRET_CONFIG_ERROR("B101", "密钥配置错误"),
    UNKNOWN_STATUS_CODE("B102", "未知状态码"),
    DB_SELECT_CODE("B103", "数据库查询异常"),
    DB_SAVE_CODE("B104", "数据库保存异常"),
    EMAIL_INFO_ERROR("B105", "邮件信息错误"),
    EMAIL_SEND_CODE("B106", "邮件发送异常"),
    DEVICE_LOCATION_FAILED("B107", "设备地理位置获取失败"),


    // 第三方服务错误码:C
    MAP_SERVICE_ERROR("C001", "地图服务异常"),
    ;

    // 业务错误码
    private final String code;

    // 错误码描述
    private final String message;

}
