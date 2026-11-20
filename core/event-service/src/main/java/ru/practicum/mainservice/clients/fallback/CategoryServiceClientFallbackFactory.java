package ru.practicum.mainservice.clients.fallback;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import ru.practicum.common.exception.DownstreamExceptionMapper;
import ru.practicum.mainservice.clients.CategoryServiceClient;
import ru.practicum.common.dto.CategoryDto;
import java.util.List;

@Component
public class CategoryServiceClientFallbackFactory implements FallbackFactory<CategoryServiceClient> {

    @Override
    public CategoryServiceClient create(Throwable cause) {
        return new CategoryServiceClient() {
            @Override
            public CategoryDto getCategory(Long categoryId) {
                throw DownstreamExceptionMapper.map("category-service", cause);
            }

            @Override
            public List<CategoryDto> getCategories(List<Long> ids) {
                throw DownstreamExceptionMapper.map("category-service", cause);
            }
        };
    }
}
