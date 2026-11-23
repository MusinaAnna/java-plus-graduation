package ru.practicum.mainservice.clients.fallback;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import ru.practicum.common.exception.DownstreamExceptionMapper;
import ru.practicum.mainservice.clients.RequestServiceClient;

/** Protects user deletion when the request service cannot be queried. */
@Component
public class RequestServiceClientFallbackFactory implements FallbackFactory<RequestServiceClient> {
    @Override
    public RequestServiceClient create(Throwable cause) {
        return userId -> {
            throw DownstreamExceptionMapper.map("request-service", cause);
        };
    }
}
