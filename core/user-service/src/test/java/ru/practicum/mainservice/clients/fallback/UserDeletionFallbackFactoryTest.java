package ru.practicum.mainservice.clients.fallback;

import org.junit.jupiter.api.Test;
import ru.practicum.common.exception.NotFoundException;
import ru.practicum.common.exception.ServiceUnavailableException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserDeletionFallbackFactoryTest {
    @Test
    void eventServiceFailureMustNotAllowUserDeletion() {
        EventServiceClientFallbackFactory factory = new EventServiceClientFallbackFactory();
        assertThatThrownBy(() -> factory.create(new IllegalStateException("connection refused")).hasEvents(7L))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessageContaining("event-service");
    }

    @Test
    void requestServiceFailureMustNotAllowUserDeletion() {
        RequestServiceClientFallbackFactory factory = new RequestServiceClientFallbackFactory();
        assertThatThrownBy(() -> factory.create(new IllegalStateException("timeout")).hasRequests(7L))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessageContaining("request-service");
    }

    @Test
    void legitimateDownstreamNotFoundIsNotTransformedIntoUnavailable() {
        EventServiceClientFallbackFactory factory = new EventServiceClientFallbackFactory();
        assertThatThrownBy(() -> factory.create(new NotFoundException("not found")).hasEvents(7L))
                .isInstanceOf(NotFoundException.class);
    }
}
