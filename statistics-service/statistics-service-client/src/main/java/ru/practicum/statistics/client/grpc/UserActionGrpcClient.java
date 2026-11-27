package ru.practicum.statistics.client.grpc;

import com.google.protobuf.Timestamp;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.grpc.collector.ActionTypeProto;
import ru.practicum.ewm.stats.grpc.collector.UserActionControllerGrpc;
import ru.practicum.ewm.stats.grpc.collector.UserActionProto;

import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/** Sends user interactions to Collector via service discovery. */
@Component
public class UserActionGrpcClient {
    private static final long DEADLINE_SECONDS = 5;

    @GrpcClient("collector")
    private UserActionControllerGrpc.UserActionControllerBlockingStub client;

    public void sendView(long userId, long eventId) {
        sendAction(userId, eventId, ActionTypeProto.ACTION_VIEW, Instant.now());
    }

    public void sendRegistration(long userId, long eventId) {
        sendAction(userId, eventId, ActionTypeProto.ACTION_REGISTER, Instant.now());
    }

    public void sendLike(long userId, long eventId) {
        sendAction(userId, eventId, ActionTypeProto.ACTION_LIKE, Instant.now());
    }

    /** Sends an individual event without retrying a potentially duplicated action. */
    public void sendAction(long userId, long eventId, ActionTypeProto actionType, Instant actionTime) {
        if (userId <= 0 || eventId <= 0) {
            throw new IllegalArgumentException("User and event IDs must be positive");
        }
        Objects.requireNonNull(actionType, "actionType");
        Objects.requireNonNull(actionTime, "actionTime");
        if (actionType == ActionTypeProto.UNRECOGNIZED) {
            throw new IllegalArgumentException("Unrecognized action type");
        }

        Timestamp timestamp = Timestamp.newBuilder()
                .setSeconds(actionTime.getEpochSecond())
                .setNanos(actionTime.getNano())
                .build();
        UserActionProto request = UserActionProto.newBuilder()
                .setUserId(userId)
                .setEventId(eventId)
                .setActionType(actionType)
                .setTimestamp(timestamp)
                .build();
        client.withDeadlineAfter(DEADLINE_SECONDS, TimeUnit.SECONDS).collectUserAction(request);
    }
}
