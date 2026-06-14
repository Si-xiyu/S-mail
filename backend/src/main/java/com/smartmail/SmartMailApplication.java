package com.smartmail;

import com.smartmail.attachment.config.AttachmentStorageProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@MapperScan("com.smartmail.**.mapper")
@EnableConfigurationProperties(AttachmentStorageProperties.class)
public class SmartMailApplication {
    public static void main(String[] args) {
        SpringApplication.run(SmartMailApplication.class, args);
    }
}
