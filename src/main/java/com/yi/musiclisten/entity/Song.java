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
 * 歌曲表
 * </p>
 *
 * @author zzy
 * @since 2025-11-16
 */
@Getter
@Setter
@ToString
@TableName("song")
@Accessors(chain = true)
public class Song implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 歌曲名称
     */
    @TableField("title")
    private String title;

    /**
     * 歌手（user.id）
     */
    @TableField("singer_id")
    private Long singerId;

    /**
     * 所属专辑
     */
    @TableField("album_id")
    private Long albumId;

    /**
     * 音频地址
     */
    @TableField("audio_url")
    private String audioUrl;

    /**
     * 封面
     */
    @TableField("cover_url")
    private String coverUrl;

    /**
     * 风格
     */
    @TableField("style")
    private String style;

    /**
     * 播放量
     */
    @TableField("play_count")
    private Long playCount;

    @TableField("create_time")
    private Long createTime;

    @TableField("update_time")
    private Long updateTime;

    @TableLogic(delval = "UNIX_TIMESTAMP()")
    @TableField("delete_time")
    private Long deleteTime;

    public static final String ID = "id";

    public static final String TITLE = "title";

    public static final String SINGER_ID = "singer_id";

    public static final String ALBUM_ID = "album_id";

    public static final String AUDIO_URL = "audio_url";

    public static final String COVER_URL = "cover_url";

    public static final String PLAY_COUNT = "play_count";

    public static final String CREATE_TIME = "create_time";

    public static final String UPDATE_TIME = "update_time";

    public static final String DELETE_TIME = "delete_time";
}
