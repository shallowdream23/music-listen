package com.yi.musiclisten.utils;

public class EncryptUtils {
    /**
     * 手机脱敏
     * @param text 明文
     * @return 密文
     */
    public static String hidePhone(String text){
        // 显示前三位，后4位，其他隐藏，比如139****9999
        // 截取前半段字符
        StringBuilder sb = new StringBuilder(text.substring(0,3));
        // 用*号隐藏中间字符
        sb.append("*".repeat(text.length() - 7)) ;
        // 截取后半段符
        sb.append(text.substring(text.length() - 4));
        return sb.toString();
    }
}
