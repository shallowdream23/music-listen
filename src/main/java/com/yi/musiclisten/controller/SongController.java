package com.yi.musiclisten.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yi.musiclisten.entity.Song;
import com.yi.musiclisten.entity.User;
import com.yi.musiclisten.enums.ResponseEnum;
import com.yi.musiclisten.service.ISongService;
import com.yi.musiclisten.service.IUserService;
import com.yi.musiclisten.utils.EntityUtil;
import com.yi.musiclisten.utils.Result;
import com.yi.musiclisten.vo.SongUpdateVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 歌曲表 前端控制器
 *
 * 提供基础的 CRUD 接口：
 * - 新增歌曲
 * - 查询歌曲详情
 * - 分页查询歌曲
 * - 更新歌曲
 * - 删除歌曲
 *
 * @author zzy
 * @since 2025-11-16
 */
@RestController
@RequestMapping("/song")
public class SongController {

    @Autowired
    private ISongService songService;

    @Autowired
    private IUserService userService;

    @Value("${app.admin-usernames:admin}")
    private String adminUsernames;

    /**
     * 根据 ID 查询歌曲详情
     *
     * @param id 歌曲 ID
     * @return Result
     */
    @GetMapping("/{id}")
    public Result getSong(@PathVariable Long id) {
        Song song = songService.getById(id);
        return song != null ? Result.success(ResponseEnum.SUCCESS, song) : Result.fail(ResponseEnum.PARAM_IS_INVALID, "未找到该歌曲");
    }

    /**
     * 【后端下载示例】根据歌曲 ID 流式下载音频文件。
     * 后端从歌曲的 audioUrl（如 MinIO）拉取文件流，再写入 response，浏览器会以附件形式保存。
     * 学习要点：流式转发、Content-Disposition、HttpServletResponse 写二进制。
     * 若不需要后端代理下载，可删除此方法，前端继续用 audioUrl 直接下载即可。
     *
     * @param id       歌曲 ID
     * @param response 响应对象，用于设置头并输出流
     */
    @GetMapping("/{id}/download")
    public void downloadSong(@PathVariable Long id, HttpServletResponse response) {
        Song song = songService.getById(id);
        if (song == null || !StringUtils.hasText(song.getAudioUrl())) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String filename = (song.getTitle() != null ? song.getTitle().replaceAll("[/\\\\?*:\"<>|]", "_") : "song") + ".mp3";
        HttpURLConnection conn = null;
        InputStream in = null;
        try {
            URL url = URI.create(song.getAudioUrl()).toURL();
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(60000);
            conn.connect();
            int code = conn.getResponseCode();
            if (code < 200 || code >= 300) {
                response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
                return;
            }
            in = conn.getInputStream();
            response.setContentType("audio/mpeg");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + java.net.URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20") + "\"");
            OutputStream out = response.getOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
            }
            out.flush();
        } catch (Exception e) {
            try {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            } catch (Exception ignored) {}
        } finally {
            if (in != null) {
                try { in.close(); } catch (Exception ignored) {}
            }
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /** 歌曲状态：已上架（仅已上架可被列表/搜索展示并计播放量） */
    private static final int STATUS_APPROVED = 1;

    /**
     * 记录一次播放（播放量 +1），仅已上架歌曲计播放
     */
    @PostMapping("/{id}/play")
    public Result recordPlay(@PathVariable Long id) {
        Song song = songService.getById(id);
        if (song == null) {
            return Result.fail(ResponseEnum.PARAM_IS_INVALID, "未找到该歌曲");
        }
        if (song.getStatus() == null || song.getStatus() != STATUS_APPROVED) {
            return Result.success(ResponseEnum.SUCCESS, song.getPlayCount() == null ? 0 : song.getPlayCount());
        }
        long newCount = song.getPlayCount() == null ? 1 : song.getPlayCount() + 1;
        song.setPlayCount(newCount);
        songService.updateById(song);
        return Result.success(ResponseEnum.SUCCESS, newCount);
    }

    /**
     * 分页查询歌曲列表
     *
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @return Result
     */
    @GetMapping("/list")
    public Result listSongs(@RequestParam(defaultValue = "1") Integer pageNum,
                            @RequestParam(defaultValue = "1") Integer pageSize) {
        LambdaQueryWrapper<Song> wrapper = new LambdaQueryWrapper<Song>().eq(Song::getStatus, STATUS_APPROVED);
        IPage<Song> page = songService.page(new Page<>(pageNum, pageSize), wrapper);
        return Result.success(ResponseEnum.SUCCESS, page);
    }

    /**
     * 根据 ID 更新歌曲
     * @param song 更新数据
     * @return Result
     */
    @PutMapping("/{id}")
    public Result updateSong(@RequestBody SongUpdateVo song) {
        Result.checkParam(song == null, "参数不能为空");
        Song one = songService.getById(song.getId());
        Result.checkParam(one == null, "歌曲不存在");
        if (!canModifySong(one)) {
            return Result.fail(ResponseEnum.FAIL, "无权限修改该歌曲");
        }
        EntityUtil.copyProperties(song, one, true);
        boolean result = songService.updateById(one);
        return result ? Result.success(ResponseEnum.SUCCESS, "更新成功") : Result.fail(ResponseEnum.FAIL, "更新失败");
    }

    /**
     * 根据 ID 删除歌曲（仅歌手本人或管理员可操作）
     */
    @DeleteMapping("/{id}")
    public Result deleteSong(@PathVariable Long id) {
        Result.checkParam(id == null, "参数不能为空");
        Song song = songService.getById(id);
        Result.checkParam(song == null, "歌曲不存在");
        if (!canModifySong(song)) {
            return Result.fail(ResponseEnum.FAIL, "无权限删除该歌曲");
        }
        boolean result = songService.removeById(id);
        return result ? Result.success(ResponseEnum.SUCCESS, "删除成功") : Result.fail(ResponseEnum.FAIL, "删除失败");
    }

    /** 当前用户是否为该歌曲的歌手或管理员 */
    private boolean canModifySong(Song song) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;
        try {
            Long userId = Long.valueOf(auth.getName());
            if (song.getSingerId() != null && song.getSingerId().equals(userId)) return true;
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
     * 根据歌曲风格获取歌曲列表
     * @param style 歌曲风格ID
     * @return 返回符合指定风格的歌曲列表结果
     */
    @GetMapping("/getBystyle")
    public Result getByStyle(@RequestParam Integer style) {
        Result.checkParam(style == null, "参数不能为空");
        return Result.success(ResponseEnum.SUCCESS, songService.list(new LambdaQueryWrapper<Song>()
                .eq(Song::getStatus, STATUS_APPROVED)
                .eq(Song::getStyle, style)));
    }

    /**
     * 根据关键词搜索歌曲（歌名模糊匹配）
     */
    @GetMapping("/search")
    public Result search(@RequestParam(defaultValue = "") String keyword,
                         @RequestParam(defaultValue = "1") Integer pageNum,
                         @RequestParam(defaultValue = "20") Integer pageSize) {
        LambdaQueryWrapper<Song> wrapper = new LambdaQueryWrapper<Song>().eq(Song::getStatus, STATUS_APPROVED);
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.like(Song::getTitle, keyword.trim());
        }
        wrapper.orderByDesc(Song::getPlayCount);
        IPage<Song> page = songService.page(new Page<>(pageNum, pageSize), wrapper);
        return Result.success(ResponseEnum.SUCCESS, page);
    }

    /**
     * 我的上传（当前用户作为歌手的歌曲，含待审核/已上架）
     */
    @GetMapping("/my-uploads")
    public Result myUploads() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Result.checkParam(auth == null, "请先登录");
        Long userId = Long.valueOf(auth.getName());
        List<Song> list = songService.list(new LambdaQueryWrapper<Song>()
                .eq(Song::getSingerId, userId)
                .orderByDesc(Song::getCreateTime));
        return Result.success(ResponseEnum.SUCCESS, list);
    }

    /** 状态：待审核 */
    private static final int STATUS_PENDING = 0;

    /**
     * 审核通过（将歌曲设为已上架）
     */
    @PostMapping("/{id}/approve")
    public Result approve(@PathVariable Long id) {
        Result.checkParam(id == null, "参数错误");
        Song song = songService.getById(id);
        Result.checkParam(song == null, "歌曲不存在");
        song.setStatus(STATUS_APPROVED);
        boolean ok = songService.updateById(song);
        return ok ? Result.success(ResponseEnum.SUCCESS) : Result.fail(ResponseEnum.FAIL, "操作失败");
    }

    /**
     * 下架（将歌曲设为待审核状态）
     */
    @PostMapping("/{id}/offline")
    public Result offline(@PathVariable Long id) {
        Result.checkParam(id == null, "参数错误");
        Song song = songService.getById(id);
        Result.checkParam(song == null, "歌曲不存在");
        song.setStatus(STATUS_PENDING);
        boolean ok = songService.updateById(song);
        return ok ? Result.success(ResponseEnum.SUCCESS) : Result.fail(ResponseEnum.FAIL, "操作失败");
    }

    /**
     * 管理端：分页查询歌曲列表，支持按状态筛选（null=全部，0=待审核，1=已上架）
     */
    @GetMapping("/list-admin")
    public Result listAdmin(@RequestParam(defaultValue = "1") Integer pageNum,
                            @RequestParam(defaultValue = "10") Integer pageSize,
                            @RequestParam(required = false) Integer status,
                            @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<Song> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(Song::getStatus, status);
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.like(Song::getTitle, keyword.trim());
        }
        wrapper.orderByDesc(Song::getCreateTime);
        IPage<Song> page = songService.page(new Page<>(pageNum, pageSize), wrapper);
        return Result.success(ResponseEnum.SUCCESS, page);
    }
}

