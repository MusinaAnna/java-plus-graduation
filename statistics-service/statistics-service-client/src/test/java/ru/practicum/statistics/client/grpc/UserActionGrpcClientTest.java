package ru.practicum.statistics.client.grpc;

import com.google.protobuf.Empty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import ru.practicum.ewm.stats.grpc.collector.ActionTypeProto;
import ru.practicum.ewm.stats.grpc.collector.UserActionControllerGrpc;
import ru.practicum.ewm.stats.grpc.collector.UserActionProto;

import java.time.Instant;
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

class UserActionGrpcClientTest {
    private UserActionGrpcClient client;
    private UserActionControllerGrpc.UserActionControllerBlockingStub stub;

    @BeforeEach
    void setUp() {
        client = new UserActionGrpcClient();
        stub = mock(UserActionControllerGrpc.UserActionControllerBlockingStub.class);
        ReflectionTestUtils.setField(client, "client", stub);
        when(stub.withDeadlineAfter(anyLong(), eq(TimeUnit.SECONDS))).thenReturn(stub);
        when(stub.collectUserAction(any(UserActionProto.class))).thenReturn(Empty.getDefaultInstance());
    }

    @Test
    void sendsActionWithExactTimestampAndType() {
        Instant actionTime = Instant.parse("2026-11-25T13:00:00.123456789Z");
        client.sendAction(11L, 22L, ActionTypeProto.ACTION_REGISTER, actionTime);

        ArgumentCaptor<UserActionProto> sent = ArgumentCaptor.forClass(UserActionProto.class);
        verify(stub).collectUserAction(sent.capture());
        assertEquals(11L, sent.getValue().getUserId());
        assertEquals(22L, sent.getValue().getEventId());
        assertEquals(ActionTypeProto.ACTION_REGISTER, sent.getValue().getActionType());
        assertEquals(actionTime.getEpochSecond(), sent.getValue().getTimestamp().getSeconds());
        assertEquals(actionTime.getNano(), sent.getValue().getTimestamp().getNanos());
    }

    @Test
    void sendsLikeUsingLikeActionType() {
        client.sendLike(3L, 4L);
        ArgumentCaptor<UserActionProto> sent = ArgumentCaptor.forClass(UserActionProto.class);
        verify(stub).collectUserAction(sent.capture());
        assertEquals(ActionTypeProto.ACTION_LIKE, sent.getValue().getActionType());
    }

    @Test
    void rejectsInvalidIdentifiersBeforeCallingGrpc() {
        assertThrows(IllegalArgumentException.class, () -> client.sendView(0L, 10L));
        assertThrows(IllegalArgumentException.class, () -> client.sendRegistration(3L, -1L));
        verifyNoInteractions(stub);
    }
}
