package com.yi.musiclisten.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yi.musiclisten.entity.User;
import com.yi.musiclisten.enums.ResponseEnum;
import com.yi.musiclisten.service.IUserService;
import com.yi.musiclisten.utils.EntityUtil;
import com.yi.musiclisten.utils.Result;
import com.yi.musiclisten.utils.EmailCodeUtil;
import com.yi.musiclisten.vo.AdminUserUpdateVo;
import com.yi.musiclisten.vo.PasswordUpdateVo;
import com.yi.musiclisten.vo.UserUpdateVo;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * <p>
 * 用户表 前端控制器
 * </p>
 *
 * @author zzy
 * @since 2025-11-16
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private IUserService userService;

    @Resource
    private EmailCodeUtil emailCodeUtil;

    @Resource
    private RedisTemplate redisTemplate;

    @Resource
    private MinioClient minioClient;

    @Value("${minio.endpoint}")
    private String minioEndpoint;

    @Value("${minio.bucket}")
    private String minioBucket;

    @Value("${app.admin-usernames:admin}")
    private String adminUsernames;

    /**
     * 删除用户
     */
    @PostMapping("/delete")
    public Result delete(@RequestParam Long id) {
        userService.removeById(id);
        return Result.success(ResponseEnum.SUCCESS);
    }

    /**
     * 冻结用户账号
     *
     * @param userId 用户ID，用于定位需要冻结的具体用户
     * @return boolean 更新操作是否成功执行
     */
    @PostMapping("/freeze")
    public Result freezeUser(@RequestParam Long userId) {
        // 构造更新条件：根据用户ID匹配记录，将用户状态字段设置为0(冻结状态)
        userService.update(
                null,
                new LambdaUpdateWrapper<User>()
                        .eq(User::getId, userId)
                        .set(User::getStatus, 1)
        );
        return Result.success(ResponseEnum.SUCCESS);
    }

    /**
     * 解冻用户账号
     *
     * @param userId 用户ID，用于定位需要冻结的具体用户
     * @return boolean 更新操作是否成功执行
     */
    @PostMapping("/unFreeze")
    public Result unFreezeUser(@RequestParam Long userId) {
        // 构造更新条件：根据用户ID匹配记录，将用户状态字段设置为0(冻结状态)
        userService.update(
                null,
                new LambdaUpdateWrapper<User>()
                        .eq(User::getId, userId)
                        .set(User::getStatus, 0)
        );
        return Result.success(ResponseEnum.SUCCESS);
    }

    /**
     * 获取用户信息
     *
     * @return 用户信息
     */
    @GetMapping("/getUserInfo")
    public Result getUserInfo() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long id = Long.valueOf(auth.getName());
        Result.checkParam(id == null, "参数错误");
        User user = userService.getById(id);
        var userTo = userService.fomrmatone(user);
        boolean isAdmin = StringUtils.hasText(adminUsernames)
                && java.util.Arrays.stream(adminUsernames.split(","))
                .map(String::trim)
                .anyMatch(name -> name.equalsIgnoreCase(user.getUsername()));
        userTo.setIsAdmin(isAdmin);
        return Result.success(ResponseEnum.SUCCESS, userTo);
    }

    /**
     * 修改用户信息
     *
     * @param user 用户信息
     * @return 修改结果
     */
    @PostMapping("/updateUserInfo")
    public Result updateUserInfo(@RequestBody UserUpdateVo user) {
        Result.checkParam(user == null, "参数错误");
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long currentUserId = Long.valueOf(auth.getName());
        Result.checkParam(!currentUserId.equals(user.getId()), "只能修改本人信息");
        Result.checkParam(userService.getById(user.getId()) == null, "用户不存在");
        LambdaUpdateWrapper<User> wrapper = new LambdaUpdateWrapper<User>()
                .eq(User::getId, user.getId())
                .set(User::getUsername, user.getUsername())
                .set(User::getAvatarUrl, user.getAvatarUrl() != null ? user.getAvatarUrl() : "")
                .set(User::getProfile, user.getProfile() != null ? user.getProfile() : "");
        if (user.getDownloadPath() != null) {
            wrapper.set(User::getDownloadPath, user.getDownloadPath().trim().isEmpty() ? null : user.getDownloadPath().trim());
        }
        boolean ok = userService.update(wrapper);
        return ok ? Result.success(ResponseEnum.SUCCESS) : Result.fail(ResponseEnum.FAIL, "更新失败");
    }

    /**
     * 管理端：编辑用户信息（仅管理员可操作）
     */
    @PostMapping("/admin/update")
    public Result adminUpdateUser(@RequestBody AdminUserUpdateVo vo) {
        Result.checkParam(vo == null || vo.getId() == null, "参数错误");
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = auth != null ? auth.getName() : null;
        boolean isAdmin = StringUtils.hasText(adminUsernames) && currentUsername != null
                && java.util.Arrays.stream(adminUsernames.split(","))
                .map(String::trim)
                .anyMatch(name -> name.equalsIgnoreCase(currentUsername));
        Result.checkParam(!isAdmin, "无权限");
        User target = userService.getById(vo.getId());
        Result.checkParam(target == null, "用户不存在");
        LambdaUpdateWrapper<User> wrapper = new LambdaUpdateWrapper<User>().eq(User::getId, vo.getId());
        if (vo.getUsername() != null) wrapper.set(User::getUsername, vo.getUsername().trim());
        if (vo.getAvatarUrl() != null) wrapper.set(User::getAvatarUrl, vo.getAvatarUrl());
        if (vo.getProfile() != null) wrapper.set(User::getProfile, vo.getProfile());
        if (vo.getStatus() != null) wrapper.set(User::getStatus, vo.getStatus());
        boolean ok = userService.update(wrapper);
        return ok ? Result.success(ResponseEnum.SUCCESS) : Result.fail(ResponseEnum.FAIL, "更新失败");
    }

    /**
     * 获取用户列表
     *
     * @return Result 统一响应结果，包含用户列表数据
     */
    @GetMapping("/getUserList")
    public Result getUserList() {
        // 调用userService获取用户列表，并封装成统一响应格式返回
        return Result.success(ResponseEnum.SUCCESS, userService.fomrmatList(userService.list()));
    }

    /**
     * 管理端：分页查询用户列表，支持用户名模糊搜索
     */
    @GetMapping("/list-admin")
    public Result listAdmin(@RequestParam(defaultValue = "1") Integer pageNum,
                            @RequestParam(defaultValue = "10") Integer pageSize,
                            @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(User::getUsername, keyword.trim());
        }
        wrapper.orderByDesc(User::getCreateTime);
        IPage<User> page = userService.page(new Page<>(pageNum, pageSize), wrapper);
        // 转为 UserTo 并脱敏（不返回密码）
        IPage<com.yi.musiclisten.to.UserTo> toPage = page.convert(user -> {
            var to = userService.fomrmatone(user);
            return to;
        });
        return Result.success(ResponseEnum.SUCCESS, toPage);
    }

    /**
     * 根据用户名模糊查询用户列表
     *
     * @param name 用户名关键字，用于模糊匹配
     * @return 统一响应结果，包含查询到的用户列表数据
     */
    @GetMapping("/getUserListLikeName")
    public Result getUserListLikeName(@RequestParam String name) {
        Result.checkParam(name != null, "参数错误");
        // 调用userService获取用户列表，并封装成统一响应格式返回
        return Result.success(ResponseEnum.SUCCESS, userService.fomrmatList(userService.list(new LambdaQueryWrapper<User>()
                .like(User::getUsername, name))));
    }

    /**
     * 搜索用户（按昵称模糊，公开接口，用于首页搜索栏）
     */
    @GetMapping("/search")
    public Result searchUsers(@RequestParam(defaultValue = "") String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return Result.success(ResponseEnum.SUCCESS, Collections.emptyList());
        }
        var list = userService.fomrmatList(userService.list(
                new LambdaQueryWrapper<User>().like(User::getUsername, keyword.trim())));
        return Result.success(ResponseEnum.SUCCESS, list);
    }

    /**
     * 获取用户公开信息（用于访问他人主页，不包含敏感信息）
     */
    @GetMapping("/{id}/public")
    public Result getPublicUser(@PathVariable Long id) {
        User user = userService.getById(id);
        if (user == null) {
            return Result.fail(ResponseEnum.PARAM_IS_INVALID, "用户不存在");
        }
        return Result.success(ResponseEnum.SUCCESS, userService.fomrmatone(user));
    }

        /**
     * 更新用户密码接口
     * @param user 包含用户密码更新信息的对象
     * @return 返回操作结果，成功时返回SUCCESS状态码
     */
    @PostMapping("/updatePassword")
    public Result updatePassword(@RequestBody PasswordUpdateVo user){
        // 调用服务层执行密码修改操作
        userService.changePassword( user);
        return Result.success(ResponseEnum.SUCCESS);
    }

    /**
     * 发送修改密码验证码（发送到当前登录用户绑定邮箱）
     */
    @GetMapping("/sendPasswordCode")
    public Result sendPasswordCode() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long userId = Long.valueOf(auth.getName());
        User user = userService.getById(userId);
        Result.checkParam(user == null, "用户不存在");
        Result.checkParam(user.getEmail() == null || user.getEmail().trim().isEmpty(), "请先在账号设置中绑定邮箱");
        String code = emailCodeUtil.generateCode();
        emailCodeUtil.sendCodeEmail(user.getEmail().trim(), code);
        String redisKey = "email:code:" + user.getEmail().trim();
        redisTemplate.opsForValue().set(redisKey, code, Duration.ofMinutes(5));
        return Result.success(ResponseEnum.SUCCESS, "验证码已发送，请查收邮箱");
    }

    /**
     * 上传头像图片，返回可访问的 URL
     */
    @PostMapping("/uploadAvatar")
    public Result uploadAvatar(@RequestParam("file") MultipartFile file) {
        Result.checkParam(file == null || file.isEmpty(), "请选择图片");
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return Result.fail(ResponseEnum.PARAM_IS_INVALID, "仅支持图片格式");
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long userId = Long.valueOf(auth.getName());
        String originalFilename = Objects.requireNonNullElse(file.getOriginalFilename(), "avatar");
        String ext = originalFilename.contains(".") ? originalFilename.substring(originalFilename.lastIndexOf(".")) : ".jpg";
        String objectName = "avatar/" + userId + "_" + System.currentTimeMillis() + ext;
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioBucket)
                            .object(objectName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(contentType)
                            .build()
            );
        } catch (Exception e) {
            return Result.fail(ResponseEnum.FAIL, "上传失败：" + e.getMessage());
        }
        String url = minioEndpoint + "/" + minioBucket + "/" + objectName;
        return Result.success(ResponseEnum.SUCCESS, url);
    }

    /**
     * 通用图片上传（专辑封面、歌单封面等），登录后可用，返回图片 URL
     */
    @PostMapping("/uploadImage")
    public Result uploadImage(@RequestParam("file") MultipartFile file) {
        Result.checkParam(file == null || file.isEmpty(), "请选择图片");
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return Result.fail(ResponseEnum.PARAM_IS_INVALID, "仅支持图片格式");
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Result.checkParam(auth == null || !auth.isAuthenticated(), "请先登录");
        Long userId = Long.valueOf(auth.getName());
        String originalFilename = Objects.requireNonNullElse(file.getOriginalFilename(), "image");
        String ext = originalFilename.contains(".") ? originalFilename.substring(originalFilename.lastIndexOf(".")) : ".jpg";
        String objectName = "images/" + userId + "_" + System.currentTimeMillis() + ext;
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioBucket)
                            .object(objectName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(contentType)
                            .build()
            );
        } catch (Exception e) {
            return Result.fail(ResponseEnum.FAIL, "上传失败：" + e.getMessage());
        }
        String url = minioEndpoint + "/" + minioBucket + "/" + objectName;
        return Result.success(ResponseEnum.SUCCESS, url);
    }

    /**
     * 管理端：为指定用户上传头像（仅管理员可操作），上传后更新该用户头像 URL
     */
    @PostMapping("/admin/uploadAvatar")
    public Result adminUploadAvatar(@RequestParam("userId") Long targetUserId, @RequestParam("file") MultipartFile file) {
        Result.checkParam(file == null || file.isEmpty(), "请选择图片");
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return Result.fail(ResponseEnum.PARAM_IS_INVALID, "仅支持图片格式");
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = auth != null ? auth.getName() : null;
        boolean isAdmin = StringUtils.hasText(adminUsernames) && currentUsername != null
                && java.util.Arrays.stream(adminUsernames.split(","))
                .map(String::trim)
                .anyMatch(name -> name.equalsIgnoreCase(currentUsername));
        Result.checkParam(!isAdmin, "无权限");
        User target = userService.getById(targetUserId);
        Result.checkParam(target == null, "用户不存在");
        String originalFilename = Objects.requireNonNullElse(file.getOriginalFilename(), "avatar");
        String ext = originalFilename.contains(".") ? originalFilename.substring(originalFilename.lastIndexOf(".")) : ".jpg";
        String objectName = "avatar/" + targetUserId + "_" + System.currentTimeMillis() + ext;
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioBucket)
                            .object(objectName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(contentType)
                            .build()
            );
        } catch (Exception e) {
            return Result.fail(ResponseEnum.FAIL, "上传失败：" + e.getMessage());
        }
        String url = minioEndpoint + "/" + minioBucket + "/" + objectName;
        target.setAvatarUrl(url);
        userService.updateById(target);
        return Result.success(ResponseEnum.SUCCESS, url);
    }
}
