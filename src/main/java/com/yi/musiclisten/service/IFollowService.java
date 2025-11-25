package com.yi.musiclisten.service;

import com.yi.musiclisten.entity.Follow;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yi.musiclisten.to.followTabCountTo;

/**
 * <p>
 * 关注表 服务类
 * </p>
 *
 * @author zzy
 * @since 2025-11-16
 */
public interface IFollowService extends IService<Follow> {

    followTabCountTo tabCount(Long id);

}
