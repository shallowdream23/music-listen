package com.yi.musiclisten.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yi.musiclisten.entity.Playlist;
import com.yi.musiclisten.enums.ResponseEnum;
import com.yi.musiclisten.service.IPlaylistService;
import com.yi.musiclisten.to.PlayListTo;
import com.yi.musiclisten.utils.EntityUtil;
import com.yi.musiclisten.utils.Result;
import com.yi.musiclisten.utils.TimeUtil;
import com.yi.musiclisten.vo.PlaylistCollectVo;
import com.yi.musiclisten.vo.PlaylistCreateVo;
import com.yi.musiclisten.vo.PlaylistUpdateVo;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Objects;

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

    @Resource
    private MinioClient minioClient;

    @Value("${minio.endpoint}")
    private String minioEndpoint;

    @Value("${minio.bucket}")
    private String minioBucket;

    /**
     * 推荐歌单：随机返回最多 6 个歌单（供首页展示，不足 6 个则返回实际数量）
     * 注：须放在 /{id} 之前，否则 "recommend" 会被当作 id 解析
     */
    @GetMapping("/recommend")
    public Result recommend(@RequestParam(value = "limit", defaultValue = "6") int limit) {
        int size = Math.min(Math.max(limit, 1), 20);
        List<Playlist> list = playlistService.list(
                new LambdaQueryWrapper<Playlist>().last("ORDER BY RAND() LIMIT " + size));
        List<PlayListTo> formatlist = playlistService.formatlist(list);
        return Result.success(ResponseEnum.SUCCESS, formatlist);
    }

    /**
     * 获取歌单详情（公开歌单可被任何人查看）
     *
     * @param id 歌单ID
     * @return 歌单基本信息
     */
    @GetMapping("/{id}")
    public Result getById(@PathVariable Long id) {
        Result.checkParam(id == null, "参数错误");
        Playlist playlist = playlistService.getById(id);
        Result.checkParam(playlist == null, "歌单不存在");
        PlayListTo to = new PlayListTo();
        EntityUtil.copyProperties(playlist, to, true);
        return Result.success(ResponseEnum.SUCCESS, to);
    }

    /**
     * 获取用户播放列表
     *
     * @param id 用户ID
     * @return 返回该用户的所有播放列表信息
     */
    @GetMapping("/list")
    public Result list(@RequestParam("id") Long id) {
        // 根据用户ID查询对应的播放列表
        List<Playlist> list = playlistService.list(new LambdaQueryWrapper<Playlist>()
                .eq(Playlist::getUserId, id));
        // 对播放列表进行格式化处理
        List<PlayListTo> formatlist = playlistService.formatlist(list);
        return Result.success(ResponseEnum.SUCCESS, formatlist);
    }

    /**
     * 创建歌单（当前登录用户）
     */
    @PostMapping("/create")
    public Result create(@RequestBody PlaylistCreateVo vo) {
        Result.checkParam(vo == null, "参数错误");
        Result.checkParam(vo.getName() == null || vo.getName().trim().isEmpty(), "歌单名称不能为空");

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Result.checkParam(auth == null, "未登录");
        Long userId = Long.valueOf(auth.getName());

        long now = TimeUtil.currentTimestampSeconds();

        Playlist playlist = new Playlist()
                .setUserId(userId)
                .setName(vo.getName().trim())
                .setCoverUrl(vo.getCoverUrl())
                .setType(1) // 1 普通歌单
                .setIsPublic(Boolean.TRUE.equals(vo.getIsPublic()))
                .setCreateTime(now)
                .setUpdateTime(now);

        boolean ok = playlistService.save(playlist);
        return ok ? Result.success(ResponseEnum.SUCCESS) : Result.fail(ResponseEnum.FAIL, "创建歌单失败");
    }

    /**
     * 收藏他人的歌单
     */
    @PostMapping("/collect")
    public Result collect(@RequestBody PlaylistCollectVo vo) {
        Result.checkParam(vo == null && vo.getPlaylistId() != null, "参数错误");

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Result.checkParam(auth == null, "未登录");
        Long userId = Long.valueOf(auth.getName());

        // 原始歌单
        Playlist source = playlistService.getById(vo.getPlaylistId());
        Result.checkParam(source == null, "歌单不存在");

        long now = TimeUtil.currentTimestampSeconds();

        // 为当前用户创建一条 type=2 的收藏歌单记录，复用名称和封面
        Playlist playlist = new Playlist()
                .setUserId(userId)
                .setName(source.getName())
                .setCoverUrl(source.getCoverUrl())
                .setType(2) // 2 收藏歌单
                .setIsPublic(true)
                .setCreateTime(now)
                .setUpdateTime(now);

        boolean ok = playlistService.save(playlist);
        return ok ? Result.success(ResponseEnum.SUCCESS) : Result.fail(ResponseEnum.FAIL, "收藏歌单失败");
    }

    /**
     * 更新歌单信息（仅歌单创建者：名称、封面 URL）
     */
    @PutMapping("/{id}")
    public Result update(@PathVariable Long id, @RequestBody PlaylistUpdateVo vo) {
        Result.checkParam(id == null, "参数错误");
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Result.checkParam(auth == null, "请先登录");
        Long userId = Long.valueOf(auth.getName());

        Playlist playlist = playlistService.getById(id);
        Result.checkParam(playlist == null, "歌单不存在");
        Result.checkParam(!playlist.getUserId().equals(userId), "只能修改自己的歌单");

        if (vo.getName() != null && !vo.getName().trim().isEmpty()) {
            playlist.setName(vo.getName().trim());
        }
        if (vo.getCoverUrl() != null) {
            playlist.setCoverUrl(vo.getCoverUrl().trim().isEmpty() ? null : vo.getCoverUrl().trim());
        }
        playlist.setUpdateTime(TimeUtil.currentTimestampSeconds());
        boolean ok = playlistService.updateById(playlist);
        return ok ? Result.success(ResponseEnum.SUCCESS) : Result.fail(ResponseEnum.FAIL, "更新失败");
    }

    /**
     * 上传歌单封面（仅歌单创建者），返回新封面 URL
     */
    @PostMapping("/{id}/cover")
    public Result uploadCover(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        Result.checkParam(id == null || file == null || file.isEmpty(), "参数错误");
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return Result.fail(ResponseEnum.PARAM_IS_INVALID, "仅支持图片格式");
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Result.checkParam(auth == null, "请先登录");
        Long userId = Long.valueOf(auth.getName());

        Playlist playlist = playlistService.getById(id);
        Result.checkParam(playlist == null, "歌单不存在");
        Result.checkParam(!playlist.getUserId().equals(userId), "只能修改自己的歌单");

        String originalFilename = Objects.requireNonNullElse(file.getOriginalFilename(), "cover");
        String ext = originalFilename.contains(".") ? originalFilename.substring(originalFilename.lastIndexOf(".")) : ".jpg";
        String objectName = "playlist/" + id + "_" + System.currentTimeMillis() + ext;
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioBucket)
                            .object(objectName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(contentType)
                            .build()
            );
        } catch (Exception e) {
            return Result.fail(ResponseEnum.FAIL, "上传失败：" + e.getMessage());
        }
        String url = minioEndpoint + "/" + minioBucket + "/" + objectName;
        playlist.setCoverUrl(url);
        playlist.setUpdateTime(TimeUtil.currentTimestampSeconds());
        playlistService.updateById(playlist);
        return Result.success(ResponseEnum.SUCCESS, url);
    }

}
