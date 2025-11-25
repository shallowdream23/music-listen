package com.yi.musiclisten.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

@Data
public class AlbumSaveVo {
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
