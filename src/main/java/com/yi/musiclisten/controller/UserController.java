package com.yi.musiclisten.controller;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.yi.musiclisten.entity.User;
import com.yi.musiclisten.enums.ResponseEnum;
import com.yi.musiclisten.service.IUserService;
import com.yi.musiclisten.utils.Result;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 用户表 前端控制器
 * </p>
 *
 * @author zzy
 * @since 2025-11-16
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private IUserService userService;
    /**
    * 删除用户
    */
    @PostMapping("/delete")
    public Result delete(Long id) {
        userService.removeById(id);
        return Result.success(ResponseEnum.SUCCESS);
    }

     /**
     * 冻结用户账号
     * @param userId 用户ID，用于定位需要冻结的具体用户
     * @return boolean 更新操作是否成功执行
     */
     @PostMapping("/freeze")
    public Result freezeUser(Long userId) {
        // 构造更新条件：根据用户ID匹配记录，将用户状态字段设置为0(冻结状态)
         userService.update(
                 null,
                 new LambdaUpdateWrapper<User>()
                         .eq(User::getId, userId)
                         .set(User::getStatus, 1)
         );
        return Result.success(ResponseEnum.SUCCESS);
    }
    /**
     * 解冻用户账号
     * @param userId 用户ID，用于定位需要冻结的具体用户
     * @return boolean 更新操作是否成功执行
     */
    @PostMapping("/unFreeze")
    public Result unFreezeUser(Long userId) {
        // 构造更新条件：根据用户ID匹配记录，将用户状态字段设置为0(冻结状态)
        userService.update(
                null,
                new LambdaUpdateWrapper<User>()
                        .eq(User::getId, userId)
                        .set(User::getStatus, 0)
        );
        return Result.success(ResponseEnum.SUCCESS);
    }
    /**
     * 获取用户信息
     * @param id 用户ID
     * @return 用户信息
     */
    @PostMapping("/getUserInfo")
    public Result getUserInfo(@RequestParam Long id) {
        User user = userService.getById(id);
        return Result.success(ResponseEnum.SUCCESS,user);
    }

    /**
     * 修改用户信息
     * @param user 用户信息
     * @return 修改结果
     */
    @PostMapping("/updateUserInfo")
    public Result updateUserInfo(@RequestBody User user) {
        userService.updateById(user);
        return Result.success(ResponseEnum.SUCCESS);
    }

}
