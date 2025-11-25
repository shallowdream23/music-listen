package com.yi.musiclisten.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Data
@Accessors(chain = true)
@TableName("music_song_style")
@Schema(name = "MusicSongStyle", description = "MusicSongStyle 实体")
public class MusicSongStyle implements Serializable {

    private static final long serialVersionUID = 1L;

        /**
         * 主键ID
         */
        @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private Long id;

        /**
         * 歌曲ID
         */
    @Schema(description = "歌曲ID")
    private Long songId;

        /**
         * 风格ID
         */
    @Schema(description = "风格ID")
    private Long styleId;

        /**
         * 删除时间（0 表示未删除，秒级时间戳）
         */
    @Schema(description = "删除时间（0 表示未删除，秒级时间戳）")
    private Long deleteTime;

}
