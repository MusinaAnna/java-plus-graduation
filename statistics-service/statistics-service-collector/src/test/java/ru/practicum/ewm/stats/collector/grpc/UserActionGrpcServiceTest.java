package ru.practicum.ewm.stats.collector.grpc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.protobuf.Empty;
import com.google.protobuf.Timestamp;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.grpc.collector.ActionTypeProto;
import ru.practicum.ewm.stats.grpc.collector.UserActionProto;

class UserActionGrpcServiceTest {

    private KafkaTemplate<Long, UserActionAvro> template;
    private UserActionGrpcService service;
    private StreamObserver<Empty> observer;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        template = mock(KafkaTemplate.class);
        service = new UserActionGrpcService(template, "stats.user-actions.v1");
        observer = mock(StreamObserver.class);
    }

    @Test
    void sendsActionToKafkaAndAcknowledgesAfterSuccess() {
        UserActionProto request = UserActionProto.newBuilder()
                .setUserId(21)
                .setEventId(32)
                .setActionType(ActionTypeProto.ACTION_REGISTER)
                .setTimestamp(Timestamp.newBuilder().setSeconds(1_738_000_000L).setNanos(123_000_000))
                .build();
        when(template.send(eq("stats.user-actions.v1"), eq(21L), any(UserActionAvro.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        service.collectUserAction(request, observer);

        ArgumentCaptor<UserActionAvro> captor = ArgumentCaptor.forClass(UserActionAvro.class);
        verify(template).send(eq("stats.user-actions.v1"), eq(21L), captor.capture());
        UserActionAvro sent = captor.getValue();
        assertEquals(21L, sent.getUserId());
        assertEquals(32L, sent.getEventId());
        assertEquals(ActionTypeAvro.REGISTER, sent.getActionType());
        assertEquals(Instant.ofEpochSecond(1_738_000_000L, 123_000_000L), sent.getTimestamp());
        verify(observer).onNext(Empty.getDefaultInstance());
        verify(observer).onCompleted();
    }

    @Test
    void doesNotPublishInvalidPayload() {
        UserActionProto request = UserActionProto.newBuilder()
                .setUserId(21)
                .setEventId(0)
                .setActionType(ActionTypeProto.ACTION_VIEW)
                .setTimestamp(Timestamp.newBuilder().setSeconds(1_738_000_000L))
                .build();

        service.collectUserAction(request, observer);

        verify(template, never()).send(any(), any(), any());
        ArgumentCaptor<Throwable> captor = ArgumentCaptor.forClass(Throwable.class);
        verify(observer).onError(captor.capture());
        assertEquals(Status.Code.INVALID_ARGUMENT, Status.fromThrowable(captor.getValue()).getCode());
    }

    @Test
    void reportsKafkaFailureWithoutAcknowledging() {
        UserActionProto request = UserActionProto.newBuilder()
                .setUserId(21)
                .setEventId(32)
                .setActionType(ActionTypeProto.ACTION_LIKE)
                .setTimestamp(Timestamp.newBuilder().setSeconds(1_738_000_000L))
                .build();
        CompletableFuture<SendResult<Long, UserActionAvro>> future = new CompletableFuture<>();
        when(template.send(eq("stats.user-actions.v1"), eq(21L), any(UserActionAvro.class)))
                .thenReturn(future);

        service.collectUserAction(request, observer);
        future.completeExceptionally(new IllegalStateException("Kafka unavailable"));

        ArgumentCaptor<Throwable> captor = ArgumentCaptor.forClass(Throwable.class);
        verify(observer).onError(captor.capture());
        assertEquals(Status.Code.UNAVAILABLE, Status.fromThrowable(captor.getValue()).getCode());
        verify(observer, never()).onCompleted();
    }
}
