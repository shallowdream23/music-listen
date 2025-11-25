package com.yi.musiclisten.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yi.musiclisten.entity.Follow;
import com.yi.musiclisten.enums.ResponseEnum;
import com.yi.musiclisten.service.IFollowService;
import com.yi.musiclisten.utils.Result;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 关注表 前端控制器
 * </p>
 *
 * @author zzy
 * @since 2025-11-16
 */
@RestController
@RequestMapping("/follow")
public class FollowController {
    @Resource
    private IFollowService followService;

    /**
     * 获取用户的关注列表
     * @param id 用户ID
     * @return 关注列表结果
     */
    @GetMapping("/listFollow")
    public Result list(@RequestParam("id") Long id) {
        // 查询用户关注列表
        return Result.success(ResponseEnum.SUCCESS,followService.list(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getFollowerId, id)));
    }

    /**
     * 获取用户的粉丝列表
     * @param id 用户ID
     * @return 关注列表结果
     */
    @GetMapping("/listFollowee")
    public Result listFollowee(@RequestParam("id") Long id) {
        // 查询用户粉丝列表
        return Result.success(ResponseEnum.SUCCESS,followService.list(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getFolloweeId, id)));
    }

    @GetMapping("/tabCount")
    public Result tabCount(@RequestParam("id") Long id){
        return Result.success(ResponseEnum.SUCCESS,followService.tabCount(id));
    }

}
