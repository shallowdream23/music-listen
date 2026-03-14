package com.yi.musiclisten.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yi.musiclisten.entity.Album;
import com.yi.musiclisten.entity.Song;
import com.yi.musiclisten.entity.User;
import com.yi.musiclisten.enums.ResponseEnum;
import com.yi.musiclisten.service.IAlbumService;
import com.yi.musiclisten.service.ISongService;
import com.yi.musiclisten.service.IUserService;
import com.yi.musiclisten.to.AlbumTo;
import com.yi.musiclisten.utils.EntityUtil;
import com.yi.musiclisten.utils.Result;
import com.yi.musiclisten.vo.AlbumSaveVo;
import com.yi.musiclisten.vo.AlbumUpdateVo;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 专辑表 前端控制器
 * </p>
 *
 * @author zzy
 * @since 2025-11-16
 */
@RestController
@RequestMapping("/album")
public class AlbumController {

    @Resource
    private IAlbumService albumService;

    @Resource
    private ISongService songService;

    @Resource
    private IUserService userService;

    @Value("${app.admin-usernames:admin}")
    private String adminUsernames;

    private static final int ALBUM_STATUS_PENDING = 0;
    private static final int ALBUM_STATUS_APPROVED = 1;

    /**
     * 推荐专辑：随机返回最多 6 个已通过审核的专辑（供首页展示，不足则返回实际数量）
     * 注：须放在带路径变量的接口之前，避免路径冲突
     */
    @GetMapping("/recommend")
    public Result recommend(@RequestParam(value = "limit", defaultValue = "6") int limit) {
        int size = Math.min(Math.max(limit, 1), 20);
        List<Album> list = albumService.list(
                new LambdaQueryWrapper<Album>()
                        .eq(Album::getStatus, ALBUM_STATUS_APPROVED)
                        .last("ORDER BY RAND() LIMIT " + size));
        List<AlbumTo> result = albumService.formatList(list);
        return Result.success(ResponseEnum.SUCCESS, result);
    }

    /**
     * 获取专辑列表
     */
    @GetMapping("/list")
    public Result list() {
        List<AlbumTo> albumTos = albumService.formatList(albumService.list());
        return Result.success(ResponseEnum.SUCCESS,albumTos);
    }

    /**
     * 获取专辑列表
     */
    @GetMapping("/listById")
    public Result listById(@RequestParam Long id) {
        Result.checkParam(id==null, "参数错误");
        List<AlbumTo> albumTos = albumService.formatList(albumService.list(new LambdaQueryWrapper<Album>()
                .eq(Album::getSingerId, id)));
        return Result.success(ResponseEnum.SUCCESS,albumTos);
    }

        /**
     * 添加专辑信息
     * @param album 专辑保存对象，包含要添加的专辑信息
     * @return 返回操作结果，成功则返回成功状态码，失败则返回失败状态码
     */
    /**
     * 歌手创建专辑（需登录且为歌手），状态为待审核
     */
    @PostMapping("/add")
    public Result add(@RequestBody AlbumSaveVo album) {
        Result.checkParam(album == null, "参数错误");
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Result.checkParam(auth == null || !auth.isAuthenticated(), "请先登录");
        Long singerId = Long.valueOf(auth.getName());
        User user = userService.getById(singerId);
        Result.checkParam(user == null || user.getIsSinger() == null || user.getIsSinger() != 1, "仅歌手可创建专辑");
        Album album1 = new Album();
        EntityUtil.copyProperties(album, album1, true);
        album1.setSingerId(singerId);
        album1.setStatus(ALBUM_STATUS_PENDING);
        album1.setCreateTime(System.currentTimeMillis());
        album1.setUpdateTime(System.currentTimeMillis());
        return albumService.save(album1) ?
                Result.success(ResponseEnum.SUCCESS, album1.getId()) : Result.fail(ResponseEnum.FAIL);
    }

    /**
     * 修改专辑信息
     * @param album 专辑保存对象，包含要修改的专辑信息
     * @return 返回操作结果，成功则返回成功状态码，失败则返回失败状态码
     */
    /**
     * 修改专辑（专辑所有者或管理员可修改）
     */
    @PostMapping("/update")
    public Result update(@RequestBody AlbumUpdateVo album) {
        Result.checkParam(album != null && album.getId() != null, "参数错误");
        Album album1 = albumService.getById(album.getId());
        Result.checkParam(album1 == null, "专辑不存在");
        EntityUtil.copyProperties(album, album1, true);
        album1.setUpdateTime(System.currentTimeMillis());
        return albumService.updateById(album1) ?
                Result.success(ResponseEnum.SUCCESS) : Result.fail(ResponseEnum.FAIL);
    }

    /**
     * 删除专辑信息
     * @param id 专辑ID
     * @return 删除结果
     */
    @GetMapping("/delete")
    public Result delete(@RequestParam Long id) {
        Result.checkParam(id != null, "参数错误");
        // 调用服务层删除专辑信息，并根据删除结果返回相应的操作结果
        return albumService.removeById(id) ?
                Result.success(ResponseEnum.SUCCESS) : Result.fail(ResponseEnum.FAIL);
    }

    /**
     * 批量删除专辑信息
     * @param ids 专辑ID列表
     * @return 删除结果
     */
    @GetMapping("/deleteBatch")
    public Result deleteBatch(@RequestBody List<Long> ids) {
        Result.checkParam(ids != null, "参数错误");
        // 调用服务层批量删除专辑信息，并根据删除结果返回相应的操作结果
        return albumService.removeByIds(ids) ?
                Result.success(ResponseEnum.SUCCESS) : Result.fail(ResponseEnum.FAIL);
    }

    /**
     * 获取专辑信息
     * @param id 专辑ID
     * @return 专辑信息
     */
    @GetMapping("/get")
    public Result get(@RequestParam Long id) {
        Result.checkParam(id != null, "参数错误");
        // 调用服务层获取专辑信息，并根据获取结果返回相应的操作结果
        return albumService.getById(id) != null ?
                Result.success(ResponseEnum.SUCCESS,albumService.getById(id)) :
                Result.fail(ResponseEnum.FAIL);
    }
        /**
     * 根据专辑ID获取歌曲列表
     * @param id 专辑ID
     * @return 包含歌曲列表的结果对象
     */
    /** 根据专辑ID获取歌曲列表 */
    @GetMapping(params = "id")
    public Result getSongs(@RequestParam Long id) {
        List<Song> list = songService.list(new LambdaQueryWrapper<Song>()
                .eq(Song::getAlbumId, id)
                .orderByAsc(Song::getCreateTime));
        return Result.success(ResponseEnum.SUCCESS, list);
    }

    /**
     * 专辑详情页：专辑信息 + 歌手名 + 歌曲列表（公开）
     */
    @GetMapping("/detail/{id}")
    public Result detail(@PathVariable Long id) {
        Result.checkParam(id == null, "参数错误");
        Album album = albumService.getById(id);
        if (album == null) return Result.fail(ResponseEnum.PARAM_IS_INVALID, "专辑不存在");
        User singer = userService.getById(album.getSingerId());
        List<Song> songs = songService.list(new LambdaQueryWrapper<Song>()
                .eq(Song::getAlbumId, id)
                .orderByAsc(Song::getCreateTime));
        Map<String, Object> data = new HashMap<>();
        data.put("album", album);
        data.put("singerName", singer != null ? singer.getUsername() : "");
        data.put("songs", songs);
        return Result.success(ResponseEnum.SUCCESS, data);
    }

    /**
     * 将已上传的歌曲加入专辑（仅专辑所属歌手可操作）
     */
    @PostMapping("/{albumId}/songs")
    public Result addSongToAlbum(@PathVariable Long albumId, @RequestBody Map<String, Long> body) {
        Long songId = body != null ? body.get("songId") : null;
        Result.checkParam(albumId == null || songId == null, "参数错误");
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Result.checkParam(auth == null || !auth.isAuthenticated(), "请先登录");
        Long currentUserId = Long.valueOf(auth.getName());
        Album album = albumService.getById(albumId);
        Result.checkParam(album == null, "专辑不存在");
        Result.checkParam(!album.getSingerId().equals(currentUserId), "只能操作自己的专辑");
        Song song = songService.getById(songId);
        Result.checkParam(song == null, "歌曲不存在");
        Result.checkParam(!song.getSingerId().equals(currentUserId), "只能添加自己的歌曲");
        song.setAlbumId(albumId);
        boolean ok = songService.updateById(song);
        return ok ? Result.success(ResponseEnum.SUCCESS) : Result.fail(ResponseEnum.FAIL, "操作失败");
    }

    /**
     * 从专辑中移除歌曲（仅专辑所属歌手可操作）
     */
    @PostMapping("/{albumId}/songs/remove")
    public Result removeSongFromAlbum(@PathVariable Long albumId, @RequestBody Map<String, Long> body) {
        Long songId = body != null ? body.get("songId") : null;
        Result.checkParam(albumId == null || songId == null, "参数错误");
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Result.checkParam(auth == null || !auth.isAuthenticated(), "请先登录");
        Long currentUserId = Long.valueOf(auth.getName());
        Album album = albumService.getById(albumId);
        Result.checkParam(album == null, "专辑不存在");
        Result.checkParam(!album.getSingerId().equals(currentUserId), "只能操作自己的专辑");
        Song song = songService.getById(songId);
        Result.checkParam(song == null || !albumId.equals(song.getAlbumId()), "歌曲不在该专辑中");
        song.setAlbumId(null);
        boolean ok = songService.updateById(song);
        return ok ? Result.success(ResponseEnum.SUCCESS) : Result.fail(ResponseEnum.FAIL, "操作失败");
    }

    private boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;
        try {
            Long userId = Long.valueOf(auth.getName());
            User user = userService.getById(userId);
            if (user == null || !StringUtils.hasText(adminUsernames)) return false;
            return java.util.Arrays.stream(adminUsernames.split(","))
                    .map(String::trim)
                    .anyMatch(name -> name.equalsIgnoreCase(user.getUsername()));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 管理端：分页列表，支持状态、关键词筛选，返回专辑及歌手名
     */
    @GetMapping("/list-admin")
    public Result listAdmin(@RequestParam(defaultValue = "1") Integer pageNum,
                            @RequestParam(defaultValue = "10") Integer pageSize,
                            @RequestParam(required = false) Integer status,
                            @RequestParam(required = false) String keyword) {
        if (!isAdmin()) return Result.fail(ResponseEnum.FAIL, "无权限");
        LambdaQueryWrapper<Album> wrapper = new LambdaQueryWrapper<>();
        if (status != null) wrapper.eq(Album::getStatus, status);
        if (StringUtils.hasText(keyword)) wrapper.like(Album::getTitle, keyword.trim());
        wrapper.orderByDesc(Album::getCreateTime);
        IPage<Album> page = albumService.page(new Page<>(pageNum, pageSize), wrapper);
        List<Long> singerIds = page.getRecords().stream().map(Album::getSingerId).distinct().collect(Collectors.toList());
        Map<Long, String> singerNames = new HashMap<>();
        if (!singerIds.isEmpty()) {
            userService.listByIds(singerIds).forEach(u -> singerNames.put(u.getId(), u.getUsername()));
        }
        List<Map<String, Object>> rows = page.getRecords().stream().map(a -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", a.getId());
            m.put("singerId", a.getSingerId());
            m.put("singerName", singerNames.getOrDefault(a.getSingerId(), ""));
            m.put("title", a.getTitle());
            m.put("coverUrl", a.getCoverUrl());
            m.put("description", a.getDescription());
            m.put("status", a.getStatus());
            m.put("createTime", a.getCreateTime());
            long count = songService.count(new LambdaQueryWrapper<Song>().eq(Song::getAlbumId, a.getId()));
            m.put("songCount", count);
            return m;
        }).collect(Collectors.toList());
        Map<String, Object> result = new HashMap<>();
        result.put("records", rows);
        result.put("total", page.getTotal());
        result.put("size", page.getSize());
        result.put("current", page.getCurrent());
        return Result.success(ResponseEnum.SUCCESS, result);
    }

    /**
     * 管理端：专辑详情（含歌曲列表、歌手名）
     */
    @GetMapping("/admin/{id}")
    public Result adminGet(@PathVariable Long id) {
        if (!isAdmin()) return Result.fail(ResponseEnum.FAIL, "无权限");
        Album album = albumService.getById(id);
        if (album == null) return Result.fail(ResponseEnum.PARAM_IS_INVALID, "专辑不存在");
        User singer = userService.getById(album.getSingerId());
        List<Song> songs = songService.list(new LambdaQueryWrapper<Song>().eq(Song::getAlbumId, id).orderByAsc(Song::getCreateTime));
        Map<String, Object> data = new HashMap<>();
        data.put("album", album);
        data.put("singerName", singer != null ? singer.getUsername() : "");
        data.put("songs", songs);
        return Result.success(ResponseEnum.SUCCESS, data);
    }

    /**
     * 管理端：审核通过
     */
    @PostMapping("/admin/{id}/approve")
    public Result adminApprove(@PathVariable Long id) {
        if (!isAdmin()) return Result.fail(ResponseEnum.FAIL, "无权限");
        Album album = albumService.getById(id);
        Result.checkParam(album == null, "专辑不存在");
        album.setStatus(ALBUM_STATUS_APPROVED);
        album.setUpdateTime(System.currentTimeMillis());
        return albumService.updateById(album) ? Result.success(ResponseEnum.SUCCESS) : Result.fail(ResponseEnum.FAIL, "操作失败");
    }

    /**
     * 管理端：审核驳回（设为待审核）
     */
    @PostMapping("/admin/{id}/reject")
    public Result adminReject(@PathVariable Long id) {
        if (!isAdmin()) return Result.fail(ResponseEnum.FAIL, "无权限");
        Album album = albumService.getById(id);
        Result.checkParam(album == null, "专辑不存在");
        album.setStatus(ALBUM_STATUS_PENDING);
        album.setUpdateTime(System.currentTimeMillis());
        return albumService.updateById(album) ? Result.success(ResponseEnum.SUCCESS) : Result.fail(ResponseEnum.FAIL, "操作失败");
    }

    /**
     * 管理端：更新专辑信息及管理专辑内歌曲（可修改信息、从专辑移除歌曲）
     */
    @PostMapping("/admin/update")
    public Result adminUpdate(@RequestBody AlbumUpdateVo vo) {
        if (!isAdmin()) return Result.fail(ResponseEnum.FAIL, "无权限");
        Result.checkParam(vo == null || vo.getId() == null, "参数错误");
        Album album = albumService.getById(vo.getId());
        Result.checkParam(album == null, "专辑不存在");
        if (vo.getTitle() != null) album.setTitle(vo.getTitle().trim());
        if (vo.getCoverUrl() != null) album.setCoverUrl(vo.getCoverUrl());
        if (vo.getDescription() != null) album.setDescription(vo.getDescription());
        if (vo.getStatus() != null) album.setStatus(vo.getStatus());
        album.setUpdateTime(System.currentTimeMillis());
        return albumService.updateById(album) ? Result.success(ResponseEnum.SUCCESS) : Result.fail(ResponseEnum.FAIL, "更新失败");
    }

    /**
     * 管理端：从专辑中移除歌曲
     */
    @PostMapping("/admin/{albumId}/songs/remove")
    public Result adminRemoveSong(@PathVariable Long albumId, @RequestBody Map<String, Long> body) {
        if (!isAdmin()) return Result.fail(ResponseEnum.FAIL, "无权限");
        Long songId = body != null ? body.get("songId") : null;
        Result.checkParam(albumId == null || songId == null, "参数错误");
        Song song = songService.getById(songId);
        Result.checkParam(song == null || !albumId.equals(song.getAlbumId()), "歌曲不在该专辑中");
        song.setAlbumId(null);
        return songService.updateById(song) ? Result.success(ResponseEnum.SUCCESS) : Result.fail(ResponseEnum.FAIL, "操作失败");
    }

}
