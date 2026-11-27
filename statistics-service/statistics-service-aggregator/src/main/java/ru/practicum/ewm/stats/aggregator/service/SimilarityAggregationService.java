package ru.practicum.ewm.stats.aggregator.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.ActionWeights;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

@Service
public class SimilarityAggregationService {

    private final Map<Long, Map<Long, Double>> weightsByEvent = new HashMap<>();
    private final Map<Long, Double> sumByEvent = new HashMap<>();
    private final Map<EventPair, Double> minimumSums = new HashMap<>();

    /**
     * Builds an update without changing stored sums. Kafka output must succeed before commit.
     */
    public synchronized AggregationUpdate prepare(UserActionAvro action) {
        if (action.getUserId() <= 0 || action.getEventId() <= 0 || action.getTimestamp() == null) {
            throw new IllegalArgumentException("Invalid user action");
        }
        long eventId = action.getEventId();
        long userId = action.getUserId();
        double weight = ActionWeights.weight(action.getActionType());
        double oldWeight = weightsByEvent.getOrDefault(eventId, Map.of())
                .getOrDefault(userId, 0.0);
        double newWeight = Math.max(oldWeight, weight);
        if (newWeight <= oldWeight) {
            return new AggregationUpdate(eventId, userId, oldWeight,
                    sumByEvent.getOrDefault(eventId, 0.0), Map.of(), List.of());
        }

        double newSum = sumByEvent.getOrDefault(eventId, 0.0) + newWeight - oldWeight;
        Map<EventPair, Double> changedMinimumSums = new HashMap<>();
        List<EventSimilarityAvro> similarities = new ArrayList<>();

        for (Map.Entry<Long, Map<Long, Double>> entry : weightsByEvent.entrySet()) {
            long otherEvent = entry.getKey();
            if (otherEvent == eventId) {
                continue;
            }
            double otherSum = sumByEvent.getOrDefault(otherEvent, 0.0);
            double otherUserWeight = entry.getValue().getOrDefault(userId, 0.0);
            EventPair pair = EventPair.of(eventId, otherEvent);
            double oldMinimum = Math.min(oldWeight, otherUserWeight);
            double newMinimum = Math.min(newWeight, otherUserWeight);
            double minimumSum = minimumSums.getOrDefault(pair, 0.0)
                    + newMinimum - oldMinimum;
            if (minimumSum <= 0.0) {
                continue;
            }
            changedMinimumSums.put(pair, minimumSum);

            double score = minimumSum / Math.sqrt(newSum * otherSum);
            similarities.add(EventSimilarityAvro.newBuilder()
                    .setEventA(pair.eventA())
                    .setEventB(pair.eventB())
                    .setScore(score)
                    .setTimestamp(action.getTimestamp())
                    .build());
        }

        return new AggregationUpdate(eventId, userId, newWeight, newSum,
                changedMinimumSums, similarities);
    }

    public synchronized void commit(AggregationUpdate update) {
        if (update.newWeight() <= weightsByEvent.getOrDefault(update.eventId(), Map.of())
                .getOrDefault(update.userId(), 0.0)) {
            return;
        }
        weightsByEvent.computeIfAbsent(update.eventId(), ignored -> new HashMap<>())
                .put(update.userId(), update.newWeight());
        sumByEvent.put(update.eventId(), update.newEventSum());
        minimumSums.putAll(update.newMinimumSums());
    }

    public record AggregationUpdate(long eventId, long userId, double newWeight,
                                    double newEventSum, Map<EventPair, Double> newMinimumSums,
                                    List<EventSimilarityAvro> similarities) {

        public AggregationUpdate {
            newMinimumSums = Collections.unmodifiableMap(new HashMap<>(newMinimumSums));
            similarities = List.copyOf(similarities);
        }
    }

    public record EventPair(long eventA, long eventB) {

        public static EventPair of(long first, long second) {
            return new EventPair(Math.min(first, second), Math.max(first, second));
        }
    }
}
