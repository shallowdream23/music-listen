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
 * 播放记录
 * </p>
 *
 * @author zzy
 * @since 2025-11-16
 */
@Getter
@Setter
@ToString
@Accessors(chain = true)
@TableName("play_history")
public class PlayHistory implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 歌曲ID
     */
    @TableField("song_id")
    private Long songId;

    @TableField("create_time")
    private Long createTime;

    @TableLogic(delval = "UNIX_TIMESTAMP()")
    @TableField("delete_time")
    private Long deleteTime;

    public static final String ID = "id";

    public static final String USER_ID = "user_id";

    public static final String SONG_ID = "song_id";

    public static final String CREATE_TIME = "create_time";

    public static final String DELETE_TIME = "delete_time";
}
