package com.wohngeld;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class WohngeldApplication {

    public static void main(String[] args) {
        SpringApplication.run(WohngeldApplication.class, args);
    }
}
