package ru.practicum.statistics.server.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import ru.practicum.dto.ViewStats;
import ru.practicum.statistics.server.model.HitEntity;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@ActiveProfiles("test")
public class StatsRepositoryTest {

    @Autowired
    private StatsRepository repository;

    private HitEntity hit1;
    private HitEntity hit2;

    @BeforeEach
    void setUp() {
        hit1 = new HitEntity();
        hit1.setApp("app1");
        hit1.setUri("/test");
        hit1.setIp("1.1.1.1");
        hit1.setTimestamp(LocalDateTime.of(2025, 1, 1, 10, 0));

        hit2 = new HitEntity();
        hit2.setApp("app1");
        hit2.setUri("/test");
        hit2.setIp("2.2.2.2");
        hit2.setTimestamp(LocalDateTime.of(2025, 1, 1, 11, 0));

        repository.save(hit1);
        repository.save(hit2);
    }

    @Test
    void findStats_shouldCountAllHits() {
        List<ViewStats> result = repository.findStats(
                LocalDateTime.of(2025, 1, 1, 9, 0),
                LocalDateTime.of(2025, 1, 1, 12, 0),
                null
        );

        assertEquals(1, result.size());
        assertEquals("app1", result.getFirst().getApp());
        assertEquals("/test", result.getFirst().getUri());
        assertEquals(2, result.getFirst().getHits());
    }

    @Test
    void findStatsUnique_shouldCountDistinctIps() {
        List<ViewStats> result = repository.findStatsUnique(
                LocalDateTime.of(2025, 1, 1, 9, 0),
                LocalDateTime.of(2025, 1, 1, 12, 0),
                null
        );

        assertEquals(1, result.size());
        assertEquals(2, result.getFirst().getHits());
    }

    @Test
    void findStats_withUrisFilter_shouldFilterByUris() {
        List<ViewStats> result = repository.findStatsUnique(
                LocalDateTime.of(2025, 1, 1, 9, 0),
                LocalDateTime.of(2025, 1, 1, 12, 0),
                List.of("/uri")
        );

        assertEquals(0, result.size());
    }
}
