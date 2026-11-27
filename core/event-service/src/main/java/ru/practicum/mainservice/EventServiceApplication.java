package ru.practicum.mainservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.openfeign.EnableFeignClients;



@EnableFeignClients(basePackages = "ru.practicum.mainservice.clients")
@SpringBootApplication(scanBasePackages = "ru.practicum")
@EntityScan(basePackages = {"ru.practicum.common", "ru.practicum.mainservice"})
public class EventServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(EventServiceApplication.class, args);
    }
}
