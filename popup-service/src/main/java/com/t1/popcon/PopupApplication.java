package com.t1.popcon;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class PopupApplication {
    public static void main(String[] args) {
        SpringApplication.run(PopupApplication.class, args);
    }
}