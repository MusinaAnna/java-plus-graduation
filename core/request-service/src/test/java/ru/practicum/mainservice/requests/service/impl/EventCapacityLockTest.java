package ru.practicum.mainservice.requests.service.impl;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class EventCapacityLockTest {

    @Test
    void locksTheEventInTheCurrentPostgresTransaction() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);

        new EventCapacityLock(jdbc).lock(10L);

        verify(jdbc).queryForObject("SELECT 1 FROM pg_advisory_xact_lock(?)", Integer.class, 10L);
    }
}
