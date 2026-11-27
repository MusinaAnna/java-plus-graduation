package ru.practicum.mainservice.clients.fallback;

import org.junit.jupiter.api.Test;
import ru.practicum.common.exception.NotFoundException;

import java.net.ConnectException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RequestServiceClientFallbackFactoryTest {

    private final RequestServiceClientFallbackFactory factory = new RequestServiceClientFallbackFactory();

    @Test
    void mapsTechnicalFailureForBatchConfirmedCountsLookup() {
        var fallback = factory.create(new ConnectException("connection refused"));

        assertTrue(fallback.getConfirmedCounts(List.of(1L, 2L)).isEmpty());
        assertEquals(0L, fallback.getConfirmedCount(1L));
    }

    @Test
    void preservesBusinessNotFoundErrorForSingleConfirmedCountLookup() {
        NotFoundException expected = new NotFoundException("Event request was not found");
        var fallback = factory.create(expected);

        NotFoundException actual = assertThrows(
                NotFoundException.class,
                () -> fallback.getConfirmedCount(1L)
        );

        assertSame(expected, actual);
    }
}
