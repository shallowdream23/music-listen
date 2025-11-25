package com.yi.musiclisten.vo;

import lombok.Data;

@Data
public class UserUpdateVo {
    /**
     * 用户ID
     */
    private Long id;

    /**
     * 账号/昵称
     */
    private String username;

    /**
     * 头像
     */
    private String avatarUrl;

    /**
     * 简介
     */
    private String profile;

}
