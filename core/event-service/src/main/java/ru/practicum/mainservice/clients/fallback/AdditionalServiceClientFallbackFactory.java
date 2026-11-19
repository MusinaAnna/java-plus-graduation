package ru.practicum.mainservice.clients.fallback;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import ru.practicum.common.exception.DownstreamExceptionMapper;
import ru.practicum.mainservice.clients.AdditionalServiceClient;
import ru.practicum.common.dto.CategoryDto;
import java.util.List;

@Component
public class AdditionalServiceClientFallbackFactory implements FallbackFactory<AdditionalServiceClient> {

    @Override
    public AdditionalServiceClient create(Throwable cause) {
        return new AdditionalServiceClient() {
            @Override
            public CategoryDto getCategory(Long categoryId) {
                throw DownstreamExceptionMapper.map("additional-service", cause);
            }

            @Override
            public List<CategoryDto> getCategories(List<Long> ids) {
                throw DownstreamExceptionMapper.map("additional-service", cause);
            }
        };
    }
}
