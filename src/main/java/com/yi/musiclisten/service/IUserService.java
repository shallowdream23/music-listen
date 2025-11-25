package com.yi.musiclisten.service;

import com.yi.musiclisten.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yi.musiclisten.to.UserTo;

import java.util.List;

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

    List<UserTo> fomrmatList(List<User> list);

    UserTo fomrmatone(User user);
}
