package com.smartmail;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.smartmail.**.mapper")
public class SmartMailApplication {
    public static void main(String[] args) {
        SpringApplication.run(SmartMailApplication.class, args);
    }
}
