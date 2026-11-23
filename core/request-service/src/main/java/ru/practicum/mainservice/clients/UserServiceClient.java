package ru.practicum.mainservice.clients;

import org.springframework.cloud.openfeign.FeignClient;
import ru.practicum.common.feign.FeignClientErrorConfiguration;
import ru.practicum.mainservice.clients.fallback.UserServiceClientFallbackFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.practicum.common.dto.UserDto;

@FeignClient(name = "user-service", path = "/internal/users", configuration = FeignClientErrorConfiguration.class, fallbackFactory = UserServiceClientFallbackFactory.class)
public interface UserServiceClient {
    @GetMapping("/{userId}")
    UserDto getUser(@PathVariable("userId") Long userId);
}
