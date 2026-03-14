package com.yi.musiclisten.vo;

import lombok.Data;

/**
 * 管理端编辑用户信息
 */
@Data
public class AdminUserUpdateVo {
    private Long id;
    private String username;
    private String avatarUrl;
    private String profile;
    /** 状态：0 正常 1 冻结 */
    private Integer status;
}
