package ru.practicum.ewm.stats.collector.kafka;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;
import ru.practicum.ewm.stats.avro.UserActionAvro;

public class UserActionAvroSerializer implements Serializer<UserActionAvro> {

    @Override
    public byte[] serialize(String topic, UserActionAvro action) {
        if (action == null) {
            return null;
        }

        try (ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(buffer, null);
            new SpecificDatumWriter<UserActionAvro>(UserActionAvro.getClassSchema())
                    .write(action, encoder);
            encoder.flush();
            return buffer.toByteArray();
        } catch (IOException ex) {
            throw new SerializationException("Unable to encode user action for topic " + topic, ex);
        }
    }
}
