package com.yi.musiclisten.vo;

import lombok.Data;

/**
 * 更新歌单入参（名称、封面 URL）
 */
@Data
public class PlaylistUpdateVo {
    private String name;
    private String coverUrl;
}
