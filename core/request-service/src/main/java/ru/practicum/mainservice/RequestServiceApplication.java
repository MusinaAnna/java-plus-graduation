package ru.practicum.mainservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableFeignClients(basePackages = "ru.practicum.mainservice.clients")
@EnableScheduling
@EnableAsync
@SpringBootApplication(scanBasePackages = "ru.practicum")
@EntityScan(basePackages = {"ru.practicum.common", "ru.practicum.mainservice"})
public class RequestServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(RequestServiceApplication.class, args);
    }
}
