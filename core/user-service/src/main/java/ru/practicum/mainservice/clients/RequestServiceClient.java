package ru.practicum.mainservice.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.practicum.mainservice.config.FeignClientErrorConfiguration;

@FeignClient(name = "request-service", path = "/internal/requests", configuration = FeignClientErrorConfiguration.class)
public interface RequestServiceClient {
    @GetMapping("/exists-by-requester/{userId}")
    boolean hasRequests(@PathVariable("userId") Long userId);
}
