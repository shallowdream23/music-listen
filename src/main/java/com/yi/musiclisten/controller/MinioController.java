package com.yi.musiclisten.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yi.musiclisten.entity.Album;
import com.yi.musiclisten.entity.Song;
import com.yi.musiclisten.entity.User;
import com.yi.musiclisten.enums.ResponseEnum;
import com.yi.musiclisten.service.IAlbumService;
import com.yi.musiclisten.service.ISongService;
import com.yi.musiclisten.service.IUserService;
import com.yi.musiclisten.service.MinioService;
import com.yi.musiclisten.to.UploadSongRequestDto;
import com.yi.musiclisten.utils.Result;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import jakarta.annotation.Resource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class MinioController {

    @Resource
    private MinioClient minioClient;

    @Resource
    private ISongService songService;

    @Resource
    private IUserService userService;

    @Resource
    private IAlbumService albumService;

    @Resource
    private MinioService minioService;
     /**
     * 上传歌曲
     * @param file 要上传的文件，通过@RequestParam接收前端传递的文件参数
     * @return 返回上传文件的临时访问URL
     * @throws Exception 文件上传或处理过程中可能抛出的异常
     */
    @PostMapping("/upload")
    @Transactional
    public Result upload(@RequestPart("file") MultipartFile file, @RequestPart("songDto") UploadSongRequestDto songDto) throws Exception {
        minioService.uploadSong(file, songDto);
        return Result.success(ResponseEnum.SUCCESS);
    }
}
