package com.yi.musiclisten.to;

import lombok.Data;

@Data
public class AlbumTo {
    private Long id;
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
