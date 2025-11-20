package com.yi.musiclisten.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yi.musiclisten.entity.Album;
import com.yi.musiclisten.entity.Song;
import com.yi.musiclisten.entity.User;
import com.yi.musiclisten.enums.ResponseEnum;
import com.yi.musiclisten.service.IAlbumService;
import com.yi.musiclisten.service.ISongService;
import com.yi.musiclisten.service.IUserService;
import com.yi.musiclisten.service.impl.SongServiceImpl;
import com.yi.musiclisten.utils.Result;
import com.yi.musiclisten.vo.SongVo;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.http.Method;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
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
     /**
     * 上传歌曲
     * @param file 要上传的文件，通过@RequestParam接收前端传递的文件参数
     * @return 返回上传文件的临时访问URL
     * @throws Exception 文件上传或处理过程中可能抛出的异常
     */
    @PostMapping("/upload")
    @Transactional
    public Result upload(@RequestPart("file") MultipartFile file, @RequestPart("SongVo") SongVo songVo) throws Exception {

        //生成 MinIO objectName
        String objectName = System.currentTimeMillis() + "_" + file.getOriginalFilename();

        //查询或创建歌手
        String singerName = songVo.getSingerName();
        if (singerName == null || singerName.trim().isEmpty()) {
            return Result.fail(ResponseEnum.PARAM_IS_INVALID,"歌手名不能为空");
        }

        User singer = userService.getByUsername(singerName.trim());
        if (singer == null) {
            singer = new User()
                    .setUsername(singerName.trim())   // 确保 username 不为空
                    .setIsSinger(1)
                    .setPassword("000000");
            userService.save(singer);
        }
        Long singerId = singer.getId();

        //查询或创建专辑）
        Album album = null;
        String albumName = songVo.getAlbumName();
        if (albumName != null) {
            album = albumService.getOne(new LambdaQueryWrapper<Album>()
                    .eq(Album::getTitle, albumName.trim())
            );
            if (album == null) {
                album = new Album()
                        .setTitle(albumName.trim())
                        .setSingerId(singerId)
                        .setCreateTime(System.currentTimeMillis());
                albumService.save(album);
            }
        }
        Long albumId = album != null ? album.getId() : null;

        //生成永久 URL（public 桶直接拼接）
        String permanentUrl = "http://127.0.0.1:9000/music/" + objectName;

        //保存歌曲
        Song song = new Song();
        song.setTitle(file.getOriginalFilename());
        song.setAudioUrl(permanentUrl);
        song.setSingerId(singerId);
        song.setAlbumId(albumId);
        song.setCreateTime(System.currentTimeMillis());

        songService.save(song);

        //上传文件到 MinIO
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket("music")
                        .object(objectName)
                        .stream(file.getInputStream(), file.getSize(), -1)
                        .contentType(file.getContentType())
                        .build()
        );

        return Result.success(ResponseEnum.SUCCESS);
    }
}
