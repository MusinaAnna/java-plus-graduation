package ru.practicum.ewm.stats.collector.grpc;

import com.google.protobuf.Empty;
import com.google.protobuf.Timestamp;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.time.DateTimeException;
import java.time.Instant;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.grpc.collector.UserActionControllerGrpc;
import ru.practicum.ewm.stats.grpc.collector.UserActionProto;

@GrpcService
public class UserActionGrpcService extends UserActionControllerGrpc.UserActionControllerImplBase {

    private final KafkaTemplate<Long, UserActionAvro> kafkaTemplate;
    private final String topic;

    public UserActionGrpcService(KafkaTemplate<Long, UserActionAvro> kafkaTemplate,
                                 @Value("${stats.kafka.topics.user-actions:stats.user-actions.v1}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    @Override
    public void collectUserAction(UserActionProto request, StreamObserver<Empty> responseObserver) {
        if (request.getUserId() <= 0 || request.getEventId() <= 0 || !request.hasTimestamp()) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("User, event and timestamp must be specified")
                    .asRuntimeException());
            return;
        }

        Timestamp timestamp = request.getTimestamp();
        if (timestamp.getNanos() < 0 || timestamp.getNanos() >= 1_000_000_000) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("Invalid action timestamp")
                    .asRuntimeException());
            return;
        }

        final Instant actionTime;
        final ActionTypeAvro actionType;
        try {
            actionTime = Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
            actionType = switch (request.getActionType()) {
                case ACTION_VIEW -> ActionTypeAvro.VIEW;
                case ACTION_REGISTER -> ActionTypeAvro.REGISTER;
                case ACTION_LIKE -> ActionTypeAvro.LIKE;
                default -> throw new IllegalArgumentException("Unknown action type");
            };
        } catch (DateTimeException | IllegalArgumentException ex) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("Invalid action payload")
                    .asRuntimeException());
            return;
        }

        UserActionAvro action = UserActionAvro.newBuilder()
                .setUserId(request.getUserId())
                .setEventId(request.getEventId())
                .setActionType(actionType)
                .setTimestamp(actionTime)
                .build();

        try {
            kafkaTemplate.send(topic, request.getUserId(), action).whenComplete((result, error) -> {
                if (error != null) {
                    responseObserver.onError(Status.UNAVAILABLE
                            .withDescription("Unable to store user action")
                            .withCause(error)
                            .asRuntimeException());
                } else {
                    responseObserver.onNext(Empty.getDefaultInstance());
                    responseObserver.onCompleted();
                }
            });
        } catch (RuntimeException ex) {
            responseObserver.onError(Status.UNAVAILABLE
                    .withDescription("Unable to send user action")
                    .withCause(ex)
                    .asRuntimeException());
        }
    }
}
