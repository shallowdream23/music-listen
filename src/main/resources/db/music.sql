/*
 Navicat Premium Dump SQL

 Source Server         : linux
 Source Server Type    : MySQL
 Source Server Version : 90200 (9.2.0)
 Source Host           : 192.168.128.130:3306
 Source Schema         : music

 Target Server Type    : MySQL
 Target Server Version : 90200 (9.2.0)
 File Encoding         : 65001

 Date: 13/03/2026 12:10:35
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for album
-- ----------------------------
DROP TABLE IF EXISTS `album`;
CREATE TABLE `album`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `singer_id` bigint UNSIGNED NOT NULL COMMENT '歌手ID（user.id）',
  `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '专辑名',
  `cover_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '封面',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `create_time` bigint UNSIGNED NOT NULL DEFAULT 0,
  `update_time` bigint UNSIGNED NOT NULL DEFAULT 0,
  `delete_time` bigint UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `singer_id`(`singer_id` ASC) USING BTREE,
  CONSTRAINT `album_ibfk_1` FOREIGN KEY (`singer_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '专辑表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for follow
-- ----------------------------
DROP TABLE IF EXISTS `follow`;
CREATE TABLE `follow`  (
  `id` bigint UNSIGNED NOT NULL,
  `follower_id` bigint UNSIGNED NOT NULL COMMENT '关注发起者',
  `followee_id` bigint UNSIGNED NOT NULL COMMENT '被关注的人（普通用户 or 歌手）',
  `create_time` bigint UNSIGNED NOT NULL DEFAULT 0,
  `delete_time` bigint UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_follower_followee`(`follower_id` ASC, `followee_id` ASC) USING BTREE,
  INDEX `followee_id`(`followee_id` ASC) USING BTREE,
  CONSTRAINT `follow_ibfk_1` FOREIGN KEY (`follower_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `follow_ibfk_2` FOREIGN KEY (`followee_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '关注表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for music_song_style
-- ----------------------------
DROP TABLE IF EXISTS `music_song_style`;
CREATE TABLE `music_song_style`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `song_id` bigint UNSIGNED NOT NULL COMMENT '歌曲ID',
  `style_id` bigint UNSIGNED NOT NULL COMMENT '风格ID',
  `delete_time` bigint NOT NULL DEFAULT 0 COMMENT '删除时间（0 表示未删除，秒级时间戳）',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uniq_song_style`(`song_id` ASC, `style_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_bin COMMENT = '歌曲风格关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for music_style
-- ----------------------------
DROP TABLE IF EXISTS `music_style`;
CREATE TABLE `music_style`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '风格ID',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin NOT NULL COMMENT '风格名称',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序值',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1启用 0禁用',
  `create_time` bigint NOT NULL COMMENT '创建时间（毫秒时间戳）',
  `update_time` bigint NOT NULL COMMENT '更新时间（毫秒时间戳）',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_bin COMMENT = '歌曲风格分类表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for play_history
-- ----------------------------
DROP TABLE IF EXISTS `play_history`;
CREATE TABLE `play_history`  (
  `id` bigint UNSIGNED NOT NULL,
  `user_id` bigint UNSIGNED NOT NULL COMMENT '用户ID',
  `song_id` bigint UNSIGNED NOT NULL COMMENT '歌曲ID',
  `create_time` bigint UNSIGNED NOT NULL DEFAULT 0,
  `update_time` bigint NOT NULL,
  `delete_time` bigint UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `user_id`(`user_id` ASC) USING BTREE,
  INDEX `song_id`(`song_id` ASC) USING BTREE,
  CONSTRAINT `play_history_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `play_history_ibfk_2` FOREIGN KEY (`song_id`) REFERENCES `song` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '播放记录' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for playlist
-- ----------------------------
DROP TABLE IF EXISTS `playlist`;
CREATE TABLE `playlist`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` bigint UNSIGNED NOT NULL COMMENT '歌单所属用户',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '歌单名称',
  `cover_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '封面',
  `type` tinyint NOT NULL DEFAULT 1 COMMENT '歌单类型：1普通 2收藏歌单',
  `is_public` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否公开',
  `create_time` bigint UNSIGNED NOT NULL DEFAULT 0,
  `update_time` bigint UNSIGNED NOT NULL DEFAULT 0,
  `delete_time` bigint UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `user_id`(`user_id` ASC) USING BTREE,
  CONSTRAINT `playlist_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '歌单表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for playlist_song
-- ----------------------------
DROP TABLE IF EXISTS `playlist_song`;
CREATE TABLE `playlist_song`  (
  `id` bigint UNSIGNED NOT NULL,
  `playlist_id` bigint UNSIGNED NOT NULL COMMENT '歌单ID',
  `song_id` bigint UNSIGNED NOT NULL COMMENT '歌曲ID',
  `create_time` bigint UNSIGNED NOT NULL DEFAULT 0,
  `delete_time` bigint UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_playlist_song`(`playlist_id` ASC, `song_id` ASC) USING BTREE,
  INDEX `song_id`(`song_id` ASC) USING BTREE,
  CONSTRAINT `playlist_song_ibfk_1` FOREIGN KEY (`playlist_id`) REFERENCES `playlist` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `playlist_song_ibfk_2` FOREIGN KEY (`song_id`) REFERENCES `song` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '歌单歌曲表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for singer_verify
-- ----------------------------
DROP TABLE IF EXISTS `singer_verify`;
CREATE TABLE `singer_verify`  (
  `id` bigint UNSIGNED NOT NULL,
  `user_id` bigint UNSIGNED NOT NULL COMMENT '申请用户ID',
  `real_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '真实姓名/艺名',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '申请说明',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '审核状态：0待审核 1通过 2拒绝',
  `create_time` bigint UNSIGNED NOT NULL DEFAULT 0,
  `update_time` bigint UNSIGNED NOT NULL DEFAULT 0,
  `delete_time` bigint UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `user_id`(`user_id` ASC) USING BTREE,
  CONSTRAINT `singer_verify_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '歌手审核表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for song
-- ----------------------------
DROP TABLE IF EXISTS `song`;
CREATE TABLE `song`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '歌曲名称',
  `singer_id` bigint UNSIGNED NOT NULL COMMENT '歌手（user.id）',
  `album_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT '所属专辑',
  `audio_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '音频地址',
  `cover_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '封面',
  `style` int NOT NULL COMMENT '风格',
  `play_count` bigint UNSIGNED NOT NULL DEFAULT 0 COMMENT '播放量',
  `create_time` bigint UNSIGNED NOT NULL DEFAULT 0,
  `update_time` bigint UNSIGNED NOT NULL DEFAULT 0,
  `delete_time` bigint UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `singer_id`(`singer_id` ASC) USING BTREE,
  INDEX `album_id`(`album_id` ASC) USING BTREE,
  CONSTRAINT `song_ibfk_1` FOREIGN KEY (`singer_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `song_ibfk_2` FOREIGN KEY (`album_id`) REFERENCES `album` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '歌曲表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for tb_admin
-- ----------------------------
DROP TABLE IF EXISTS `tb_admin`;
CREATE TABLE `tb_admin`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '管理员 id',
  `username` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '管理员用户名',
  `password` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '管理员密码',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `username`(`username` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 171 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for tb_artist
-- ----------------------------
DROP TABLE IF EXISTS `tb_artist`;
CREATE TABLE `tb_artist`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '歌手 id',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '歌手姓名',
  `gender` int NULL DEFAULT NULL COMMENT '歌手性别：0-男，1-女，2-组合/乐队',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '歌手头像',
  `birth` date NULL DEFAULT NULL COMMENT '歌手出生日期',
  `area` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '歌手国籍',
  `introduction` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '歌手简介',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `name`(`name` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 142 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for tb_banner
-- ----------------------------
DROP TABLE IF EXISTS `tb_banner`;
CREATE TABLE `tb_banner`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '轮播图 id',
  `banner_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '轮播图 url',
  `status` tinyint NOT NULL COMMENT '轮播图状态：0-启用，1-禁用',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 31 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for tb_comment
-- ----------------------------
DROP TABLE IF EXISTS `tb_comment`;
CREATE TABLE `tb_comment`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '评论 id',
  `user_id` bigint NOT NULL COMMENT '用户 id',
  `song_id` bigint NULL DEFAULT NULL COMMENT '歌曲 id',
  `playlist_id` bigint NULL DEFAULT NULL COMMENT '歌单 id',
  `content` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '评论内容',
  `create_time` datetime NOT NULL COMMENT '评论时间',
  `type` tinyint NOT NULL COMMENT '评论类型：0-歌曲评论，1-歌单评论',
  `like_count` bigint NULL DEFAULT NULL COMMENT '点赞数量',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `fk_comment_song_id`(`song_id` ASC) USING BTREE,
  INDEX `fk_comment_user_id`(`user_id` ASC) USING BTREE,
  INDEX `fk_comment_playlist_id`(`playlist_id` ASC) USING BTREE,
  CONSTRAINT `fk_comment_playlist_id` FOREIGN KEY (`playlist_id`) REFERENCES `tb_playlist` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_comment_song_id` FOREIGN KEY (`song_id`) REFERENCES `tb_song` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_comment_user_id` FOREIGN KEY (`user_id`) REFERENCES `tb_user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 195 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for tb_feedback
-- ----------------------------
DROP TABLE IF EXISTS `tb_feedback`;
CREATE TABLE `tb_feedback`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '反馈 id',
  `user_id` bigint NOT NULL COMMENT '用户 id',
  `feedback` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '反馈内容',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `fk_feedback_user_id`(`user_id` ASC) USING BTREE,
  CONSTRAINT `fk_feedback_user_id` FOREIGN KEY (`user_id`) REFERENCES `tb_user` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 73 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for tb_genre
-- ----------------------------
DROP TABLE IF EXISTS `tb_genre`;
CREATE TABLE `tb_genre`  (
  `song_id` bigint NOT NULL COMMENT '歌曲 id',
  `style_id` bigint NOT NULL COMMENT '风格 id',
  PRIMARY KEY (`song_id`, `style_id`) USING BTREE,
  INDEX `fk_genre_style_id`(`style_id` ASC) USING BTREE,
  CONSTRAINT `fk_genre_song_id` FOREIGN KEY (`song_id`) REFERENCES `tb_song` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_genre_style_id` FOREIGN KEY (`style_id`) REFERENCES `tb_style` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for tb_playlist
-- ----------------------------
DROP TABLE IF EXISTS `tb_playlist`;
CREATE TABLE `tb_playlist`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '歌单 id',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '歌单标题',
  `cover_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '歌单封面',
  `introduction` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '歌单简介',
  `style` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '歌单风格',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 24 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for tb_playlist_binding
-- ----------------------------
DROP TABLE IF EXISTS `tb_playlist_binding`;
CREATE TABLE `tb_playlist_binding`  (
  `playlist_id` bigint NOT NULL COMMENT '歌单 id',
  `song_id` bigint NOT NULL COMMENT '歌曲 id',
  PRIMARY KEY (`playlist_id`, `song_id`) USING BTREE,
  INDEX `fk_playlist_binding_song_id`(`song_id` ASC) USING BTREE,
  CONSTRAINT `fk_playlist_binding_playlist_id` FOREIGN KEY (`playlist_id`) REFERENCES `tb_playlist` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_playlist_binding_song_id` FOREIGN KEY (`song_id`) REFERENCES `tb_song` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for tb_song
-- ----------------------------
DROP TABLE IF EXISTS `tb_song`;
CREATE TABLE `tb_song`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '歌曲 id',
  `artist_id` bigint NOT NULL COMMENT '歌手 id',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '歌名',
  `album` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '专辑',
  `lyric` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '歌词',
  `duration` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '歌曲时长',
  `style` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '歌曲风格',
  `cover_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '歌曲封面 url',
  `audio_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '歌曲 url',
  `release_time` date NOT NULL COMMENT '歌曲发行时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `fk_song_artist_id`(`artist_id` ASC) USING BTREE,
  CONSTRAINT `fk_song_artist_id` FOREIGN KEY (`artist_id`) REFERENCES `tb_artist` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 536 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for tb_style
-- ----------------------------
DROP TABLE IF EXISTS `tb_style`;
CREATE TABLE `tb_style`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '风格 id',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '风格名称',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `name`(`name` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 18 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for tb_user
-- ----------------------------
DROP TABLE IF EXISTS `tb_user`;
CREATE TABLE `tb_user`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户 id',
  `username` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户名',
  `password` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户密码',
  `phone` varchar(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用户手机号',
  `email` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户邮箱',
  `user_avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用户头像',
  `introduction` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用户简介',
  `create_time` datetime NOT NULL COMMENT '用户创建时间',
  `update_time` datetime NOT NULL COMMENT '用户修改时间',
  `status` tinyint NOT NULL COMMENT '用户状态：0-启用，1-禁用',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `username`(`username` ASC) USING BTREE,
  UNIQUE INDEX `email`(`email` ASC) USING BTREE,
  UNIQUE INDEX `phone`(`phone` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 149 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for tb_user_favorite
-- ----------------------------
DROP TABLE IF EXISTS `tb_user_favorite`;
CREATE TABLE `tb_user_favorite`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `user_id` bigint NOT NULL COMMENT '用户 id',
  `type` tinyint NOT NULL COMMENT '收藏类型：0-歌曲，1-歌单',
  `song_id` bigint NULL DEFAULT NULL COMMENT '收藏歌曲 id',
  `playlist_id` bigint NULL DEFAULT NULL COMMENT '收藏歌单 id',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `fk_user_favorite_user_id`(`user_id` ASC) USING BTREE,
  INDEX `fk_user_favorite_song_id`(`song_id` ASC) USING BTREE,
  INDEX `fk_user_favorite_playlist_id`(`playlist_id` ASC) USING BTREE,
  CONSTRAINT `fk_user_favorite_playlist_id` FOREIGN KEY (`playlist_id`) REFERENCES `tb_playlist` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_user_favorite_song_id` FOREIGN KEY (`song_id`) REFERENCES `tb_song` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_user_favorite_user_id` FOREIGN KEY (`user_id`) REFERENCES `tb_user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 433 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '账号/昵称',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '密码（加密后）',
  `sex` tinyint(1) NOT NULL DEFAULT 0 COMMENT '0 未知 1 男 2 女',
  `email` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '邮箱地址',
  `avatar_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '头像',
  `profile` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '简介',
  `is_singer` tinyint(1) NULL DEFAULT 0 COMMENT '是否歌手：0否 1是',
  `status` tinyint(1) NOT NULL DEFAULT 0 COMMENT '0 正常 1 冻结',
  `create_time` bigint UNSIGNED NOT NULL DEFAULT 0 COMMENT '创建时间（秒）',
  `update_time` bigint UNSIGNED NOT NULL DEFAULT 0 COMMENT '更新时间（秒）',
  `delete_time` bigint UNSIGNED NOT NULL DEFAULT 0 COMMENT '软删除时间，0=未删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_username`(`username` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 13 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户表' ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
