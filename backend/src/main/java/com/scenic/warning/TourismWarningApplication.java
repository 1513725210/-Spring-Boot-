package com.scenic.warning;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.scenic.warning.mapper")
@EnableScheduling
public class TourismWarningApplication {

    public static void main(String[] args) {
        SpringApplication.run(TourismWarningApplication.class, args);
    }
}
