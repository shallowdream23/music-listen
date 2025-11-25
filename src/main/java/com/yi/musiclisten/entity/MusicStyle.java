package com.yi.musiclisten.entity;
import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Data
@Accessors(chain = true)
@TableName("music_style")
@Schema(name = "MusicStyle", description = "MusicStyle 实体")
public class MusicStyle implements Serializable {

    private static final long serialVersionUID = 1L;

        /**
         * 风格ID
         */
        @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Schema(description = "风格ID")
    private Long id;

        /**
         * 风格名称
         */
    @Schema(description = "风格名称")
    private String name;

        /**
         * 排序值
         */
    @Schema(description = "排序值")
    private Integer sort;

        /**
         * 状态：1启用 0禁用
         */
    @Schema(description = "状态：1启用 0禁用")
    private Byte status;

        /**
         * 创建时间（毫秒时间戳）
         */
    @Schema(description = "创建时间（毫秒时间戳）")
    private Long createTime;

        /**
         * 更新时间（毫秒时间戳）
         */
    @Schema(description = "更新时间（毫秒时间戳）")
    private Long updateTime;

}
