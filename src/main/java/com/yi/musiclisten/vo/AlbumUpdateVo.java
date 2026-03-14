package com.yi.musiclisten.vo;

import lombok.Data;

@Data
public class AlbumUpdateVo {
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

    /** 审核状态：0 待审核 1 已通过 */
    private Integer status;
}
