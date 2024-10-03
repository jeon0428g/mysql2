package org.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories("org.example.demo.svc") // 리포지토리 패키지 경로
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}