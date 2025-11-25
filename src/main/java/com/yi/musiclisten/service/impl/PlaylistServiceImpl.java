package com.yi.musiclisten.service.impl;

import com.yi.musiclisten.entity.Playlist;
import com.yi.musiclisten.mapper.PlaylistMapper;
import com.yi.musiclisten.service.IPlaylistService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.yi.musiclisten.to.PlayListTo;
import com.yi.musiclisten.utils.EntityUtil;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 歌单表 服务实现类
 * </p>
 *
 * @author zzy
 * @since 2025-11-16
 */
@Service
public class PlaylistServiceImpl extends ServiceImpl<PlaylistMapper, Playlist> implements IPlaylistService {

    @Override
    public List<PlayListTo> formatlist(List<Playlist> playlist) {
        return playlist.stream().map(item->{
            PlayListTo playListTo = new PlayListTo();
            EntityUtil.copyProperties(item,playListTo,true);
            return playListTo;
        }).collect(Collectors.toList());
    }
}
