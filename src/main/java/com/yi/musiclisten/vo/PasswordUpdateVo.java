package com.yi.musiclisten.vo;

import lombok.Data;

@Data
public class PasswordUpdateVo {
    private String oldPassword;
    private String newPassword;
    private Long userId;
    private String code;
}
