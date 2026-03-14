package com.yi.musiclisten.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yi.musiclisten.entity.PlayHistory;
import com.yi.musiclisten.entity.Song;
import com.yi.musiclisten.enums.ResponseEnum;
import com.yi.musiclisten.service.IPlayHistoryService;
import com.yi.musiclisten.service.ISongService;
import com.yi.musiclisten.utils.Result;
import com.yi.musiclisten.utils.TimeUtil;
import com.yi.musiclisten.vo.PlayHistoryVo;
import jakarta.annotation.Resource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 播放记录 前端控制器
 * </p>
 *
 * @author zzy
 * @since 2025-11-16
 */
@RestController
@RequestMapping("/play-history")
public class PlayHistoryController {

    @Resource
    private IPlayHistoryService playHistoryService;

    @Resource
    private ISongService songService;

    /**
     * 根据用户ID获取播放历史列表
     *
     * @param id 用户ID
     * @return 包含播放历史列表的统一响应结果
     */
    @RequestMapping("/list")
    public Result list(@RequestParam Long id) {
        // 根据用户ID查询播放历史记录
        List<PlayHistory> playHistories = playHistoryService.listByUserId(id);
        return Result.success(ResponseEnum.SUCCESS, playHistories);
    }

    /**
     * 最近播放列表（带歌曲信息，用于首页展示）
     */
    @GetMapping("/recentWithSongs")
    public Result recentWithSongs(@RequestParam Long id) {
        List<PlayHistory> list = playHistoryService.listByUserId(id);
        List<Map<String, Object>> result = new ArrayList<>();
        for (PlayHistory h : list) {
            Song song = songService.getById(h.getSongId());
            if (song == null) continue;
            Map<String, Object> item = new HashMap<>();
            item.put("song", song);
            item.put("updateTime", h.getUpdateTime());
            result.add(item);
        }
        return Result.success(ResponseEnum.SUCCESS, result);
    }


    /**
     * 添加播放历史记录
     *
     * @param vo 播放历史信息对象，包含用户ID和歌曲ID
     * @return 操作结果，成功时返回成功信息，失败时返回失败信息
     */
    @PostMapping("/add")
    public Result add(@RequestBody PlayHistoryVo vo) {

        // 查询用户是否已存在该歌曲的播放记录
        PlayHistory playHistory = playHistoryService.getOne(new LambdaQueryWrapper<PlayHistory>()
                .eq(PlayHistory::getUserId, vo.getUserId())
                .eq(PlayHistory::getSongId, vo.getSongid()));

        if (playHistory != null) {
            // 更新播放时间
            playHistory.setUpdateTime(TimeUtil.currentTimestampSeconds());
            boolean ok = playHistoryService.updateById(playHistory);
            return ok ? Result.success(ResponseEnum.SUCCESS, "更新成功")
                    : Result.fail(ResponseEnum.FAIL, "更新失败");
        }

        // 新增播放历史记录
        playHistory = new PlayHistory();   // 必须 new
        playHistory.setUserId(vo.getUserId());
        playHistory.setSongId(vo.getSongid());
        playHistory.setCreateTime(TimeUtil.currentTimestampSeconds());
        playHistory.setUpdateTime(TimeUtil.currentTimestampSeconds());

        boolean save = playHistoryService.save(playHistory);
        return save ? Result.success(ResponseEnum.SUCCESS, "添加成功")
                : Result.fail(ResponseEnum.FAIL, "添加失败");
    }

    /**
     * 记录当前登录用户的播放（仅需传歌曲ID，用户从 Token 取）
     */
    @PostMapping("/addCurrent")
    public Result addCurrent(@RequestBody Map<String, Long> body) {
        Long songId = body != null ? body.get("songId") : null;
        Result.checkParam(songId == null, "歌曲ID不能为空");
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            return Result.fail(ResponseEnum.FAIL, "请先登录");
        }
        Long userId = Long.valueOf(auth.getName());
        PlayHistoryVo vo = new PlayHistoryVo();
        vo.setUserId(userId);
        vo.setSongid(songId);
        PlayHistory playHistory = playHistoryService.getOne(new LambdaQueryWrapper<PlayHistory>()
                .eq(PlayHistory::getUserId, userId)
                .eq(PlayHistory::getSongId, songId));
        if (playHistory != null) {
            playHistory.setUpdateTime(TimeUtil.currentTimestampSeconds());
            boolean ok = playHistoryService.updateById(playHistory);
            return ok ? Result.success(ResponseEnum.SUCCESS) : Result.fail(ResponseEnum.FAIL, "更新失败");
        }
        playHistory = new PlayHistory();
        playHistory.setUserId(userId);
        playHistory.setSongId(songId);
        playHistory.setCreateTime(TimeUtil.currentTimestampSeconds());
        playHistory.setUpdateTime(TimeUtil.currentTimestampSeconds());
        boolean save = playHistoryService.save(playHistory);
        return save ? Result.success(ResponseEnum.SUCCESS) : Result.fail(ResponseEnum.FAIL, "添加失败");
    }
}
