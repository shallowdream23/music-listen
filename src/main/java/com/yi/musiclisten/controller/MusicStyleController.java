package com.yi.musiclisten.controller;

import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import com.yi.musiclisten.service.IMusicStyleService;
import com.yi.musiclisten.entity.MusicStyle;

@Tag(name = "歌曲风格分类表接口")
@RestController
@RequestMapping("/musicStyle")
@RequiredArgsConstructor
public class MusicStyleController {

private final IMusicStyleService musicStyleService;

@Operation(summary = "获取歌曲风格分类表列表")
@GetMapping
public Object list() {
    return musicStyleService.list();
}

@Operation(summary = "根据ID查询歌曲风格分类表")
@GetMapping("/{id}")
public Object getById(@Parameter(description = "ID") @PathVariable Long id) {
    return musicStyleService.getById(id);
}

@Operation(summary = "新增歌曲风格分类表")
@PostMapping
public Object save(@RequestBody MusicStyle entity) {
    return musicStyleService.save(entity);
}

@Operation(summary = "修改歌曲风格分类表")
@PutMapping
public Object update(@RequestBody MusicStyle entity) {
    return musicStyleService.updateById(entity);
}

@Operation(summary = "删除歌曲风格分类表")
@DeleteMapping("/{id}")
public Object delete(@Parameter(description = "ID") @PathVariable Long id) {
    return musicStyleService.removeById(id);
}
}