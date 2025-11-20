package com.yi.musiclisten.service;

import com.yi.musiclisten.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 用户表 服务类
 * </p>
 *
 * @author zzy
 * @since 2025-11-16
 */
public interface IUserService extends IService<User> {

    User getByUsername(String username);
}
