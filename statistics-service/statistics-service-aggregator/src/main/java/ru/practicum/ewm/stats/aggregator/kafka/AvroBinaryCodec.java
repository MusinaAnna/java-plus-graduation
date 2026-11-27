package ru.practicum.ewm.stats.aggregator.kafka;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificDatumWriter;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

@Component
public class AvroBinaryCodec {

    public UserActionAvro decodeAction(byte[] data) {
        try {
            return new SpecificDatumReader<UserActionAvro>(UserActionAvro.getClassSchema())
                    .read(null, DecoderFactory.get().binaryDecoder(data, null));
        } catch (IOException | RuntimeException ex) {
            throw new IllegalArgumentException("Invalid user action Avro payload", ex);
        }
    }

    public byte[] encodeSimilarity(EventSimilarityAvro similarity) {
        try (ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(buffer, null);
            new SpecificDatumWriter<EventSimilarityAvro>(EventSimilarityAvro.getClassSchema())
                    .write(similarity, encoder);
            encoder.flush();
            return buffer.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to encode event similarity", ex);
        }
    }
}
