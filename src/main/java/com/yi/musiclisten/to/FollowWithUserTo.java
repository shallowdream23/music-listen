package com.yi.musiclisten.to;

import lombok.Data;

/**
 * 关注/粉丝列表项：关注记录 + 对应用户基本信息（用于列表展示与跳转主页）
 */
@Data
public class FollowWithUserTo {
    private Long id;
    private Long createTime;
    /** 关注列表中为被关注用户 id，粉丝列表中为关注发起者 id */
    private Long userId;
    private UserTo user;
}
