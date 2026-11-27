package ru.practicum.ewm.stats.analyzer.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.analyzer.repository.AnalyzerRepository;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

@Component
public class AnalyzerKafkaListener {

    private final AnalyzerRepository repository;
    private final AnalyzerAvroDecoder decoder;

    public AnalyzerKafkaListener(AnalyzerRepository repository, AnalyzerAvroDecoder decoder) {
        this.repository = repository;
        this.decoder = decoder;
    }

    @KafkaListener(topics = "${stats.kafka.topics.user-actions:stats.user-actions.v1}",
                   containerFactory = "analyzerKafkaListenerContainerFactory")
    public void handleAction(ConsumerRecord<Long, byte[]> record, Acknowledgment acknowledgment) {
        UserActionAvro action = decoder.decodeAction(record.value());
        repository.upsertAction(action);
        acknowledgment.acknowledge();
    }

    @KafkaListener(topics = "${stats.kafka.topics.events-similarity:stats.events-similarity.v1}",
                   containerFactory = "analyzerKafkaListenerContainerFactory")
    public void handleSimilarity(ConsumerRecord<Long, byte[]> record, Acknowledgment acknowledgment) {
        EventSimilarityAvro similarity = decoder.decodeSimilarity(record.value());
        repository.upsertSimilarity(similarity, record.offset());
        acknowledgment.acknowledge();
    }
}
