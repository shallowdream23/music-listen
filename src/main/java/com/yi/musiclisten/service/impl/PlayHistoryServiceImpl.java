package com.yi.musiclisten.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yi.musiclisten.entity.PlayHistory;
import com.yi.musiclisten.mapper.PlayHistoryMapper;
import com.yi.musiclisten.service.IPlayHistoryService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 播放记录 服务实现类
 * </p>
 *
 * @author zzy
 * @since 2025-11-16
 */
@Service
public class PlayHistoryServiceImpl extends ServiceImpl<PlayHistoryMapper, PlayHistory> implements IPlayHistoryService {

    @Override
    public List<PlayHistory> listByUserId(Long userId) {

        // 1) 查询用户最近 500 条播放记录，按时间倒序
        List<PlayHistory> rawList = list(
                new LambdaQueryWrapper<PlayHistory>()
                        .eq(PlayHistory::getUserId, userId)
                        .orderByDesc(PlayHistory::getUpdateTime)
                        .last("LIMIT 500")
        );

        // 2) 对这 500 条按 songId 去重，只保留最新的（因为已经按时间排序了）
        Map<Long, PlayHistory> map = new LinkedHashMap<>();
        for (PlayHistory item : rawList) {
            // 若 map 中不存在该 songId，则放入（第一次遇到就是最新的）
            map.putIfAbsent(item.getSongId(), item);
        }

        // 3) 最终结果再按时间倒序（以防万一）
        return map.values().stream()
                .sorted(Comparator.comparing(PlayHistory::getUpdateTime).reversed())
                .collect(Collectors.toList());
    }
}
