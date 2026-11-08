package ru.practicum.statistics.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "ru.practicum")
public class StatisticsServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(StatisticsServerApplication.class);
    }
}
