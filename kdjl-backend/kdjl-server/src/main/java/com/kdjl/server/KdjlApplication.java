package com.kdjl.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.kdjl")
@EntityScan("com.kdjl.common.entity")
@EnableCaching
@EnableScheduling
public class KdjlApplication {

    public static void main(String[] args) {
        SpringApplication.run(KdjlApplication.class, args);
    }
}
