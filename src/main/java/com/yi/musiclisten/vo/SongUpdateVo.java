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
}
