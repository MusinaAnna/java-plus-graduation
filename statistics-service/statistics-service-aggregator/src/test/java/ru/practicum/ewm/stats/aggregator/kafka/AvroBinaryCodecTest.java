package ru.practicum.ewm.stats.aggregator.kafka;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

class AvroBinaryCodecTest {

    @Test
    void decodesCollectorWireFormatAndEncodesSimilarity() throws Exception {
        Instant timestamp = Instant.ofEpochMilli(1_738_000_000_123L);
        UserActionAvro action = UserActionAvro.newBuilder()
                .setUserId(1L).setEventId(2L).setActionType(ActionTypeAvro.LIKE)
                .setTimestamp(timestamp).build();
        AvroBinaryCodec codec = new AvroBinaryCodec();
        java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
            var encoder = org.apache.avro.io.EncoderFactory.get().binaryEncoder(buffer, null);
            new org.apache.avro.specific.SpecificDatumWriter<UserActionAvro>(UserActionAvro.getClassSchema())
                    .write(action, encoder);
            encoder.flush();
            byte[] wire = buffer.toByteArray();
        assertEquals(action, codec.decodeAction(wire));

        EventSimilarityAvro similarity = EventSimilarityAvro.newBuilder()
                .setEventA(1L).setEventB(2L).setScore(0.5)
                .setTimestamp(timestamp).build();
        var decoded = new org.apache.avro.specific.SpecificDatumReader<EventSimilarityAvro>(
                EventSimilarityAvro.getClassSchema()).read(null,
                org.apache.avro.io.DecoderFactory.get().binaryDecoder(codec.encodeSimilarity(similarity), null));
        assertEquals(similarity, decoded);
    }
}
