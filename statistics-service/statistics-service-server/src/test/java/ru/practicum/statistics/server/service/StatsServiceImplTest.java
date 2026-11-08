package ru.practicum.statistics.server.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.dto.EndpointHit;
import ru.practicum.dto.ViewStats;
import ru.practicum.statistics.server.mapper.HitEntityMapper;
import ru.practicum.statistics.server.model.HitEntity;
import ru.practicum.statistics.server.repository.StatsRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class StatsServiceImplTest {

    @Mock
    private StatsRepository repository;

    @Mock
    private HitEntityMapper mapper;

    @InjectMocks
    private StatsServiceImpl service;

    @Test
    void save_ShouldMapAndSaveHit() {
        EndpointHit dto = new EndpointHit();
        dto.setApp("test-app");
        dto.setUri("/test");
        dto.setIp("127.0.0.1");
        dto.setTimestamp(LocalDateTime.now());

        HitEntity entity = new HitEntity();
        entity.setId(1L);
        entity.setApp(dto.getApp());
        entity.setUri(dto.getUri());
        entity.setIp(dto.getIp());
        entity.setTimestamp(dto.getTimestamp());

        when(mapper.toEntity(dto)).thenReturn(entity);
        when(repository.save(entity)).thenReturn(entity);

        HitEntity result = service.save(dto);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("test-app", result.getApp());

        verify(mapper, times(1)).toEntity(dto);
        verify(repository, times(1)).save(entity);
    }

    @Test
    void getStats_shouldCallUniqueMethodWhenTrue() {
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now();
        List<String> uris = List.of("/test");
        List<ViewStats> expectedStats = List.of(new ViewStats("test-app", "/test", 5L));

        when(repository.findStatsUnique(start, end, uris)).thenReturn(expectedStats);

        List<ViewStats> result = service.getStats(start, end, uris, true);

        assertEquals(1, result.size());
        assertEquals("test-app", result.getFirst().getApp());

        verify(repository, times(1)).findStatsUnique(start, end, uris);
        verify(repository, never()).findStats(start, end, uris);
    }

    @Test
    void getStats_shouldCallNormalMethodWhenFalse() {
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now();
        List<String> uris = List.of("/test");
        List<ViewStats> expectedStats = List.of(new ViewStats("test-app", "/test", 5L));

        when(repository.findStats(start, end, uris)).thenReturn(expectedStats);

        List<ViewStats> result = service.getStats(start, end, uris, false);

        assertEquals(1, result.size());
        assertEquals("test-app", result.getFirst().getApp());

        verify(repository, times(1)).findStats(start, end, uris);
        verify(repository, never()).findStatsUnique(start, end, uris);
    }
}
