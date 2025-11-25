package com.yi.musiclisten.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yi.musiclisten.entity.Playlist;
import com.yi.musiclisten.enums.ResponseEnum;
import com.yi.musiclisten.service.IPlaylistService;
import com.yi.musiclisten.to.PlayListTo;
import com.yi.musiclisten.utils.Result;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 歌单表 前端控制器
 * </p>
 *
 * @author zzy
 * @since 2025-11-16
 */
@RestController
@RequestMapping("/playlist")
public class PlaylistController {

    @Resource
    private IPlaylistService playlistService;
    @GetMapping("/list")
    public Result list(@RequestParam("id") Long id) {
        List<Playlist> list = playlistService.list(new LambdaQueryWrapper<Playlist>()
                .eq(Playlist::getUserId, id));
        List<PlayListTo> formatlist = playlistService.formatlist(list);
        return Result.success(ResponseEnum.SUCCESS,formatlist);
    }
}
