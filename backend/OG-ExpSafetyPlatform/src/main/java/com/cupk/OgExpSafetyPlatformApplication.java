package com.cupk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class OgExpSafetyPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(OgExpSafetyPlatformApplication.class, args);
    }

}
