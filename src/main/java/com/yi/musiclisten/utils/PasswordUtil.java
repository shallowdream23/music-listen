package com.yi.musiclisten.utils;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 密码加密与校验工具类
 * - 基于 BCrypt，不可逆，高安全性
 */
public class PasswordUtil {

    // 全局单例密码加密器（线程安全）
    private static final PasswordEncoder encoder = new BCryptPasswordEncoder();

    /**
     * 加密明文密码
     * @param rawPassword 明文密码
     * @return 加密后的密码（60位）
     */
    public static String encrypt(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    /**
     * 校验密码是否匹配
     * @param rawPassword 明文密码
     * @param encodedPassword 数据库中的加密密码
     * @return 是否匹配
     */
    public static boolean matches(String rawPassword, String encodedPassword) {
        return encoder.matches(rawPassword, encodedPassword);
    }
}
