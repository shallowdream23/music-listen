package com.yi.musiclisten.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yi.musiclisten.entity.Playlist;
import com.yi.musiclisten.entity.PlaylistSong;
import com.yi.musiclisten.entity.Song;
import com.yi.musiclisten.enums.ResponseEnum;
import com.yi.musiclisten.service.IPlaylistService;
import com.yi.musiclisten.service.IPlaylistSongService;
import com.yi.musiclisten.service.ISongService;
import com.yi.musiclisten.utils.Result;
import com.yi.musiclisten.utils.TimeUtil;
import jakarta.annotation.Resource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 歌单歌曲表 前端控制器
 * </p>
 *
 * @author zzy
 * @since 2025-11-16
 */
@RestController
@RequestMapping("/playlist-song")
public class PlaylistSongController {

    @Resource
    private IPlaylistSongService playlistSongService;

    @Resource
    private IPlaylistService playlistService;

    @Resource
    private ISongService songService;

    /**
     * 获取歌单下的歌曲列表（按加入顺序），公开接口
     */
    @GetMapping("/list")
    public Result listSongs(@RequestParam("playlistId") Long playlistId) {
        Result.checkParam(playlistId == null, "参数错误");
        List<PlaylistSong> psList = playlistSongService.list(
                new LambdaQueryWrapper<PlaylistSong>()
                        .eq(PlaylistSong::getPlaylistId, playlistId)
                        .orderByAsc(PlaylistSong::getCreateTime));
        if (psList.isEmpty()) {
            return Result.success(ResponseEnum.SUCCESS, new ArrayList<Song>());
        }
        List<Long> songIds = psList.stream().map(PlaylistSong::getSongId).collect(Collectors.toList());
        List<Song> songs = songService.listByIds(songIds);
        Map<Long, Song> idToSong = songs.stream().collect(Collectors.toMap(Song::getId, s -> s));
        List<Song> ordered = new ArrayList<>(psList.size());
        for (Long sid : songIds) {
            Song s = idToSong.get(sid);
            if (s != null) ordered.add(s);
        }
        return Result.success(ResponseEnum.SUCCESS, ordered);
    }

    /**
     * 将歌曲添加到我的歌单（仅可添加到当前用户自己的歌单）
     */
    @PostMapping("/add")
    public Result add(@RequestBody AddSongToPlaylistVo vo) {
        Result.checkParam(vo == null || vo.getPlaylistId() == null || vo.getSongId() == null, "参数错误");
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Result.checkParam(auth == null, "请先登录");
        Long userId = Long.valueOf(auth.getName());

        Playlist playlist = playlistService.getById(vo.getPlaylistId());
        Result.checkParam(playlist == null, "歌单不存在");
        Result.checkParam(!userId.equals(playlist.getUserId()), "只能添加到自己的歌单");

        long count = playlistSongService.count(new LambdaQueryWrapper<PlaylistSong>()
                .eq(PlaylistSong::getPlaylistId, vo.getPlaylistId())
                .eq(PlaylistSong::getSongId, vo.getSongId()));
        Result.checkParam(count > 0, "该歌曲已在歌单中");

        PlaylistSong ps = new PlaylistSong();
        ps.setPlaylistId(vo.getPlaylistId());
        ps.setSongId(vo.getSongId());
        ps.setCreateTime(TimeUtil.currentTimestampSeconds());
        boolean ok = playlistSongService.save(ps);
        return ok ? Result.success(ResponseEnum.SUCCESS) : Result.fail(ResponseEnum.FAIL, "添加失败");
    }

    @lombok.Data
    public static class AddSongToPlaylistVo {
        private Long playlistId;
        private Long songId;
    }
}
