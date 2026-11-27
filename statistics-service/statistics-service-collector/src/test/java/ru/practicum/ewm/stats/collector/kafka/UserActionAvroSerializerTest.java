package ru.practicum.ewm.stats.collector.kafka;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.time.Instant;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.junit.jupiter.api.Test;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

class UserActionAvroSerializerTest {

    @Test
    void encodesReadableAvroBinary() throws IOException {
        Instant time = Instant.ofEpochMilli(1_738_000_000_123L);
        UserActionAvro original = UserActionAvro.newBuilder()
                .setUserId(12L)
                .setEventId(42L)
                .setActionType(ActionTypeAvro.LIKE)
                .setTimestamp(time)
                .build();

        byte[] bytes = new UserActionAvroSerializer().serialize("stats.user-actions.v1", original);
        UserActionAvro restored = new SpecificDatumReader<UserActionAvro>(UserActionAvro.getClassSchema())
                .read(null, DecoderFactory.get().binaryDecoder(bytes, null));

        assertEquals(original.getUserId(), restored.getUserId());
        assertEquals(original.getEventId(), restored.getEventId());
        assertEquals(original.getActionType(), restored.getActionType());
        assertEquals(time, restored.getTimestamp());
    }
}
