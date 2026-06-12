package com.englishstudy.backend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.englishstudy.backend.mapper")
public class EnglishStudyApplication {

    public static void main(String[] args) {
        SpringApplication.run(EnglishStudyApplication.class, args);
    }
}
