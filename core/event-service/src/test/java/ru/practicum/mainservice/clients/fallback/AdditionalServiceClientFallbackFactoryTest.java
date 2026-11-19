package ru.practicum.mainservice.clients.fallback;

import org.junit.jupiter.api.Test;
import ru.practicum.common.exception.NotFoundException;
import ru.practicum.common.exception.ServiceUnavailableException;

import java.net.ConnectException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AdditionalServiceClientFallbackFactoryTest {

    private final AdditionalServiceClientFallbackFactory factory = new AdditionalServiceClientFallbackFactory();

    @Test
    void mapsTechnicalFailureForSingleCategoryLookup() {
        var fallback = factory.create(new ConnectException("connection refused"));

        assertThrows(ServiceUnavailableException.class, () -> fallback.getCategory(1L));
    }

    @Test
    void preservesBusinessNotFoundErrorForCategoryBatchLookup() {
        NotFoundException expected = new NotFoundException("Category was not found");
        var fallback = factory.create(expected);

        NotFoundException actual = assertThrows(
                NotFoundException.class,
                () -> fallback.getCategories(List.of(1L, 2L))
        );

        assertSame(expected, actual);
    }
}
