package com.yi.musiclisten.vo;

import lombok.Data;

@Data
public class RegisterVo {
    private String username;
    private String password;
    private String email;
    private Integer code;
}
