package ru.practicum.ewm.stats.analyzer.kafka;

import java.io.IOException;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

@Component
public class AnalyzerAvroDecoder {

    public UserActionAvro decodeAction(byte[] data) {
        try {
            return new SpecificDatumReader<UserActionAvro>(UserActionAvro.getClassSchema())
                    .read(null, DecoderFactory.get().binaryDecoder(data, null));
        } catch (IOException | RuntimeException ex) {
            throw new IllegalArgumentException("Invalid user action Avro payload", ex);
        }
    }

    public EventSimilarityAvro decodeSimilarity(byte[] data) {
        try {
            return new SpecificDatumReader<EventSimilarityAvro>(EventSimilarityAvro.getClassSchema())
                    .read(null, DecoderFactory.get().binaryDecoder(data, null));
        } catch (IOException | RuntimeException ex) {
            throw new IllegalArgumentException("Invalid event similarity Avro payload", ex);
        }
    }
}
