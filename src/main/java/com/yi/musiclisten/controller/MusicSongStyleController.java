package com.yi.musiclisten.controller;

import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import com.yi.musiclisten.service.IMusicSongStyleService;
import com.yi.musiclisten.entity.MusicSongStyle;

@Tag(name = "歌曲风格关联表接口")
@RestController
@RequestMapping("/musicSongStyle")
@RequiredArgsConstructor
public class MusicSongStyleController {

private final IMusicSongStyleService musicSongStyleService;

@Operation(summary = "获取歌曲风格关联表列表")
@GetMapping
public Object list() {
    return musicSongStyleService.list();
}

@Operation(summary = "根据ID查询歌曲风格关联表")
@GetMapping("/{id}")
public Object getById(@Parameter(description = "ID") @PathVariable Long id) {
    return musicSongStyleService.getById(id);
}

@Operation(summary = "新增歌曲风格关联表")
@PostMapping
public Object save(@RequestBody MusicSongStyle entity) {
    return musicSongStyleService.save(entity);
}

@Operation(summary = "修改歌曲风格关联表")
@PutMapping
public Object update(@RequestBody MusicSongStyle entity) {
    return musicSongStyleService.updateById(entity);
}

@Operation(summary = "删除歌曲风格关联表")
@DeleteMapping("/{id}")
public Object delete(@Parameter(description = "ID") @PathVariable Long id) {
    return musicSongStyleService.removeById(id);
}
}