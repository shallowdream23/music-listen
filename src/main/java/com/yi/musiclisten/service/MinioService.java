package com.yi.musiclisten.service;

import com.yi.musiclisten.to.UploadSongRequestDto;
import io.minio.errors.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public interface MinioService {
    void uploadSong(MultipartFile file,UploadSongRequestDto songDto) throws IOException, ServerException, InsufficientDataException, ErrorResponseException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException;
}
