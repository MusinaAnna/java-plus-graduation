package ru.practicum.statistics.client;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Конфигурация для создания бина RestTemplate.
 * Используется для HTTP-запросов к сервису статистики.
 */
@Configuration
public class RestTemplateConfig {

    /**
     * Создание бина RestTemplate для выполнения HTTP-запросов.
     *
     * @return экземпляр RestTemplate
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}