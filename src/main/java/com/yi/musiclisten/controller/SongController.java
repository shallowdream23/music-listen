package com.yi.musiclisten.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yi.musiclisten.entity.Song;
import com.yi.musiclisten.enums.ResponseEnum;
import com.yi.musiclisten.service.ISongService;
import com.yi.musiclisten.utils.EntityUtil;
import com.yi.musiclisten.utils.Result;
import com.yi.musiclisten.vo.SongUpdateVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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

    /**
     * 根据 ID 查询歌曲详情
     *
     * @param id 歌曲 ID
     * @return Result
     */
    @GetMapping("/{id}")
    public Result getSong(@PathVariable Long id) {
        Song song = songService.getById(id);
        song.setPlayCount(song.getPlayCount() + 1);
        songService.updateById(song);
        return song != null ? Result.success(ResponseEnum.SUCCESS,song) : Result.fail(ResponseEnum.PARAM_IS_INVALID,"未找到该歌曲");
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
                            @RequestParam(defaultValue = "10") Integer pageSize) {
        IPage<Song> page = songService.page(new Page<>(pageNum, pageSize));
        return Result.success(ResponseEnum.SUCCESS,page);
    }

    /**
     * 根据 ID 更新歌曲
     * @param song 更新数据
     * @return Result
     */
    @PutMapping("/{id}")
    public Result updateSong( @RequestBody SongUpdateVo song) {
        Result.checkParam(song != null, "参数不能为空");
        Song one = songService.getById(song.getId());
        EntityUtil.copyProperties(song, one, true);
        boolean result = songService.updateById(one);
        return result ? Result.success(ResponseEnum.SUCCESS,"更新成功") : Result.fail(ResponseEnum.FAIL,"更新失败");
    }

    /**
     * 根据 ID 删除歌曲
     *
     * @param id 歌曲 ID
     * @return Result
     */
    @DeleteMapping("/{id}")
    public Result deleteSong(@PathVariable Long id) {
        Result.checkParam(id != null, "参数不能为空");
        boolean result = songService.removeById(id);
        return result ? Result.success(ResponseEnum.SUCCESS,"删除成功") : Result.fail(ResponseEnum.FAIL,"删除失败");
    }
        /**
     * 根据歌曲风格获取歌曲列表
     * @param style 歌曲风格ID
     * @return 返回符合指定风格的歌曲列表结果
     */
    @GetMapping("/getBystyle")
    public Result getByStyle(@RequestParam Integer style){
        Result.checkParam(style != null, "参数不能为空");
        // 构造查询条件，根据歌曲风格筛选歌曲列表
        return Result.success(ResponseEnum.SUCCESS,songService.list(new LambdaQueryWrapper<Song>().eq(Song::getStyle,style)));
    }

}

