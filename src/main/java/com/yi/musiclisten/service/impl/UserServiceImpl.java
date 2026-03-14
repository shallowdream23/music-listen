package com.yi.musiclisten.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.yi.musiclisten.entity.User;
import com.yi.musiclisten.mapper.UserMapper;
import com.yi.musiclisten.service.IUserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yi.musiclisten.to.UserTo;
import com.yi.musiclisten.utils.EntityUtil;
import com.yi.musiclisten.utils.PasswordUtil;
import com.yi.musiclisten.utils.Result;
import com.yi.musiclisten.vo.PasswordUpdateVo;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 用户表 服务实现类
 * </p>
 *
 * @author zzy
 * @since 2025-11-16
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Resource
    private RedisTemplate redisTemplate;

    @Override
    public User getByUsername(String username) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>().eq(User::getUsername, username);
        return this.getOne(wrapper);
    }

    @Override
    public List<UserTo> fomrmatList(List<User> list) {
        return list.stream().map(item->{
            UserTo userTo = new UserTo();
            EntityUtil.copyProperties(item, userTo, true);
            return userTo;
        }).collect(Collectors.toList());
    }

    @Override
    public UserTo fomrmatone(User user) {
        UserTo userTo = new UserTo();
        EntityUtil.copyProperties(user, userTo, true);
        return userTo;
    }

    @Override
    public void changePassword(PasswordUpdateVo vo) {
        User one = this.getById(vo.getUserId());
        Result.checkParam(!PasswordUtil.matches(vo.getOldPassword(), one.getPassword()), "旧密码错误");
        one.setPassword(PasswordUtil.encrypt(vo.getNewPassword()));

        // 读取 Redis 中的验证码
        String redisKey = "email:code:" + one.getEmail();
        String redisCode = (String) redisTemplate.opsForValue().get(redisKey);
        Result.checkParam(redisCode == null, "验证码已失效");
        Result.checkParam(!redisCode.equals(vo.getCode()), "验证码错误");
        this.updateById(one);
        redisTemplate.delete(redisKey);
    }
}
