package com.yi.musiclisten.service;

import com.yi.musiclisten.entity.PlayHistory;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 播放记录 服务类
 * </p>
 *
 * @author zzy
 * @since 2025-11-16
 */
public interface IPlayHistoryService extends IService<PlayHistory> {
    List<PlayHistory> listByUserId(Long userId);

}
