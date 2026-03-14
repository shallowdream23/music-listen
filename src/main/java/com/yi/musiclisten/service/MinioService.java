package com.yi.musiclisten.service;

import com.yi.musiclisten.to.UploadSongRequestDto;
import io.minio.errors.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public interface MinioService {
    /**
     * 上传歌曲。当 currentUserId 不为 null 时，以当前用户为歌手并写入待审核状态；否则按 DTO 歌手名创建/查询歌手（兼容旧调用）。
     */
    void uploadSong(MultipartFile file, UploadSongRequestDto songDto, Long currentUserId) throws IOException, ServerException, InsufficientDataException, ErrorResponseException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException;
}
