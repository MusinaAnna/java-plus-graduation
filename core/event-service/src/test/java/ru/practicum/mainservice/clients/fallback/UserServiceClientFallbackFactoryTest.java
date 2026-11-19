package ru.practicum.mainservice.clients.fallback;

import org.junit.jupiter.api.Test;
import ru.practicum.common.exception.NotFoundException;
import ru.practicum.common.exception.ServiceUnavailableException;

import java.net.ConnectException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserServiceClientFallbackFactoryTest {

    private final UserServiceClientFallbackFactory factory = new UserServiceClientFallbackFactory();

    @Test
    void mapsTechnicalFailureToServiceUnavailable() {
        var fallback = factory.create(new ConnectException("connection refused"));

        assertThrows(ServiceUnavailableException.class, () -> fallback.getUser(1L));
    }

    @Test
    void mapsTechnicalFailureForBatchUserLookupToServiceUnavailable() {
        var fallback = factory.create(new ConnectException("connection refused"));

        assertThrows(ServiceUnavailableException.class, () -> fallback.getUsers(List.of(1L, 2L)));
    }

    @Test
    void preservesBusinessNotFoundError() {
        NotFoundException expected = new NotFoundException("User was not found");
        var fallback = factory.create(expected);

        NotFoundException actual = assertThrows(NotFoundException.class, () -> fallback.getUser(1L));

        assertSame(expected, actual);
    }
}
