package com.yi.musiclisten.to;

import lombok.Data;

@Data
public class UserTo {
    /**
     * 用户ID
     */
    private Long id;

    /**
     * 账号/昵称
     */
    private String username;

    /**
     * 性别：1男 2女 0 未知
     */
    private Integer sex;

    /**
     * 头像
     */
    private String avatarUrl;

    /**
     * 简介
     */
    private String profile;

    /**
     * 是否歌手：0否 1是
     */
    private Integer isSinger;
}
