package com.yi.musiclisten.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * <p>
 * 歌单表
 * </p>
 *
 * @author zzy
 * @since 2025-11-16
 */
@Getter
@Setter
@ToString
@TableName("playlist")
@Accessors(chain = true)
public class Playlist implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 歌单所属用户
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 歌单名称
     */
    @TableField("name")
    private String name;

    /**
     * 封面
     */
    @TableField("cover_url")
    private String coverUrl;

    /**
     * 歌单类型：1普通 2收藏歌单
     */
    @TableField("type")
    private Integer type;

    /**
     * 是否公开
     */
    @TableField("is_public")
    private Boolean isPublic;

    @TableField("create_time")
    private Long createTime;

    @TableField("update_time")
    private Long updateTime;

    @TableLogic(delval = "UNIX_TIMESTAMP()")
    @TableField("delete_time")
    private Long deleteTime;

    public static final String ID = "id";

    public static final String USER_ID = "user_id";

    public static final String NAME = "name";

    public static final String COVER_URL = "cover_url";

    public static final String TYPE = "type";

    public static final String IS_PUBLIC = "is_public";

    public static final String CREATE_TIME = "create_time";

    public static final String UPDATE_TIME = "update_time";

    public static final String DELETE_TIME = "delete_time";
}
