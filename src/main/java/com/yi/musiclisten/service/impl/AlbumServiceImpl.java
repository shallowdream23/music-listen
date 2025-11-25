package com.yi.musiclisten.service.impl;

import com.yi.musiclisten.entity.Album;
import com.yi.musiclisten.mapper.AlbumMapper;
import com.yi.musiclisten.service.IAlbumService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yi.musiclisten.to.AlbumTo;
import com.yi.musiclisten.utils.EntityUtil;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 专辑表 服务实现类
 * </p>
 *
 * @author zzy
 * @since 2025-11-16
 */
@Service
public class AlbumServiceImpl extends ServiceImpl<AlbumMapper, Album> implements IAlbumService {

    @Override
    public List<AlbumTo> formatList(List<Album> list) {
        return list.stream().map(item->{
            AlbumTo albumTo = new AlbumTo();
            EntityUtil.copyProperties(item, albumTo, true);
            return albumTo;
        }).collect(Collectors.toList());
    }
}
