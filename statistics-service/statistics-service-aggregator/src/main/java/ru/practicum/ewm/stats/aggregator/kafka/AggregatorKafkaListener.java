package ru.practicum.ewm.stats.aggregator.kafka;

import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.aggregator.service.SimilarityAggregationService;

@Component
public class AggregatorKafkaListener {

    private final SimilarityAggregationService aggregationService;
    private final KafkaTemplate<Long, byte[]> producer;
    private final AvroBinaryCodec codec;
    private final String similarityTopic;
    private final Set<TopicPartition> validatedPartitions = new HashSet<>();

    public AggregatorKafkaListener(SimilarityAggregationService aggregationService,
                                   @Qualifier("aggregatorKafkaTemplate") KafkaTemplate<Long, byte[]> producer,
                                   AvroBinaryCodec codec,
                                   @Value("${stats.kafka.topics.events-similarity:stats.events-similarity.v1}")
                                   String similarityTopic) {
        this.aggregationService = aggregationService;
        this.producer = producer;
        this.codec = codec;
        this.similarityTopic = similarityTopic;
    }

    @KafkaListener(topics = "${stats.kafka.topics.user-actions:stats.user-actions.v1}",
                   containerFactory = "aggregatorKafkaListenerContainerFactory")
    public synchronized void consume(ConsumerRecord<Long, byte[]> record,
                                     Consumer<Long, byte[]> consumer) {
        TopicPartition source = new TopicPartition(record.topic(), record.partition());
        if (!validatedPartitions.contains(source)) {
            Long earliest = consumer.beginningOffsets(Set.of(source)).get(source);
            if (earliest == null || earliest > 0L) {
                throw new IllegalStateException("Cannot rebuild Aggregator: complete Kafka history is required for "
                        + source + ", earliest offset=" + earliest);
            }
            validatedPartitions.add(source);
        }
        UserActionAvro action = codec.decodeAction(record.value());
        SimilarityAggregationService.AggregationUpdate update = aggregationService.prepare(action);

        producer.executeInTransaction(operations -> {
            for (EventSimilarityAvro similarity : update.similarities()) {
                try {
                    operations.send(similarityTopic, similarity.getEventA(), codec.encodeSimilarity(similarity))
                            .get(30, TimeUnit.SECONDS);
                } catch (Exception exception) {
                    throw new IllegalStateException("Similarity publish failed", exception);
                }
            }
            operations.sendOffsetsToTransaction(
                    Map.of(source, new OffsetAndMetadata(record.offset() + 1)), consumer.groupMetadata());
            return null;
        });
        aggregationService.commit(update);
    }
}
