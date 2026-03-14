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

    @Schema(description = "风格ID（单值，与 styleIds 二选一）")
    private Integer style;

    @Schema(description = "歌曲名称（可选，不填则用文件名）")
    private String title;

    @Schema(description = "封面图片 URL（可选，由前端先上传图片获得）")
    private String coverUrl;

    @Schema(description = "歌词")
    private String lyric;

    @Schema(description = "发行年份")
    private Integer year;
}