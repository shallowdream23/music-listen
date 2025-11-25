package com.yi.musiclisten.service.impl;

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
import io.minio.errors.*;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
@Service
public class MinioServiceImpl implements MinioService {

    @Resource
    private ISongService songService;

    @Resource
    private IUserService userService;

    @Resource
    private IAlbumService albumService;

    @Resource
    private MinioClient minioClient;

    @Override
    public void uploadSong(MultipartFile file, UploadSongRequestDto songDto) throws IOException, ServerException, InsufficientDataException, ErrorResponseException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {

        //生成 MinIO objectName
        String objectName = System.currentTimeMillis() + "_" + file.getOriginalFilename();

        //查询或创建歌手
        String singerName = songDto.getSingerName();
        Result.checkParam(singerName == null || singerName.trim().isEmpty(), "歌手名不能为空");

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
        String albumName = songDto.getAlbumName();
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
    }
}
