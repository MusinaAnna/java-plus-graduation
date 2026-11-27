package ru.practicum.ewm.stats.analyzer.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.analyzer.repository.AnalyzerRepository;
import ru.practicum.ewm.stats.analyzer.repository.AnalyzerRepository.EventScore;
import ru.practicum.ewm.stats.analyzer.repository.AnalyzerRepository.RecommendedScore;

@Service
public class RecommendationService {

    private final AnalyzerRepository repository;
    private final int neighborCount;

    public RecommendationService(AnalyzerRepository repository,
                                 @Value("${recommendations.neighbors-count:10}") int neighborCount) {
        this.repository = repository;
        if (neighborCount <= 0) {
            throw new IllegalArgumentException("neighbors-count must be positive");
        }
        this.neighborCount = neighborCount;
    }

    public List<RecommendedScore> recommendationsForUser(long userId, int maxResults) {
        List<Long> recent = repository.recentEvents(userId, maxResults);
        if (recent.isEmpty()) {
            return List.of();
        }
        Set<Long> seen = repository.interactedEventIds(userId);
        Set<Long> recentSet = Set.copyOf(recent);
        Map<Long, Double> candidates = new HashMap<>();
        for (EventScore pair : repository.relatedEvents(recent)) {
            if (recentSet.contains(pair.eventA()) && !seen.contains(pair.eventB())) {
                candidates.merge(pair.eventB(), pair.similarity(), Math::max);
            }
            if (recentSet.contains(pair.eventB()) && !seen.contains(pair.eventA())) {
                candidates.merge(pair.eventA(), pair.similarity(), Math::max);
            }
        }
        List<Long> candidateIds = candidates.entrySet().stream()
                .sorted(Comparator.<Map.Entry<Long, Double>>comparingDouble(Map.Entry::getValue)
                        .reversed().thenComparingLong(Map.Entry::getKey))
                .limit(maxResults)
                .map(Map.Entry::getKey)
                .toList();
        Map<Long, Double> predictions = repository.predictWeights(candidateIds, userId, neighborCount);
        List<RecommendedScore> result = new ArrayList<>();
        for (long candidateId : candidateIds) {
            double predictedWeight = predictions.getOrDefault(candidateId, 0.0);
            if (predictedWeight > 0) {
                result.add(new RecommendedScore(candidateId, predictedWeight));
            }
        }
        result.sort(Comparator.comparingDouble(RecommendedScore::score).reversed()
                .thenComparingLong(RecommendedScore::eventId));
        return result;
    }

    public List<RecommendedScore> similarEvents(long eventId, long userId, int maxResults) {
        return repository.similarEvents(eventId, userId, maxResults);
    }

    public Map<Long, Double> interactionTotals(List<Long> eventIds) {
        return repository.interactionTotals(eventIds);
    }
}
