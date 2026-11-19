package ru.practicum.mainservice.config;

import feign.Response;
import feign.Request;
import feign.RetryableException;
import feign.Retryer;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.practicum.common.exception.BadRequestException;
import ru.practicum.common.exception.ConflictException;
import ru.practicum.common.exception.ForbiddenException;
import ru.practicum.common.exception.NotFoundException;

@Configuration
public class FeignClientErrorConfiguration {
    @Bean
    public ErrorDecoder downstreamErrorDecoder() {
        ErrorDecoder.Default fallback = new ErrorDecoder.Default();
        return (methodKey, response) -> decode(methodKey, response, fallback);
    }

    @Bean
    public Retryer retryer() {
        return new Retryer.Default(200, 1000, 3);
    }

    private Exception decode(String methodKey, Response response, ErrorDecoder fallback) {
        String message = "Downstream request failed: " + methodKey + " (HTTP " + response.status() + ")";
        if (response.status() >= 500 && response.status() < 600
                && response.request().httpMethod() == Request.HttpMethod.GET) {
            return new RetryableException(response.status(), message,
                    response.request().httpMethod(), (Long) null, response.request());
        }
        return switch (response.status()) {
            case 400 -> new BadRequestException(message);
            case 403 -> new ForbiddenException(message);
            case 404 -> new NotFoundException(message);
            case 409 -> new ConflictException(message);
            default -> fallback.decode(methodKey, response);
        };
    }
}
