package com.yi.musiclisten.to;

import lombok.Data;

@Data
public class PlayListTo {
    private Long id;

    /**
     * 歌单所属用户
     */
    private Long userId;

    /**
     * 歌单名称
     */
    private String name;

    /**
     * 封面
     */
    private String coverUrl;

    /**
     * 歌单类型：1普通 2收藏歌单
     */
    private Integer type;
}
