package ru.practicum.ewm.stats.collector.config;

import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.LongSerializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.collector.kafka.UserActionAvroSerializer;

@Configuration
public class CollectorKafkaConfig {

    @Bean
    public ProducerFactory<Long, UserActionAvro> userActionProducerFactory(KafkaProperties properties) {
        Map<String, Object> configuration = new HashMap<>(properties.buildProducerProperties());
        configuration.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, LongSerializer.class);
        configuration.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, UserActionAvroSerializer.class);
        configuration.put(ProducerConfig.ACKS_CONFIG, "all");
        configuration.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        return new DefaultKafkaProducerFactory<>(configuration);
    }

    @Bean
    public KafkaTemplate<Long, UserActionAvro> userActionKafkaTemplate(
            ProducerFactory<Long, UserActionAvro> userActionProducerFactory) {
        return new KafkaTemplate<>(userActionProducerFactory);
    }
}
