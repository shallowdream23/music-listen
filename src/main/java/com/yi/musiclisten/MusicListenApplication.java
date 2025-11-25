package com.yi.musiclisten;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.yi.musiclisten.mapper")
public class MusicListenApplication {

    public static void main(String[] args) {
        SpringApplication.run(MusicListenApplication.class, args);
    }

}
