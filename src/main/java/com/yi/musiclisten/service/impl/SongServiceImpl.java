package com.yi.musiclisten.service.impl;

import com.yi.musiclisten.entity.Song;
import com.yi.musiclisten.mapper.SongMapper;
import com.yi.musiclisten.service.ISongService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 歌曲表 服务实现类
 * </p>
 *
 * @author zzy
 * @since 2025-11-16
 */
@Service
public class SongServiceImpl extends ServiceImpl<SongMapper, Song> implements ISongService {

}
