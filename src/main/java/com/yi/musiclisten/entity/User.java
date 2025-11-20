package com.yi.musiclisten.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * <p>
 * 用户表
 * </p>
 *
 * @author zzy
 * @since 2025-11-16
 */
@Data
@ToString
@TableName("user")
@Accessors(chain = true)
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 账号/昵称
     */
    @TableField("username")
    private String username;

    /**
     * 密码（加密后）
     */
    @TableField("password")
    private String password;

    /**
     * 头像
     */
    @TableField("avatar_url")
    private String avatarUrl;

    /**
     * 简介
     */
    @TableField("profile")
    private String profile;

    /**
     * 是否歌手：0否 1是
     */
    @TableField("is_singer")
    private Integer isSinger;

    /**
     * 是否冻结：0 否 1是
     */
    @TableField("status")
    private Integer status;

    /**
     * 创建时间（秒）
     */
    @TableField(fill = FieldFill.INSERT)
    private Long createTime;

    /**
     * 更新时间（秒）
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateTime;

    /**
     * 软删除时间，0=未删除
     */
    @TableLogic(delval = "UNIX_TIMESTAMP()")
    @TableField(fill = FieldFill.INSERT)
    private Long deleteTime;

    public static final String ID = "id";

    public static final String USERNAME = "username";

    public static final String PASSWORD = "password";

    public static final String AVATAR_URL = "avatar_url";

    public static final String PROFILE = "profile";

    public static final String IS_SINGER = "is_singer";

    public static final String CREATE_TIME = "create_time";

    public static final String UPDATE_TIME = "update_time";

    public static final String DELETE_TIME = "delete_time";
}
