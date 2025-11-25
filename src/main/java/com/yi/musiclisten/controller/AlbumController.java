package com.yi.musiclisten.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yi.musiclisten.entity.Album;
import com.yi.musiclisten.entity.Song;
import com.yi.musiclisten.enums.ResponseEnum;
import com.yi.musiclisten.service.IAlbumService;
import com.yi.musiclisten.service.ISongService;
import com.yi.musiclisten.to.AlbumTo;
import com.yi.musiclisten.utils.EntityUtil;
import com.yi.musiclisten.utils.Result;
import com.yi.musiclisten.vo.AlbumSaveVo;
import com.yi.musiclisten.vo.AlbumUpdateVo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    private ISongService  songService;

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
        Result.checkParam(id!=null, "参数错误");
        List<AlbumTo> albumTos = albumService.formatList(albumService.list(new LambdaQueryWrapper<Album>()
                .eq(Album::getSingerId, id)));
        return Result.success(ResponseEnum.SUCCESS,albumTos);
    }

        /**
     * 添加专辑信息
     * @param album 专辑保存对象，包含要添加的专辑信息
     * @return 返回操作结果，成功则返回成功状态码，失败则返回失败状态码
     */
    @PostMapping("/add")
    public Result add(@RequestBody AlbumSaveVo album) {
        Result.checkParam(album != null, "参数错误");
        // 创建新的专辑实体对象
        Album album1 = new Album();
        // 将传入的专辑保存对象属性复制到专辑实体对象中
        EntityUtil.copyProperties(album,new Album(),true);
        // 调用服务层保存专辑信息，并根据保存结果返回相应的操作结果
        return albumService.save(album1) ?
                Result.success(ResponseEnum.SUCCESS) : Result.fail(ResponseEnum.FAIL);
    }

    /**
     * 修改专辑信息
     * @param album 专辑保存对象，包含要修改的专辑信息
     * @return 返回操作结果，成功则返回成功状态码，失败则返回失败状态码
     */
    @PostMapping("/update")
    public Result update(@RequestBody AlbumUpdateVo album) {
        Result.checkParam(album != null, "参数错误");
        // 创建新的专辑实体对象
        Album album1 = albumService.getOne(new LambdaQueryWrapper<Album>()
                .eq(Album::getSingerId, album.getSingerId())
                .eq(Album::getTitle, album.getTitle()));
        // 将传入的专辑保存对象属性复制到专辑实体对象中
        EntityUtil.copyProperties(album,album1,true);
        // 调用服务层修改专辑信息，并根据修改结果返回相应的操作结果
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
    @GetMapping
    public Result getSongs(@RequestParam Long  id) {
        // 根据专辑ID查询对应的歌曲列表
        List<Song> list = songService.list(new LambdaQueryWrapper<Song>()
                .eq(Song::getAlbumId, id));
        return Result.success(ResponseEnum.SUCCESS,list);

    }


}
