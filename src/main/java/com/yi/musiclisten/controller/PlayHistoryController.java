package com.yi.musiclisten.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yi.musiclisten.entity.PlayHistory;
import com.yi.musiclisten.enums.ResponseEnum;
import com.yi.musiclisten.service.IPlayHistoryService;
import com.yi.musiclisten.utils.Result;
import com.yi.musiclisten.utils.TimeUtil;
import com.yi.musiclisten.vo.PlayHistoryVo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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


}
