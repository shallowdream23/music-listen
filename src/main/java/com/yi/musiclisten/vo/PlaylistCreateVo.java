package com.yi.musiclisten.vo;

import lombok.Data;

/**
 * 创建歌单入参
 */
@Data
public class PlaylistCreateVo {

    /**
     * 歌单名称
     */
    private String name;

    /**
     * 封面地址（可选）
     */
    private String coverUrl;

    /**
     * 是否公开：true 公开，false 私人
     */
    private Boolean isPublic;
}

