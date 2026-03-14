package com.yi.musiclisten.controller;

import com.yi.musiclisten.enums.ResponseEnum;
import com.yi.musiclisten.service.MinioService;
import com.yi.musiclisten.to.UploadSongRequestDto;
import com.yi.musiclisten.utils.Result;
import jakarta.annotation.Resource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class MinioController {

    @Resource
    private MinioService minioService;

    /**
     * 上传歌曲（需登录，以当前用户为歌手，状态为待审核）
     */
    @PostMapping("/upload")
    @Transactional
    public Result upload(@RequestPart("file") MultipartFile file, @RequestPart("songDto") UploadSongRequestDto songDto) throws Exception {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Result.checkParam(auth == null || !auth.isAuthenticated(), "请先登录");
        Long userId = Long.valueOf(auth.getName());
        minioService.uploadSong(file, songDto, userId);
        return Result.success(ResponseEnum.SUCCESS);
    }
}
