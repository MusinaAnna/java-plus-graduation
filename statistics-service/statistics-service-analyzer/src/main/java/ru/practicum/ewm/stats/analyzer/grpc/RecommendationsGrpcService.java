package ru.practicum.ewm.stats.analyzer.grpc;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.util.List;
import java.util.Map;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.practicum.ewm.stats.analyzer.repository.AnalyzerRepository.RecommendedScore;
import ru.practicum.ewm.stats.analyzer.service.RecommendationService;
import ru.practicum.ewm.stats.grpc.dashboard.InteractionsCountRequestProto;
import ru.practicum.ewm.stats.grpc.dashboard.RecommendedEventProto;
import ru.practicum.ewm.stats.grpc.dashboard.RecommendationsControllerGrpc;
import ru.practicum.ewm.stats.grpc.dashboard.SimilarEventsRequestProto;
import ru.practicum.ewm.stats.grpc.dashboard.UserPredictionsRequestProto;

@GrpcService
public class RecommendationsGrpcService extends RecommendationsControllerGrpc.RecommendationsControllerImplBase {

    private static final int MAX_RESULTS = 1000;
    private static final int MAX_INTERACTION_IDS = 1000;

    private final RecommendationService service;

    public RecommendationsGrpcService(RecommendationService service) {
        this.service = service;
    }

    @Override
    public void getRecommendationsForUser(UserPredictionsRequestProto request,
                                          StreamObserver<RecommendedEventProto> observer) {
        if (request.getUserId() <= 0 || request.getMaxResults() <= 0
                || request.getMaxResults() > MAX_RESULTS) {
            invalid(observer, "user_id must be positive and max_results must be between 1 and 1000");
            return;
        }
        try {
            emit(service.recommendationsForUser(request.getUserId(), request.getMaxResults()), observer);
        } catch (RuntimeException ex) {
            internal(observer, ex);
        }
    }

    @Override
    public void getSimilarEvents(SimilarEventsRequestProto request,
                                 StreamObserver<RecommendedEventProto> observer) {
        if (request.getUserId() <= 0 || request.getEventId() <= 0 || request.getMaxResults() <= 0
                || request.getMaxResults() > MAX_RESULTS) {
            invalid(observer, "user_id and event_id must be positive; max_results must be between 1 and 1000");
            return;
        }
        try {
            emit(service.similarEvents(request.getEventId(), request.getUserId(), request.getMaxResults()), observer);
        } catch (RuntimeException ex) {
            internal(observer, ex);
        }
    }

    @Override
    public void getInteractionsCount(InteractionsCountRequestProto request,
                                     StreamObserver<RecommendedEventProto> observer) {
        List<Long> eventIds = request.getEventIdList();
        if (eventIds.size() > MAX_INTERACTION_IDS || eventIds.stream().anyMatch(id -> id <= 0)) {
            invalid(observer, "event_id must be positive; at most 1000 IDs per request");
            return;
        }
        try {
            Map<Long, Double> totals = service.interactionTotals(eventIds);
            for (Map.Entry<Long, Double> entry : totals.entrySet()) {
                observer.onNext(RecommendedEventProto.newBuilder()
                        .setEventId(entry.getKey()).setScore(entry.getValue()).build());
            }
            observer.onCompleted();
        } catch (RuntimeException ex) {
            internal(observer, ex);
        }
    }

    private static void emit(List<RecommendedScore> recommended,
                             StreamObserver<RecommendedEventProto> observer) {
        for (RecommendedScore score : recommended) {
            observer.onNext(RecommendedEventProto.newBuilder()
                    .setEventId(score.eventId()).setScore(score.score()).build());
        }
        observer.onCompleted();
    }

    private static void invalid(StreamObserver<RecommendedEventProto> observer, String description) {
        observer.onError(Status.INVALID_ARGUMENT.withDescription(description).asRuntimeException());
    }

    private static void internal(StreamObserver<RecommendedEventProto> observer, RuntimeException cause) {
        observer.onError(Status.INTERNAL.withDescription("Recommendation query failed")
                .withCause(cause).asRuntimeException());
    }
}
