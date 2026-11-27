package ru.practicum.ewm.stats.analyzer.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.ewm.stats.analyzer.repository.AnalyzerRepository;
import ru.practicum.ewm.stats.analyzer.repository.AnalyzerRepository.EventScore;
import ru.practicum.ewm.stats.analyzer.repository.AnalyzerRepository.RecommendedScore;

class RecommendationServiceTest {

    private AnalyzerRepository repository;
    private RecommendationService service;

    @BeforeEach
    void setUp() {
        repository = mock(AnalyzerRepository.class);
        service = new RecommendationService(repository, 3);
    }

    @Test
    void predictsOnlyUnseenCandidatesFromRecentEvents() {
        when(repository.recentEvents(21, 2)).thenReturn(List.of(10L, 20L));
        when(repository.interactedEventIds(21)).thenReturn(Set.of(10L, 20L, 30L));
        when(repository.relatedEvents(List.of(10L, 20L))).thenReturn(List.of(
                new EventScore(10, 40, 0.9),
                new EventScore(20, 40, 0.6),
                new EventScore(20, 50, 0.8),
                new EventScore(10, 30, 0.99)));
        when(repository.predictWeights(List.of(40L, 50L), 21, 3))
                .thenReturn(java.util.Map.of(40L, 0.6, 50L, 0.8));

        assertEquals(List.of(new RecommendedScore(50, 0.8), new RecommendedScore(40, 0.6)),
                service.recommendationsForUser(21, 2));
        verify(repository).predictWeights(List.of(40L, 50L), 21, 3);
    }

    @Test
    void returnsEmptyWhenUserHasNoInteractions() {
        when(repository.recentEvents(21, 5)).thenReturn(List.of());
        assertEquals(List.of(), service.recommendationsForUser(21, 5));
    }
}
