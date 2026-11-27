package ru.practicum.statistics.client.grpc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import ru.practicum.ewm.stats.grpc.dashboard.InteractionsCountRequestProto;
import ru.practicum.ewm.stats.grpc.dashboard.RecommendationsControllerGrpc;
import ru.practicum.ewm.stats.grpc.dashboard.RecommendedEventProto;
import ru.practicum.ewm.stats.grpc.dashboard.SimilarEventsRequestProto;
import ru.practicum.ewm.stats.grpc.dashboard.UserPredictionsRequestProto;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RecommendationsGrpcClientTest {
    private RecommendationsGrpcClient client;
    private RecommendationsControllerGrpc.RecommendationsControllerBlockingStub stub;

    @BeforeEach
    void setUp() {
        client = new RecommendationsGrpcClient();
        stub = mock(RecommendationsControllerGrpc.RecommendationsControllerBlockingStub.class);
        ReflectionTestUtils.setField(client, "client", stub);
        when(stub.withDeadlineAfter(anyLong(), eq(TimeUnit.SECONDS))).thenReturn(stub);
    }

    @Test
    void returnsUserRecommendationsFromStream() {
        RecommendedEventProto recommended = RecommendedEventProto.newBuilder()
                .setEventId(33L).setScore(0.4).build();
        when(stub.getRecommendationsForUser(any(UserPredictionsRequestProto.class)))
                .thenReturn(List.of(recommended).iterator());

        List<RecommendedEventProto> found = client.getRecommendationsForUser(11L, 5);
        assertEquals(List.of(recommended), found);

        ArgumentCaptor<UserPredictionsRequestProto> sent =
                ArgumentCaptor.forClass(UserPredictionsRequestProto.class);
        verify(stub).getRecommendationsForUser(sent.capture());
        assertEquals(11L, sent.getValue().getUserId());
        assertEquals(5, sent.getValue().getMaxResults());
    }

    @Test
    void forwardsEventAndUserWhenGettingSimilarEvents() {
        when(stub.getSimilarEvents(any(SimilarEventsRequestProto.class)))
                .thenReturn(List.<RecommendedEventProto>of().iterator());
        assertEquals(List.of(), client.getSimilarEvents(44L, 11L, 3));

        ArgumentCaptor<SimilarEventsRequestProto> sent =
                ArgumentCaptor.forClass(SimilarEventsRequestProto.class);
        verify(stub).getSimilarEvents(sent.capture());
        assertEquals(44L, sent.getValue().getEventId());
        assertEquals(11L, sent.getValue().getUserId());
        assertEquals(3, sent.getValue().getMaxResults());
    }

    @Test
    void passesMultipleIdentifiersToInteractionsCount() {
        when(stub.getInteractionsCount(any(InteractionsCountRequestProto.class)))
                .thenReturn(List.<RecommendedEventProto>of().iterator());
        assertEquals(List.of(), client.getInteractionsCount(List.of(3L, 5L)));

        ArgumentCaptor<InteractionsCountRequestProto> sent =
                ArgumentCaptor.forClass(InteractionsCountRequestProto.class);
        verify(stub).getInteractionsCount(sent.capture());
        assertEquals(List.of(3L, 5L), sent.getValue().getEventIdList());
    }

    @Test
    void rejectsInvalidRequestsWithoutCallingGrpc() {
        assertThrows(IllegalArgumentException.class, () -> client.getRecommendationsForUser(1L, 0));
        assertThrows(IllegalArgumentException.class, () -> client.getSimilarEvents(0L, 1L, 3));
        assertEquals(List.of(), client.getInteractionsCount(List.of()));
        verifyNoInteractions(stub);
    }
}
