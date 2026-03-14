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

    /** 状态：待审核 */
    private static final int SONG_STATUS_PENDING = 0;
    /** 状态：已上架 */
    private static final int SONG_STATUS_APPROVED = 1;

    @Override
    public void uploadSong(MultipartFile file, UploadSongRequestDto songDto, Long currentUserId) throws IOException, ServerException, InsufficientDataException, ErrorResponseException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {

        String objectName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Long singerId;
        if (currentUserId != null) {
            User singer = userService.getById(currentUserId);
            Result.checkParam(singer == null, "用户不存在");
            if (singer.getIsSinger() == null || singer.getIsSinger() == 0) {
                singer.setIsSinger(1);
                userService.updateById(singer);
            }
            singerId = currentUserId;
        } else {
            String singerName = songDto.getSingerName();
            Result.checkParam(singerName == null || singerName.trim().isEmpty(), "歌手名不能为空");
            User singer = userService.getByUsername(singerName.trim());
            if (singer == null) {
                singer = new User()
                        .setUsername(singerName.trim())
                        .setIsSinger(1)
                        .setPassword("000000");
                userService.save(singer);
            }
            singerId = singer.getId();
        }

        Album album = null;
        String albumName = songDto.getAlbumName();
        if (albumName != null && !albumName.trim().isEmpty()) {
            album = albumService.getOne(new LambdaQueryWrapper<Album>()
                    .eq(Album::getSingerId, singerId)
                    .eq(Album::getTitle, albumName.trim()));
            if (album == null) {
                album = new Album()
                        .setTitle(albumName.trim())
                        .setSingerId(singerId)
                        .setCreateTime(System.currentTimeMillis());
                albumService.save(album);
            }
        }
        Long albumId = album != null ? album.getId() : null;

        String permanentUrl = "http://127.0.0.1:9000/music/" + objectName;
        String title = (songDto.getTitle() != null && !songDto.getTitle().trim().isEmpty())
                ? songDto.getTitle().trim() : file.getOriginalFilename();

        Song song = new Song();
        song.setTitle(title);
        song.setAudioUrl(permanentUrl);
        song.setSingerId(singerId);
        song.setAlbumId(albumId);
        song.setCreateTime(System.currentTimeMillis());
        song.setStatus(SONG_STATUS_PENDING);
        song.setStyle(songDto.getStyle() != null ? songDto.getStyle() : 0);
        if (songDto.getLyric() != null && !songDto.getLyric().trim().isEmpty()) {
            song.setLyric(songDto.getLyric().trim());
        }
        if (songDto.getCoverUrl() != null && !songDto.getCoverUrl().trim().isEmpty()) {
            song.setCoverUrl(songDto.getCoverUrl().trim());
        }

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
