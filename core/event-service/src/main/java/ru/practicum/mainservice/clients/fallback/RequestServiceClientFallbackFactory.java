package ru.practicum.mainservice.clients.fallback;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import ru.practicum.common.exception.DownstreamExceptionMapper;
import ru.practicum.common.exception.ServiceUnavailableException;
import ru.practicum.mainservice.clients.RequestServiceClient;
import java.util.List;
import java.util.Map;

@Component
public class RequestServiceClientFallbackFactory implements FallbackFactory<RequestServiceClient> {

    @Override
    public RequestServiceClient create(Throwable cause) {
        return new RequestServiceClient() {
            @Override
            public Map<Long, Long> getConfirmedCounts(List<Long> eventIds) {
                RuntimeException failure = DownstreamExceptionMapper.map("request-service", cause);
                if (failure instanceof ServiceUnavailableException) {
                    return Map.of();
                }
                throw failure;
            }

            @Override
            public long getConfirmedCount(Long eventId) {
                RuntimeException failure = DownstreamExceptionMapper.map("request-service", cause);
                if (failure instanceof ServiceUnavailableException) {
                    return 0L;
                }
                throw failure;
            }
        };
    }
}
