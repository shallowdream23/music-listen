package com.yi.musiclisten.utils;

import java.time.Instant;

/**
 * 时间工具类
 * 提供毫秒级时间戳获取方法
 */
public class TimeUtil {

    /**
     * 获取当前时间的毫秒级时间戳
     *
     * @return 当前时间戳（毫秒）
     */
    public static long currentTimestamp() {
        return Instant.now().toEpochMilli();
    }

    /**
     * 获取当前时间的秒级时间戳
     *
     * @return 当前时间戳（秒）
     */
    public static long currentTimestampSeconds() {
        return Instant.now().getEpochSecond();
    }

    /**
     * 获取指定时间的毫秒时间戳
     *
     * @param instant 指定时间
     * @return 毫秒时间戳
     */
    public static long toMillis(Instant instant) {
        if (instant == null) return 0L;
        return instant.toEpochMilli();
    }

    /**
     * 获取指定时间的秒时间戳
     *
     * @param instant 指定时间
     * @return 秒时间戳
     */
    public static long toSeconds(Instant instant) {
        if (instant == null) return 0L;
        return instant.getEpochSecond();
    }

    // 测试方法
    public static void main(String[] args) {
        System.out.println("当前毫秒时间戳: " + currentTimestamp());
        System.out.println("当前秒时间戳: " + currentTimestampSeconds());
    }
}

