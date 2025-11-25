package com.yi.musiclisten.to;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;

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
