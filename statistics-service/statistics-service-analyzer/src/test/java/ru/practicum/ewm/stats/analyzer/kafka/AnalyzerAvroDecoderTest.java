package ru.practicum.ewm.stats.analyzer.kafka;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.junit.jupiter.api.Test;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

class AnalyzerAvroDecoderTest {

    @Test
    void decodesBothKafkaMessageTypes() throws IOException {
        Instant timestamp = Instant.ofEpochMilli(1_738_000_000_000L);
        UserActionAvro action = UserActionAvro.newBuilder()
                .setUserId(9).setEventId(11).setActionType(ActionTypeAvro.REGISTER)
                .setTimestamp(timestamp).build();
        EventSimilarityAvro similarity = EventSimilarityAvro.newBuilder()
                .setEventA(10).setEventB(11).setScore(0.8).setTimestamp(timestamp).build();
        AnalyzerAvroDecoder decoder = new AnalyzerAvroDecoder();
        assertEquals(action, decoder.decodeAction(encode(action)));
        assertEquals(similarity, decoder.decodeSimilarity(encode(similarity)));
    }

    private byte[] encode(org.apache.avro.specific.SpecificRecord record) throws IOException {
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
            BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(bytes, null);
            new SpecificDatumWriter<>(record.getSchema()).write(record, encoder);
            encoder.flush();
            return bytes.toByteArray();
        }
    }
}
