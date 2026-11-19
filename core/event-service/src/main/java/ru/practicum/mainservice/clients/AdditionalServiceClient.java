package ru.practicum.mainservice.clients;

import org.springframework.cloud.openfeign.FeignClient;
import ru.practicum.mainservice.config.FeignClientErrorConfiguration;
import ru.practicum.mainservice.clients.fallback.AdditionalServiceClientFallbackFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.common.dto.CategoryDto;

import java.util.List;

@FeignClient(name = "additional-service", path = "/internal/categories", configuration = FeignClientErrorConfiguration.class, fallbackFactory = AdditionalServiceClientFallbackFactory.class)
public interface AdditionalServiceClient {
    @GetMapping("/{categoryId}")
    CategoryDto getCategory(@PathVariable("categoryId") Long categoryId);

    @GetMapping
    List<CategoryDto> getCategories(@RequestParam("ids") List<Long> ids);
}
