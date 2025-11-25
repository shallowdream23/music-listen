package com.yi.musiclisten.utils;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class EntityUtil {

    private static final Map<String, Map<String, Field>> CACHE = new HashMap<>();

    /**
     * 将 source 中相同属性名的字段复制到 target
     *
     * @param source 原对象
     * @param target 新对象
     * @param ignoreNull 是否忽略 null
     */
    public static void copyProperties(Object source, Object target, boolean ignoreNull) {
        if (source == null || target == null) {
            return;
        }

        Class<?> sourceClass = source.getClass();
        Class<?> targetClass = target.getClass();

        // 获取缓存的字段 map
        Map<String, Field> sourceFieldMap = getFieldMap(sourceClass);
        Map<String, Field> targetFieldMap = getFieldMap(targetClass);

        for (String fieldName : sourceFieldMap.keySet()) {
            Field sourceField = sourceFieldMap.get(fieldName);
            Field targetField = targetFieldMap.get(fieldName);

            // 不存在同名字段直接跳过
            if (targetField == null) {
                continue;
            }

            try {
                Object value = sourceField.get(source);

                // 忽略 null 值
                if (ignoreNull && value == null) {
                    continue;
                }

                // 强制赋值（已 setAccessible）
                targetField.set(target, value);
            } catch (IllegalAccessException ignore) {
            }
        }
    }

    /**
     * 缓存字段（包括父类字段）
     */
    private static Map<String, Field> getFieldMap(Class<?> clazz) {
        String className = clazz.getName();

        if (CACHE.containsKey(className)) {
            return CACHE.get(className);
        }

        Map<String, Field> map = new HashMap<>();
        Class<?> temp = clazz;

        while (temp != null && temp != Object.class) {
            for (Field field : temp.getDeclaredFields()) {
                field.setAccessible(true);
                map.put(field.getName(), field);
            }
            temp = temp.getSuperclass();
        }

        CACHE.put(className, map);
        return map;
    }
}