package com.t1.popcon;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.t1.popcon")
public class PopConApplication {
	public static void main(String[] args) {
		SpringApplication.run(PopConApplication.class, args);
	}
}