package com.yi.musiclisten.enums;

import lombok.Getter;

/**
 * 状态码枚举
 */
@Getter
public enum ResponseEnum {
    /* 成功状态码 */
    SUCCESS(2000, "操作成功"),
    /* 错误状态码 */
    FAIL(1000, "操作失败"),
    /* 参数错误：1001-1999 */
    PARAM_IS_INVALID(1001, "参数无效"),
    PARAM_IS_VALID(1002,"参数检验错误"),
    PARAM_TYPE_BIND_ERROR(1003, "参数格式错误"),
    /* 用户错误：2001-2999*/
    USER_NOT_LOGGED_IN(2001, "用户未登录，请先登录"),
    USER_ACCOUNT_FORBIDDEN(2002, "账号不存在或账号已被禁用"),
    USER_PASSWORD_ERROR(2003, "密码错误"),
    USER_HAS_EXISTED(2004, "用户已存在"),
    /* 权限错误：3001-3999 */
    PERMISSION_UNAUTHORIZED(3001, "权限不足，无权操作"),
    PERMISSION_EXPIRE(3002, "登录状态过期！"),
    PERMISSION_TOKEN_EXPIRED(3003, "token已过期");
    private int code;
    private String msg;

    ResponseEnum(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}
