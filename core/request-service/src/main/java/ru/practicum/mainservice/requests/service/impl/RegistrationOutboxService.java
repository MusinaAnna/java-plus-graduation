package ru.practicum.mainservice.requests.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import ru.practicum.mainservice.requests.entity.RegistrationOutboxEvent;
import ru.practicum.mainservice.requests.repository.RegistrationOutboxRepository;

@Service
@RequiredArgsConstructor
public class RegistrationOutboxService {
    private final RegistrationOutboxRepository repository;
    private final ApplicationEventPublisher publisher;

    public void enqueue(Long userId, Long eventId) {
        RegistrationOutboxEvent item = repository.save(new RegistrationOutboxEvent(userId, eventId));
        publisher.publishEvent(new RegistrationQueuedEvent(item.getId()));
    }

    public record RegistrationQueuedEvent(Long id) {
    }
}
