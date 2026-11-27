package ru.practicum.ewm.stats.analyzer.repository;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.stats.avro.ActionWeights;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

@Repository
public class AnalyzerRepository {

    private final JdbcTemplate jdbc;
    private final NamedParameterJdbcTemplate namedJdbc;

    public AnalyzerRepository(JdbcTemplate jdbc, NamedParameterJdbcTemplate namedJdbc) {
        this.jdbc = jdbc;
        this.namedJdbc = namedJdbc;
    }

    @Transactional
    public void upsertAction(UserActionAvro action) {
        if (action.getUserId() <= 0 || action.getEventId() <= 0 || action.getTimestamp() == null) {
            throw new IllegalArgumentException("Invalid user action");
        }
        double weight = ActionWeights.weight(action.getActionType());
        jdbc.update("""
                INSERT INTO user_event_interactions
                    (user_id, event_id, max_weight, last_interaction_at)
                VALUES (?, ?, ?, ?)
                ON CONFLICT (user_id, event_id) DO UPDATE SET
                    max_weight = GREATEST(user_event_interactions.max_weight, EXCLUDED.max_weight),
                    last_interaction_at = GREATEST(
                        user_event_interactions.last_interaction_at, EXCLUDED.last_interaction_at)
                """, action.getUserId(), action.getEventId(), weight,
                Timestamp.from(action.getTimestamp()));
    }

    @Transactional
    public void upsertSimilarity(EventSimilarityAvro similarity, long sourceOffset) {
        if (sourceOffset < 0) {
            throw new IllegalArgumentException("Kafka similarity offset must be non-negative");
        }
        if (similarity.getEventA() <= 0 || similarity.getEventB() <= 0
                || similarity.getEventA() == similarity.getEventB()
                || !Double.isFinite(similarity.getScore())
                || similarity.getScore() < 0 || similarity.getScore() > 1
                || similarity.getTimestamp() == null) {
            throw new IllegalArgumentException("Invalid event similarity");
        }
        long eventA = Math.min(similarity.getEventA(), similarity.getEventB());
        long eventB = Math.max(similarity.getEventA(), similarity.getEventB());
        jdbc.update("""
                INSERT INTO event_similarities (event_a, event_b, score, updated_at, applied_offset)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (event_a, event_b) DO UPDATE SET
                    score = EXCLUDED.score,
                    updated_at = GREATEST(event_similarities.updated_at, EXCLUDED.updated_at),
                    applied_offset = EXCLUDED.applied_offset
                WHERE event_similarities.applied_offset < EXCLUDED.applied_offset
                """, eventA, eventB, similarity.getScore(),
                Timestamp.from(similarity.getTimestamp()), sourceOffset);
    }

    public List<Long> recentEvents(long userId, int limit) {
        return jdbc.query("""
                SELECT event_id FROM user_event_interactions
                WHERE user_id = ?
                ORDER BY last_interaction_at DESC, event_id ASC
                LIMIT ?
                """, (rs, index) -> rs.getLong("event_id"), userId, limit);
    }

    public Set<Long> interactedEventIds(long userId) {
        return new HashSet<>(jdbc.query("""
                SELECT event_id FROM user_event_interactions WHERE user_id = ?
                """, (rs, index) -> rs.getLong("event_id"), userId));
    }

    public List<EventScore> relatedEvents(List<Long> seedEvents) {
        if (seedEvents.isEmpty()) {
            return List.of();
        }
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("events", seedEvents);
        return namedJdbc.query("""
                SELECT event_a, event_b, score FROM event_similarities
                WHERE score > 0 AND (event_a IN (:events) OR event_b IN (:events))
                """, params, (rs, index) -> new EventScore(
                rs.getLong("event_a"), rs.getLong("event_b"), rs.getDouble("score")));
    }

    public List<RecommendedScore> similarEvents(long eventId, long userId, int maxResults) {
        return jdbc.query("""
                SELECT CASE WHEN s.event_a = ? THEN s.event_b ELSE s.event_a END AS candidate,
                       s.score
                FROM event_similarities s
                WHERE (s.event_a = ? OR s.event_b = ?) AND s.score > 0
                  AND NOT EXISTS (
                    SELECT 1 FROM user_event_interactions i
                    WHERE i.user_id = ? AND i.event_id =
                        CASE WHEN s.event_a = ? THEN s.event_b ELSE s.event_a END)
                ORDER BY s.score DESC, candidate ASC
                LIMIT ?
                """, (rs, index) -> new RecommendedScore(
                rs.getLong("candidate"), rs.getDouble("score")),
                eventId, eventId, eventId, userId, eventId, maxResults);
    }

    /** Calculate predictions for all candidates with one PostgreSQL query. */
    public Map<Long, Double> predictWeights(List<Long> candidateIds, long userId, int neighborCount) {
        if (candidateIds.isEmpty()) {
            return Map.of();
        }
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("candidates", candidateIds)
                .addValue("userId", userId)
                .addValue("neighbors", neighborCount);
        Map<Long, Double> result = new HashMap<>();
        namedJdbc.query("""
                WITH candidate_neighbors AS (
                    SELECT s.event_a AS candidate, s.event_b AS neighbor,
                           s.score, i.max_weight
                    FROM event_similarities s
                    JOIN user_event_interactions i ON i.event_id = s.event_b
                        AND i.user_id = :userId
                    WHERE s.event_a IN (:candidates) AND s.score > 0
                    UNION ALL
                    SELECT s.event_b AS candidate, s.event_a AS neighbor,
                           s.score, i.max_weight
                    FROM event_similarities s
                    JOIN user_event_interactions i ON i.event_id = s.event_a
                        AND i.user_id = :userId
                    WHERE s.event_b IN (:candidates) AND s.score > 0
                ), ranked AS (
                    SELECT candidate, score, max_weight,
                           ROW_NUMBER() OVER (
                               PARTITION BY candidate ORDER BY score DESC, neighbor ASC
                           ) AS neighbor_rank
                    FROM candidate_neighbors
                )
                SELECT candidate, SUM(score * max_weight) / SUM(score) AS prediction
                FROM ranked
                WHERE neighbor_rank <= :neighbors
                GROUP BY candidate
                """, params, rs -> {
            result.put(rs.getLong("candidate"), rs.getDouble("prediction"));
        });
        return result;
    }

    public Map<Long, Double> interactionTotals(List<Long> requestedEvents) {
        List<Long> unique = new ArrayList<>(new LinkedHashSet<>(requestedEvents));
        if (unique.isEmpty()) {
            return Map.of();
        }
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("events", unique);
        Map<Long, Double> totals = new HashMap<>();
        namedJdbc.query("""
                SELECT event_id, SUM(max_weight) AS total
                FROM user_event_interactions
                WHERE event_id IN (:events)
                GROUP BY event_id
                """, params, rs -> {
            totals.put(rs.getLong("event_id"), rs.getDouble("total"));
        });
        Map<Long, Double> result = new LinkedHashMap<>();
        for (long eventId : unique) {
            result.put(eventId, totals.getOrDefault(eventId, 0.0));
        }
        return result;
    }

    public record EventScore(long eventA, long eventB, double similarity) {
    }

    public record RecommendedScore(long eventId, double score) {
    }

}
