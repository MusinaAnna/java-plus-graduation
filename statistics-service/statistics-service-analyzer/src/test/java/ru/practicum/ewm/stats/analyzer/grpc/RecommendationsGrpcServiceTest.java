package ru.practicum.ewm.stats.analyzer.grpc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.util.LinkedHashMap;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import ru.practicum.ewm.stats.analyzer.repository.AnalyzerRepository.RecommendedScore;
import ru.practicum.ewm.stats.analyzer.service.RecommendationService;
import ru.practicum.ewm.stats.grpc.dashboard.InteractionsCountRequestProto;
import ru.practicum.ewm.stats.grpc.dashboard.RecommendedEventProto;
import ru.practicum.ewm.stats.grpc.dashboard.SimilarEventsRequestProto;
import ru.practicum.ewm.stats.grpc.dashboard.UserPredictionsRequestProto;

class RecommendationsGrpcServiceTest {

    private RecommendationService recommendationService;
    private RecommendationsGrpcService grpc;
    private StreamObserver<RecommendedEventProto> observer;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        recommendationService = mock(RecommendationService.class);
        grpc = new RecommendationsGrpcService(recommendationService);
        observer = mock(StreamObserver.class);
    }

    @Test
    void returnsStreamOfPredictions() {
        when(recommendationService.recommendationsForUser(21L, 3))
                .thenReturn(List.of(new RecommendedScore(31, 0.75)));
        grpc.getRecommendationsForUser(UserPredictionsRequestProto.newBuilder()
                .setUserId(21).setMaxResults(3).build(), observer);
        verify(observer).onNext(RecommendedEventProto.newBuilder()
                .setEventId(31).setScore(0.75).build());
        verify(observer).onCompleted();
    }

    @Test
    void returnsSimilarEvents() {
        when(recommendationService.similarEvents(31, 21, 2))
                .thenReturn(List.of(new RecommendedScore(32, 0.8)));
        grpc.getSimilarEvents(SimilarEventsRequestProto.newBuilder()
                .setEventId(31).setUserId(21).setMaxResults(2).build(), observer);
        verify(observer).onNext(RecommendedEventProto.newBuilder()
                .setEventId(32).setScore(0.8).build());
        verify(observer).onCompleted();
    }

    @Test
    void returnsWeightedInteractionTotals() {
        LinkedHashMap<Long, Double> totals = new LinkedHashMap<>();
        totals.put(31L, 1.8);
        totals.put(32L, 0.0);
        when(recommendationService.interactionTotals(List.of(31L, 32L))).thenReturn(totals);
        grpc.getInteractionsCount(InteractionsCountRequestProto.newBuilder()
                .addEventId(31).addEventId(32).build(), observer);
        verify(observer).onNext(RecommendedEventProto.newBuilder()
                .setEventId(31).setScore(1.8).build());
        verify(observer).onNext(RecommendedEventProto.newBuilder()
                .setEventId(32).setScore(0.0).build());
        verify(observer).onCompleted();
    }

    @Test
    void rejectsInvalidRequest() {
        grpc.getRecommendationsForUser(UserPredictionsRequestProto.newBuilder()
                .setUserId(0).setMaxResults(5).build(), observer);
        ArgumentCaptor<Throwable> captor = ArgumentCaptor.forClass(Throwable.class);
        verify(observer).onError(captor.capture());
        assertEquals(Status.Code.INVALID_ARGUMENT, Status.fromThrowable(captor.getValue()).getCode());
    }

    @Test
    void rejectsExcessiveRecommendationLimit() {
        grpc.getRecommendationsForUser(UserPredictionsRequestProto.newBuilder()
                .setUserId(21).setMaxResults(1001).build(), observer);
        ArgumentCaptor<Throwable> captor = ArgumentCaptor.forClass(Throwable.class);
        verify(observer).onError(captor.capture());
        assertEquals(Status.Code.INVALID_ARGUMENT, Status.fromThrowable(captor.getValue()).getCode());
    }

    @Test
    void rejectsExcessiveSimilarEventsLimit() {
        grpc.getSimilarEvents(SimilarEventsRequestProto.newBuilder()
                .setUserId(21).setEventId(31).setMaxResults(1001).build(), observer);
        ArgumentCaptor<Throwable> captor = ArgumentCaptor.forClass(Throwable.class);
        verify(observer).onError(captor.capture());
        assertEquals(Status.Code.INVALID_ARGUMENT, Status.fromThrowable(captor.getValue()).getCode());
    }

    @Test
    void rejectsOversizedInteractionBatch() {
        grpc.getInteractionsCount(InteractionsCountRequestProto.newBuilder()
                .addAllEventId(java.util.stream.LongStream.rangeClosed(1, 1001).boxed().toList())
                .build(), observer);
        ArgumentCaptor<Throwable> captor = ArgumentCaptor.forClass(Throwable.class);
        verify(observer).onError(captor.capture());
        assertEquals(Status.Code.INVALID_ARGUMENT, Status.fromThrowable(captor.getValue()).getCode());
    }
}
