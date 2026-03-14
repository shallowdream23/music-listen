package com.yi.musiclisten.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yi.musiclisten.entity.Follow;
import com.yi.musiclisten.mapper.FollowMapper;
import com.yi.musiclisten.service.IFollowService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yi.musiclisten.to.followTabCountTo;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 关注表 服务实现类
 * </p>
 *
 * @author zzy
 * @since 2025-11-16
 */
@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {

    @Override
    public followTabCountTo tabCount(Long id) {
        followTabCountTo to = new followTabCountTo();
        // 关注数：该用户关注了多少人（follower_id = id）
        to.setFollowCount(this.count(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getFollowerId, id)));
        // 粉丝数：有多少人关注了该用户（followee_id = id）
        to.setFansCount(this.count(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getFolloweeId, id)));
        return to;
    }
}
