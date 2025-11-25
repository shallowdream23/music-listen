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
 * 关注表
 * </p>
 *
 * @author zzy
 * @since 2025-11-16
 */
@Getter
@Setter
@ToString
@TableName("follow")
@Accessors(chain = true)
public class Follow implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 关注发起者
     */
    @TableField("follower_id")
    private Long followerId;

    /**
     * 被关注的人（普通用户 or 歌手）
     */
    @TableField("followee_id")
    private Long followeeId;

    @TableField("create_time")
    private Long createTime;

    @TableLogic(delval = "UNIX_TIMESTAMP()")
    @TableField("delete_time")
    private Long deleteTime;

    public static final String ID = "id";

    public static final String FOLLOWER_ID = "follower_id";

    public static final String FOLLOWEE_ID = "followee_id";

    public static final String CREATE_TIME = "create_time";

    public static final String DELETE_TIME = "delete_time";
}
