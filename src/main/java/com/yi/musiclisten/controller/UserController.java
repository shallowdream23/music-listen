package com.yi.musiclisten.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.yi.musiclisten.entity.User;
import com.yi.musiclisten.enums.ResponseEnum;
import com.yi.musiclisten.service.IUserService;
import com.yi.musiclisten.utils.EntityUtil;
import com.yi.musiclisten.utils.Result;
import com.yi.musiclisten.vo.UserUpdateVo;
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
    public Result delete(@RequestParam Long id) {
        userService.removeById(id);
        return Result.success(ResponseEnum.SUCCESS);
    }

    /**
     * 冻结用户账号
     *
     * @param userId 用户ID，用于定位需要冻结的具体用户
     * @return boolean 更新操作是否成功执行
     */
    @PostMapping("/freeze")
    public Result freezeUser(@RequestParam Long userId) {
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
     *
     * @param userId 用户ID，用于定位需要冻结的具体用户
     * @return boolean 更新操作是否成功执行
     */
    @PostMapping("/unFreeze")
    public Result unFreezeUser(@RequestParam Long userId) {
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
     *
     * @param id 用户ID
     * @return 用户信息
     */
    @PostMapping("/getUserInfo")
    public Result getUserInfo(@RequestParam Long id) {
        Result.checkParam(id != null, "参数错误");
        User user = userService.getById(id);
        return Result.success(ResponseEnum.SUCCESS, userService.fomrmatone(user));
    }

    /**
     * 修改用户信息
     *
     * @param user 用户信息
     * @return 修改结果
     */
    @PostMapping("/updateUserInfo")
    public Result updateUserInfo(@RequestBody UserUpdateVo user) {
        Result.checkParam(user != null, "参数错误");
        User one = userService.getById(user.getId());
        Result.checkParam(one != null, "用户不存在");
        EntityUtil.copyProperties(user, one, true);
        userService.updateById(one);
        return Result.success(ResponseEnum.SUCCESS);
    }

    /**
     * 获取用户列表
     *
     * @return Result 统一响应结果，包含用户列表数据
     */
    @GetMapping("/getUserList")
    public Result getUserList() {
        // 调用userService获取用户列表，并封装成统一响应格式返回
        return Result.success(ResponseEnum.SUCCESS, userService.fomrmatList(userService.list()));
    }

    /**
     * 根据用户名模糊查询用户列表
     *
     * @param name 用户名关键字，用于模糊匹配
     * @return 统一响应结果，包含查询到的用户列表数据
     */
    @GetMapping("/getUserListLikeName")
    public Result getUserListLikeName(@RequestParam String name) {
        Result.checkParam(name != null, "参数错误");
        // 调用userService获取用户列表，并封装成统一响应格式返回
        return Result.success(ResponseEnum.SUCCESS, userService.fomrmatList(userService.list(new LambdaQueryWrapper<User>()
                .like(User::getUsername, name))));
    }


}
