package com.yi.musiclisten.service;

import com.yi.musiclisten.entity.Playlist;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yi.musiclisten.to.PlayListTo;

import java.util.List;

/**
 * <p>
 * 歌单表 服务类
 * </p>
 *
 * @author zzy
 * @since 2025-11-16
 */
public interface IPlaylistService extends IService<Playlist> {
    List<PlayListTo> formatlist(List<Playlist> playlist);
}
