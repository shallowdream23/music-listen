package com.yi.musiclisten.vo;

import lombok.Data;

@Data
public class AlbumUpdateVo {
    /**
     * 歌手ID（user.id）
     */
    private Long singerId;
    /**
     * 专辑名
     */
    private String title;

    /**
     * 封面
     */
    private String coverUrl;

    private String description;
}
