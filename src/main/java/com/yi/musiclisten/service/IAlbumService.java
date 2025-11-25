package com.yi.musiclisten.service;

import com.yi.musiclisten.entity.Album;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yi.musiclisten.to.AlbumTo;

import java.util.List;

/**
 * <p>
 * 专辑表 服务类
 * </p>
 *
 * @author zzy
 * @since 2025-11-16
 */
public interface IAlbumService extends IService<Album> {

   List<AlbumTo> formatList (List<Album> list);

}
