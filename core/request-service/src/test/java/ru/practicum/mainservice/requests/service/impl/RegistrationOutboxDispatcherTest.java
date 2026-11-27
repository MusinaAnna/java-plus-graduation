package ru.practicum.mainservice.requests.service.impl;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import ru.practicum.ewm.stats.grpc.collector.ActionTypeProto;
import ru.practicum.mainservice.requests.entity.RegistrationOutboxEvent;
import ru.practicum.mainservice.requests.repository.RegistrationOutboxRepository;
import ru.practicum.statistics.client.grpc.UserActionGrpcClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrationOutboxDispatcherTest {

    @Mock private RegistrationOutboxRepository repository;
    @Mock private UserActionGrpcClient collector;
    @Mock private PlatformTransactionManager transactionManager;
    private RegistrationOutboxDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        dispatcher = new RegistrationOutboxDispatcher(repository, collector, transactionManager);
    }

    @Test
    void failedCollectorActionRemainsPendingForRetryAndUsesOriginalTimestamp() {
        RegistrationOutboxEvent event = new RegistrationOutboxEvent(5L, 10L);
        event.setId(1L);
        when(transactionManager.getTransaction(any())).thenAnswer(ignored -> new SimpleTransactionStatus());
        when(repository.findById(1L)).thenReturn(Optional.of(event));
        doThrow(new IllegalStateException("Collector unavailable"))
                .doNothing()
                .when(collector).sendAction(5L, 10L, ActionTypeProto.ACTION_REGISTER, event.getCreatedAt());

        dispatcher.publishOne(1L);
        assertNull(event.getSentAt());
        assertEquals(1, event.getAttemptCount());

        dispatcher.publishOne(1L);
        assertNotNull(event.getSentAt());
        assertEquals(2, event.getAttemptCount());

        dispatcher.publishOne(1L);
        verify(collector, times(2)).sendAction(
                5L, 10L, ActionTypeProto.ACTION_REGISTER, event.getCreatedAt());
    }
}
