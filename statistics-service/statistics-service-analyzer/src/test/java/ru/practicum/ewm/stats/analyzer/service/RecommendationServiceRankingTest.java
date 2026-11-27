package ru.practicum.ewm.stats.analyzer.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import ru.practicum.ewm.stats.analyzer.repository.AnalyzerRepository;
import ru.practicum.ewm.stats.analyzer.repository.AnalyzerRepository.EventScore;
import ru.practicum.ewm.stats.analyzer.repository.AnalyzerRepository.RecommendedScore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecommendationServiceRankingTest {

    @Test
    void shouldSelectTopNCandidatesBySimilarityBeforePredictingRatings() {
        AnalyzerRepository repository = mock(AnalyzerRepository.class);
        RecommendationService service = new RecommendationService(repository, 10);

        when(repository.recentEvents(7L, 2)).thenReturn(List.of(100L));
        when(repository.interactedEventIds(7L)).thenReturn(Set.of(100L));
        when(repository.relatedEvents(List.of(100L))).thenReturn(List.of(
                new EventScore(100L, 201L, 0.95),
                new EventScore(100L, 202L, 0.90),
                new EventScore(100L, 203L, 0.80)));
        when(repository.predictWeights(List.of(201L, 202L), 7L, 10))
                .thenReturn(Map.of(201L, 0.20, 202L, 0.60));

        List<RecommendedScore> result = service.recommendationsForUser(7L, 2);

        assertEquals(List.of(202L, 201L), result.stream()
                .map(RecommendedScore::eventId).toList());
        verify(repository).predictWeights(List.of(201L, 202L), 7L, 10);
    }
}
