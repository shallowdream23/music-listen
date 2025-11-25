package com.yi.musiclisten.generator;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.DataSourceConfig;
import com.baomidou.mybatisplus.generator.engine.VelocityTemplateEngine;

public class CodeGenerator {

    public static void main(String[] args) {

        FastAutoGenerator.create(new DataSourceConfig
                        .Builder("jdbc:mysql://192.168.128.130:3306/music?serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=utf-8&allowMultiQueries=true",
                        "root",
                        "root"))
                .globalConfig(builder -> {
                    builder.author("zzy")
                            .outputDir(System.getProperty("user.dir") + "/src/main/java")
                            .commentDate("yyyy-MM-dd");
                })
                .packageConfig(builder -> {
                    builder.parent("com.yi.musiclisten")
                            .entity("entity")
                            .service("service")
                            .serviceImpl("service.impl")
                            .mapper("mapper")
                            .xml("mapper.xml")
                            .controller("controller");
                })
                .strategyConfig(builder -> {
                    builder.addInclude("music_song_style") // TODO：你的表
                            .entityBuilder()
                            .enableLombok()
                            .enableChainModel()
                            .enableTableFieldAnnotation()      // 字段注解
                            .enableColumnConstant()
                            .logicDeleteColumnName("delete_time")
                            .idType(IdType.ASSIGN_ID)
                            .controllerBuilder()
                            .enableRestStyle()
                            .enableHyphenStyle()
                            .mapperBuilder()
                            .enableBaseResultMap()
                            .enableBaseColumnList();
                })
                .templateConfig(builder -> {
                    builder.entity("templates/entity-swagger.java.vm")
                            .controller("templates/controller-swagger.java.vm")
                            .xml("templates/mapper.xml.vm");
                    System.out.println("Template config is being applied"); // 添加日志验证
                })
                .templateEngine(new VelocityTemplateEngine())
                .execute();
    }
}
