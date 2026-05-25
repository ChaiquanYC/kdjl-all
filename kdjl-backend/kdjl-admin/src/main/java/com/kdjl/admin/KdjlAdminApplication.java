package com.kdjl.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {"com.kdjl.admin"})
@EntityScan("com.kdjl.common.entity")
@EnableJpaRepositories("com.kdjl.admin.repository")
public class KdjlAdminApplication {
    public static void main(String[] args) {
        SpringApplication.run(KdjlAdminApplication.class, args);
    }
}
