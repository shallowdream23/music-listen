package com.yi.musiclisten.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yi.musiclisten.entity.Follow;
import com.yi.musiclisten.enums.ResponseEnum;
import com.yi.musiclisten.service.IFollowService;
import com.yi.musiclisten.utils.EntityUtil;
import com.yi.musiclisten.utils.Result;
import com.yi.musiclisten.vo.FollowVo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

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
     *
     * @param id 用户ID
     * @return 关注列表结果
     */
    @GetMapping("/listFollow")
    public Result list(@RequestParam("id") Long id) {
        Result.checkParam(id != null, "参数错误");
        // 查询用户关注列表
        return Result.success(ResponseEnum.SUCCESS, followService.list(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getFollowerId, id)));
    }

    /**
     * 获取用户的粉丝列表
     *
     * @param id 用户ID
     * @return 关注列表结果
     */
    @GetMapping("/listFollowee")
    public Result listFollowee(@RequestParam("id") Long id) {
        Result.checkParam(id != null, "参数错误");
        // 查询用户粉丝列表
        return Result.success(ResponseEnum.SUCCESS, followService.list(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getFolloweeId, id)));
    }

    /**
     * 获取标签页统计信息
     *
     * @param id 用户ID，用于查询对应的标签页统计数据
     * @return 返回包含标签页统计信息的结果对象，包含成功状态和统计数居
     */
    @GetMapping("/tabCount")
    public Result tabCount(@RequestParam("id") Long id) {
        Result.checkParam(id != null, "参数错误");
        return Result.success(ResponseEnum.SUCCESS, followService.tabCount(id));
    }


    /**
     * 添加关注信息
     *
     * @param vo 关注信息传输对象，包含需要添加的关注数据
     * @return 操作结果，成功时返回SUCCESS，失败时返回FAIL
     */
    @PostMapping("/add")
    public Result add(@RequestBody FollowVo vo) {
        Result.checkParam(vo != null, "参数错误");
        // 创建新的关注实体对象
        Follow follow = new Follow();
        EntityUtil.copyProperties(vo, follow, true);
        // 调用服务层保存关注信息，并根据保存结果返回相应的操作结果
        return followService.save(follow) ?
                Result.success(ResponseEnum.SUCCESS) : Result.fail(ResponseEnum.FAIL);
    }

    /**
     * 取消关注信息
     *
     * @return 操作结果，成功时返回SUCCESS，失败时返回FAIL
     */
    @PostMapping("/delete")
    public Result delete(@RequestBody FollowVo vo) {
        Result.checkParam(vo != null, "参数错误");
        // 调用服务层删除关注信息，并根据删除结果返回相应的操作结果
        return followService.remove(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getFollowerId, vo.getFollowerId())
                .eq(Follow::getFolloweeId, vo.getFolloweeId())) ?
                Result.success(ResponseEnum.SUCCESS) : Result.fail(ResponseEnum.FAIL);
    }
}
