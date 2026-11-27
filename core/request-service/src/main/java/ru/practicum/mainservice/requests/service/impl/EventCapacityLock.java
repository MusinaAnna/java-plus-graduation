package ru.practicum.mainservice.requests.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class EventCapacityLock {

    private final JdbcTemplate jdbcTemplate;

    @Transactional(propagation = Propagation.MANDATORY)
    public void lock(long eventId) {
        jdbcTemplate.queryForObject("SELECT 1 FROM pg_advisory_xact_lock(?)", Integer.class, eventId);
    }
}
