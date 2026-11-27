package ru.practicum.ewm.stats.aggregator.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.ActionWeights;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

class SimilarityAggregationServiceTest {

    private final SimilarityAggregationService service = new SimilarityAggregationService();

    @Test
    void followsWeightedSimilarityExample() {
        // A: [1,1,0,0.8,0], B: [0,0.8,1,0,1].
        commit(1, 1, ActionTypeAvro.LIKE);
        commit(2, 1, ActionTypeAvro.LIKE);
        commit(4, 1, ActionTypeAvro.REGISTER);
        commit(2, 2, ActionTypeAvro.REGISTER);
        commit(3, 2, ActionTypeAvro.LIKE);
        List<EventSimilarityAvro> result = commit(5, 2, ActionTypeAvro.LIKE);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getEventA());
        assertEquals(2L, result.get(0).getEventB());
        assertEquals(0.8 / 2.8, result.get(0).getScore(), 0.000001);
    }

    @Test
    void updatesDenominatorEvenWhenSharedMinimumDoesNotChange() {
        commit(1, 1, ActionTypeAvro.LIKE);
        commit(2, 1, ActionTypeAvro.LIKE);
        commit(4, 1, ActionTypeAvro.REGISTER);
        commit(2, 4, ActionTypeAvro.VIEW);
        commit(4, 4, ActionTypeAvro.VIEW);
        commit(5, 4, ActionTypeAvro.VIEW);

        List<EventSimilarityAvro> result = commit(4, 1, ActionTypeAvro.LIKE);
        EventSimilarityAvro similarity = result.stream()
                .filter(item -> item.getEventA() == 1 && item.getEventB() == 4)
                .findFirst().orElseThrow();
        assertEquals(0.8 / Math.sqrt(3.0 * 1.2), similarity.getScore(), 0.000001);
    }

    @Test
    void ignoresRepeatsAndLowerWeightActions() {
        commit(1, 1, ActionTypeAvro.LIKE);
        assertTrue(commit(1, 1, ActionTypeAvro.VIEW).isEmpty());
        assertTrue(commit(1, 1, ActionTypeAvro.REGISTER).isEmpty());
        assertTrue(commit(1, 1, ActionTypeAvro.LIKE).isEmpty());
    }

    @Test
    void skipsUnrelatedPairsAndUsesCanonicalOrderWhenUsersOverlap() {
        commit(10, 20, ActionTypeAvro.VIEW);
        assertTrue(commit(11, 3, ActionTypeAvro.LIKE).isEmpty());
        List<EventSimilarityAvro> result = commit(10, 3, ActionTypeAvro.VIEW);
        assertEquals(1, result.size());
        assertEquals(3L, result.get(0).getEventA());
        assertEquals(20L, result.get(0).getEventB());
        assertTrue(result.get(0).getScore() > 0);
    }

    @Test
    void stateIsNotChangedUntilUpdateIsCommitted() {
        commit(1, 1, ActionTypeAvro.VIEW);
        UserActionAvro newAction = action(2, 1, ActionTypeAvro.LIKE);
        var first = service.prepare(newAction);
        var second = service.prepare(newAction);
        assertEquals(first.similarities().size(), second.similarities().size());
        service.commit(first);
        assertTrue(service.prepare(newAction).similarities().isEmpty());
    }

    @Test
    void sharedActionWeightsHaveExpectedOrder() {
        assertEquals(0.4, ActionWeights.weight(ActionTypeAvro.VIEW));
        assertEquals(0.8, ActionWeights.weight(ActionTypeAvro.REGISTER));
        assertEquals(1.0, ActionWeights.weight(ActionTypeAvro.LIKE));
    }

    @Test
    void replayFromActionsRebuildsTheSameFinalSimilarity() {
        commit(1, 10, ActionTypeAvro.VIEW);
        commit(1, 10, ActionTypeAvro.LIKE);
        commit(2, 10, ActionTypeAvro.REGISTER);
        commit(1, 20, ActionTypeAvro.REGISTER);
        List<EventSimilarityAvro> original = commit(2, 20, ActionTypeAvro.LIKE);

        SimilarityAggregationService restarted = new SimilarityAggregationService();
        List<EventSimilarityAvro> latest = List.of();
        for (UserActionAvro item : List.of(
                action(2, 20, ActionTypeAvro.LIKE),
                action(1, 10, ActionTypeAvro.LIKE),
                action(2, 10, ActionTypeAvro.REGISTER),
                action(1, 20, ActionTypeAvro.REGISTER),
                action(1, 10, ActionTypeAvro.VIEW))) {
            var update = restarted.prepare(item);
            if (!update.similarities().isEmpty()) {
                latest = update.similarities();
            }
            restarted.commit(update);
        }
        assertEquals(original.get(0).getScore(), latest.get(0).getScore(), 0.000001);
        var repeat = restarted.prepare(action(2, 20, ActionTypeAvro.LIKE));
        assertTrue(repeat.similarities().isEmpty());
        assertEquals(original.get(0).getScore(), 1.6 / Math.sqrt(1.8 * 1.8), 0.000001);
    }

    private List<EventSimilarityAvro> commit(long user, long event, ActionTypeAvro actionType) {
        var update = service.prepare(action(user, event, actionType));
        service.commit(update);
        return update.similarities();
    }

    private UserActionAvro action(long user, long event, ActionTypeAvro actionType) {
        return UserActionAvro.newBuilder()
                .setUserId(user)
                .setEventId(event)
                .setActionType(actionType)
                .setTimestamp(Instant.ofEpochMilli(1_738_000_000_000L))
                .build();
    }
}
