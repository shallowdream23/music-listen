package com.yi.musiclisten.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.yi.musiclisten.utils.TimeUtil;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

/**
 * MyBatis-Plus 自动填充配置
 * - createTime / updateTime 自动填充
 * - deleteTime 逻辑删除使用毫秒级时间戳
 */
@Component
public class AutoFieldFillHandler implements MetaObjectHandler {

    // 插入时填充 createTime、updateTime 和 deleteTime（未删除）
    @Override
    public void insertFill(MetaObject metaObject) {
        long now = TimeUtil.currentTimestampSeconds();
        this.strictInsertFill(metaObject, "createTime", Long.class, now);
        this.strictInsertFill(metaObject, "updateTime", Long.class, now);
        this.strictInsertFill(metaObject, "deleteTime", Long.class, 0L); // 默认未删除
    }

    // 更新时填充 updateTime
    @Override
    public void updateFill(MetaObject metaObject) {
        long now = TimeUtil.currentTimestamp();
        this.strictUpdateFill(metaObject, "updateTime", Long.class, now);
        this.strictUpdateFill(metaObject, "deleteTime", Long.class, now);
    }


    // 逻辑删除时调用

}
