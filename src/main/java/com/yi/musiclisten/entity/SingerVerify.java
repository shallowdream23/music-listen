package com.yi.musiclisten.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * <p>
 * 歌手审核表
 * </p>
 *
 * @author zzy
 * @since 2025-11-16
 */
@Getter
@Setter
@ToString
@Accessors(chain = true)
@TableName("singer_verify")
public class SingerVerify implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 申请用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 真实姓名/艺名
     */
    @TableField("real_name")
    private String realName;

    /**
     * 申请说明
     */
    @TableField("description")
    private String description;

    /**
     * 审核状态：0待审核 1通过 2拒绝
     */
    @TableField("status")
    private Byte status;

    @TableField("create_time")
    private Long createTime;

    @TableField("update_time")
    private Long updateTime;

    @TableLogic(delval = "UNIX_TIMESTAMP()")
    @TableField("delete_time")
    private Long deleteTime;

    public static final String ID = "id";

    public static final String USER_ID = "user_id";

    public static final String REAL_NAME = "real_name";

    public static final String DESCRIPTION = "description";

    public static final String STATUS = "status";

    public static final String CREATE_TIME = "create_time";

    public static final String UPDATE_TIME = "update_time";

    public static final String DELETE_TIME = "delete_time";
}
