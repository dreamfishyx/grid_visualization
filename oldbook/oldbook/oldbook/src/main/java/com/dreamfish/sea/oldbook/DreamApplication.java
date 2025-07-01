package com.dreamfish.sea.oldbook;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.dreamfish.sea.oldbook.dao")
@Slf4j
public class DreamApplication {
    public static void main(String[] args) {
        SpringApplication.run(DreamApplication.class, args);
        log.info("Welcome to the oldbook system(Greetings from Dream fish),Your application is running at(default): http://localhost:8080");
    }

}
