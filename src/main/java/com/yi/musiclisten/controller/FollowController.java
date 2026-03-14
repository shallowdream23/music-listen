package com.yi.musiclisten.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yi.musiclisten.entity.Follow;
import com.yi.musiclisten.entity.User;
import com.yi.musiclisten.enums.ResponseEnum;
import com.yi.musiclisten.mapper.FollowMapper;
import com.yi.musiclisten.service.IFollowService;
import com.yi.musiclisten.service.IUserService;
import com.yi.musiclisten.to.FollowWithUserTo;
import com.yi.musiclisten.to.UserTo;
import com.yi.musiclisten.utils.Result;
import com.yi.musiclisten.utils.TimeUtil;
import com.yi.musiclisten.vo.FollowVo;
import jakarta.annotation.Resource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    @Resource
    private FollowMapper followMapper;

    @Resource
    private IUserService userService;

    /**
     * 获取用户的关注列表（含被关注用户基本信息，用于展示与跳转主页）
     */
    @GetMapping("/listFollow")
    public Result listFollow(@RequestParam("id") Long id) {
        Result.checkParam(id == null, "参数错误");
        List<Follow> follows = followService.list(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getFollowerId, id)
                .orderByDesc(Follow::getCreateTime));
        List<FollowWithUserTo> result = buildFollowWithUser(follows, true);
        return Result.success(ResponseEnum.SUCCESS, result);
    }

    /**
     * 获取用户的粉丝列表（含关注发起者基本信息，用于展示与跳转主页）
     */
    @GetMapping("/listFollowee")
    public Result listFollowee(@RequestParam("id") Long id) {
        Result.checkParam(id == null, "参数错误");
        List<Follow> follows = followService.list(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getFolloweeId, id)
                .orderByDesc(Follow::getCreateTime));
        List<FollowWithUserTo> result = buildFollowWithUser(follows, false);
        return Result.success(ResponseEnum.SUCCESS, result);
    }

    /** followee=true 时取 followeeId 对应用户，否则取 followerId 对应用户 */
    private List<FollowWithUserTo> buildFollowWithUser(List<Follow> follows, boolean followee) {
        if (follows == null || follows.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> userIds = follows.stream()
                .map(f -> followee ? f.getFolloweeId() : f.getFollowerId())
                .distinct()
                .collect(Collectors.toList());
        List<User> users = userService.listByIds(userIds);
        Map<Long, UserTo> userMap = users.stream()
                .map(userService::fomrmatone)
                .collect(Collectors.toMap(UserTo::getId, u -> u, (a, b) -> a));
        List<FollowWithUserTo> result = new ArrayList<>(follows.size());
        for (Follow f : follows) {
            Long uid = followee ? f.getFolloweeId() : f.getFollowerId();
            FollowWithUserTo to = new FollowWithUserTo();
            to.setId(f.getId());
            to.setCreateTime(f.getCreateTime());
            to.setUserId(uid);
            UserTo userTo = userMap.get(uid);
            if (userTo == null) {
                userTo = new UserTo();
                userTo.setId(uid);
                userTo.setUsername("(已注销)");
            }
            to.setUser(userTo);
            result.add(to);
        }
        return result;
    }

    /**
     * 获取标签页统计信息
     *
     * @param id 用户ID，用于查询对应的标签页统计数据
     * @return 返回包含标签页统计信息的结果对象，包含成功状态和统计数居
     */
    @GetMapping("/tabCount")
    public Result tabCount(@RequestParam("id") Long id) {
        Result.checkParam(id == null, "参数错误");
        return Result.success(ResponseEnum.SUCCESS, followService.tabCount(id));
    }

    /**
     * 当前登录用户是否已关注目标用户（需登录）
     */
    @GetMapping("/check")
    public Result checkFollowing(@RequestParam("targetId") Long targetId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            Map<String, Boolean> map = new HashMap<>();
            map.put("following", false);
            return Result.success(ResponseEnum.SUCCESS, map);
        }
        Long currentUserId = Long.valueOf(auth.getName());
        long count = followService.count(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getFollowerId, currentUserId)
                .eq(Follow::getFolloweeId, targetId));
        Map<String, Boolean> map = new HashMap<>();
        map.put("following", count > 0);
        return Result.success(ResponseEnum.SUCCESS, map);
    }

    /**
     * 添加关注（需登录，当前用户为关注发起者）
     */
    @PostMapping("/add")
    public Result add(@RequestBody FollowVo vo) {
        Result.checkParam(vo == null, "参数错误");
        Result.checkParam(vo.getFolloweeId() == null, "请指定要关注的用户");
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Result.checkParam(auth == null || !auth.isAuthenticated(), "请先登录");
        Long currentUserId = Long.valueOf(auth.getName());
        Long followeeId = vo.getFolloweeId();
        Result.checkParam(followeeId.equals(currentUserId), "不能关注自己");

        long exists = followService.count(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getFollowerId, currentUserId)
                .eq(Follow::getFolloweeId, followeeId));
        if (exists > 0) {
            return Result.success(ResponseEnum.SUCCESS);
        }

        Follow follow = new Follow();
        follow.setFollowerId(currentUserId);
        follow.setFolloweeId(followeeId);
        follow.setCreateTime(TimeUtil.currentTimestampSeconds());

        try {
            boolean ok = followService.save(follow);
            return ok ? Result.success(ResponseEnum.SUCCESS) : Result.fail(ResponseEnum.FAIL, "关注失败");
        } catch (DuplicateKeyException e) {
            int restored = followMapper.restoreByFollowerAndFollowee(currentUserId, followeeId);
            return restored > 0 ? Result.success(ResponseEnum.SUCCESS) : Result.fail(ResponseEnum.FAIL, "关注失败");
        }
    }

    /**
     * 取消关注（需登录，当前用户为关注发起者）
     */
    @PostMapping("/delete")
    public Result delete(@RequestBody FollowVo vo) {
        Result.checkParam(vo == null || vo.getFolloweeId() == null, "参数错误");
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Result.checkParam(auth == null, "请先登录");
        Long currentUserId = Long.valueOf(auth.getName());
        Long followeeId = vo.getFolloweeId();
        boolean ok = followService.remove(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getFollowerId, currentUserId)
                .eq(Follow::getFolloweeId, followeeId));
        return ok ? Result.success(ResponseEnum.SUCCESS) : Result.fail(ResponseEnum.FAIL, "取消失败");
    }
}
