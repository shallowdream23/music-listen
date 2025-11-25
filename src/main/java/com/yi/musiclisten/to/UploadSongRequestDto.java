package com.yi.musiclisten.to;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class UploadSongRequestDto {

    @Schema(description = "歌手名")
    private String singerName;

    @Schema(description = "专辑名")
    private String albumName;

    @Schema(description = "风格ID数组")
    private List<Long> styleIds;

    @Schema(description = "歌词")
    private String lyric;

    @Schema(description = "发行年份")
    private Integer year;
}