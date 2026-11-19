package ru.practicum.mainservice.clients.fallback;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import ru.practicum.common.exception.DownstreamExceptionMapper;
import ru.practicum.mainservice.clients.EventServiceClient;
import ru.practicum.common.dto.EventInternalDto;

@Component
public class EventServiceClientFallbackFactory implements FallbackFactory<EventServiceClient> {

    @Override
    public EventServiceClient create(Throwable cause) {
        return new EventServiceClient() {
            @Override
            public EventInternalDto getEvent(Long eventId) {
                throw DownstreamExceptionMapper.map("event-service", cause);
            }
        };
    }
}
