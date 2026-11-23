package ru.practicum.mainservice.clients.fallback;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import ru.practicum.common.exception.DownstreamExceptionMapper;
import ru.practicum.mainservice.clients.EventServiceClient;

/** Protects user deletion when the event service cannot be queried. */
@Component
public class EventServiceClientFallbackFactory implements FallbackFactory<EventServiceClient> {
    @Override
    public EventServiceClient create(Throwable cause) {
        return userId -> {
            throw DownstreamExceptionMapper.map("event-service", cause);
        };
    }
}
