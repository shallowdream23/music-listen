package com.yi.musiclisten.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yi.musiclisten.entity.Follow;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * <p>
 * 关注表 Mapper 接口
 * </p>
 *
 * @author zzy
 * @since 2025-11-16
 */
public interface FollowMapper extends BaseMapper<Follow> {

    /**
     * 恢复已逻辑删除的关注记录（取消关注后再次关注时用）
     */
    @Update("UPDATE follow SET delete_time = 0 WHERE follower_id = #{followerId} AND followee_id = #{followeeId}")
    int restoreByFollowerAndFollowee(@Param("followerId") Long followerId, @Param("followeeId") Long followeeId);
}
