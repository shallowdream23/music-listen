package com.yi.musiclisten.to;

import lombok.Data;

@Data
public class SongTo {
    private Long id;

    /**
     * 歌曲名称
     */
    private String title;

    /**
     * 歌手（user.id）
     */
    private Long singerId;

    /**
     * 所属专辑
     */
    private Long albumId;

    /**
     * 音频地址
     */
    private String audioUrl;

    /**
     * 封面
     */
    private String coverUrl;

    /**
     * 风格
     */
    private String style;

    /**
     * 播放量
     */
    private Long playCount;
}
