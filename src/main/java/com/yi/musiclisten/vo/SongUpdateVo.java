package com.yi.musiclisten.vo;

import lombok.Data;

@Data
public class SongUpdateVo {
    private Long id;

    /**
     * 歌曲名称
     */
    private String title;

    /**
     * 所属专辑
     */
    private Long albumId;

    /**
     * 封面
     */
    private String coverUrl;

    /**
     * 风格
     */
    private Integer style;

    /**
     * 歌词（LRC 格式）
     */
    private String lyric;

    /**
     * 上架状态：0 待审核 1 已上架
     */
    private Integer status;
}
