
package ru.practicum.statistics.server.mapper;

import org.junit.jupiter.api.Test;
import ru.practicum.dto.EndpointHit;
import ru.practicum.statistics.server.model.HitEntity;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class HitEntityMapperTest {

    private final HitEntityMapper mapper = new HitEntityMapperImpl();

    @Test
    void toEntity_shouldMapAllFieldsAndLeaveIdNull() {
        LocalDateTime timestamp =
                LocalDateTime.of(2025, 1, 1, 10, 0);

        EndpointHit source = new EndpointHit();
        source.setApp("test-app");
        source.setUri("/test");
        source.setIp("127.0.0.1");
        source.setTimestamp(timestamp);

        HitEntity result = mapper.toEntity(source);

        assertNotNull(result);
        assertNull(result.getId());
        assertEquals(source.getApp(), result.getApp());
        assertEquals(source.getUri(), result.getUri());
        assertEquals(source.getIp(), result.getIp());
        assertEquals(source.getTimestamp(), result.getTimestamp());
    }

    @Test
    void toEntity_shouldReturnNull_whenSourceIsNull() {
        assertNull(mapper.toEntity(null));
    }
}
