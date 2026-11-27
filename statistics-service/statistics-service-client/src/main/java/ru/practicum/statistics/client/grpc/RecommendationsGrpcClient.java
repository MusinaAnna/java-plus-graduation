package ru.practicum.statistics.client.grpc;

import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.grpc.dashboard.InteractionsCountRequestProto;
import ru.practicum.ewm.stats.grpc.dashboard.RecommendationsControllerGrpc;
import ru.practicum.ewm.stats.grpc.dashboard.RecommendedEventProto;
import ru.practicum.ewm.stats.grpc.dashboard.SimilarEventsRequestProto;
import ru.practicum.ewm.stats.grpc.dashboard.UserPredictionsRequestProto;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/** Calls Analyzer and fully consumes the gRPC server-streaming responses. */
@Component
public class RecommendationsGrpcClient {
    private static final long DEADLINE_SECONDS = 5;

    @GrpcClient("analyzer")
    private RecommendationsControllerGrpc.RecommendationsControllerBlockingStub client;

    public List<RecommendedEventProto> getRecommendationsForUser(long userId, int maxResults) {
        checkUserId(userId);
        checkLimit(maxResults);
        UserPredictionsRequestProto request = UserPredictionsRequestProto.newBuilder()
                .setUserId(userId)
                .setMaxResults(maxResults)
                .build();
        return readAll(client.withDeadlineAfter(DEADLINE_SECONDS, TimeUnit.SECONDS)
                .getRecommendationsForUser(request));
    }

    public List<RecommendedEventProto> getSimilarEvents(long eventId, long userId, int maxResults) {
        checkEventId(eventId);
        checkUserId(userId);
        checkLimit(maxResults);
        SimilarEventsRequestProto request = SimilarEventsRequestProto.newBuilder()
                .setEventId(eventId)
                .setUserId(userId)
                .setMaxResults(maxResults)
                .build();
        return readAll(client.withDeadlineAfter(DEADLINE_SECONDS, TimeUnit.SECONDS)
                .getSimilarEvents(request));
    }

    public List<RecommendedEventProto> getInteractionsCount(List<Long> eventIds) {
        Objects.requireNonNull(eventIds, "eventIds");
        if (eventIds.isEmpty()) {
            return List.of();
        }
        for (Long eventId : eventIds) {
            if (eventId == null || eventId <= 0) {
                throw new IllegalArgumentException("Event IDs must be positive");
            }
        }
        InteractionsCountRequestProto request = InteractionsCountRequestProto.newBuilder()
                .addAllEventId(eventIds)
                .build();
        return readAll(client.withDeadlineAfter(DEADLINE_SECONDS, TimeUnit.SECONDS)
                .getInteractionsCount(request));
    }

    private List<RecommendedEventProto> readAll(Iterator<RecommendedEventProto> results) {
        List<RecommendedEventProto> events = new ArrayList<>();
        while (results.hasNext()) {
            events.add(results.next());
        }
        return List.copyOf(events);
    }

    private void checkUserId(long userId) {
        if (userId <= 0) {
            throw new IllegalArgumentException("User ID must be positive");
        }
    }

    private void checkEventId(long eventId) {
        if (eventId <= 0) {
            throw new IllegalArgumentException("Event ID must be positive");
        }
    }

    private void checkLimit(int maxResults) {
        if (maxResults <= 0) {
            throw new IllegalArgumentException("maxResults must be positive");
        }
    }
}
