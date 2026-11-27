package ru.practicum.mainservice.requests.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import org.springframework.context.ApplicationEventPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.practicum.mainservice.requests.entity.RegistrationOutboxEvent;
import ru.practicum.mainservice.requests.repository.RegistrationOutboxRepository;

@ExtendWith(MockitoExtension.class)
class RegistrationOutboxServiceTest {
    @Mock private RegistrationOutboxRepository repository;
    @Mock private ApplicationEventPublisher publisher;


    @Test
    void enqueuePersistsRegistrationWithCorrectUserAndEvent() {
        when(repository.save(any(RegistrationOutboxEvent.class))).thenAnswer(call -> {
            RegistrationOutboxEvent item = call.getArgument(0);
            item.setId(42L);
            return item;
        });
        new RegistrationOutboxService(repository, publisher).enqueue(5L, 10L);

        ArgumentCaptor<RegistrationOutboxEvent> event =
                ArgumentCaptor.forClass(RegistrationOutboxEvent.class);
        verify(repository).save(event.capture());
        assertEquals(5L, event.getValue().getUserId());
        assertEquals(10L, event.getValue().getEventId());
    }

    @Test
    void enqueuePublishesAfterCommitDeliverySignal() {
        when(repository.save(any(RegistrationOutboxEvent.class))).thenAnswer(call -> {
            RegistrationOutboxEvent item = call.getArgument(0);
            item.setId(42L);
            return item;
        });
        new RegistrationOutboxService(repository, publisher).enqueue(5L, 10L);
        verify(publisher).publishEvent(new RegistrationOutboxService.RegistrationQueuedEvent(42L));
    }
}
