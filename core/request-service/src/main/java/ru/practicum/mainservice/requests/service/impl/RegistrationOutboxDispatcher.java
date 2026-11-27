package ru.practicum.mainservice.requests.service.impl;

import java.time.Instant;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import ru.practicum.ewm.stats.grpc.collector.ActionTypeProto;
import ru.practicum.mainservice.requests.entity.RegistrationOutboxEvent;
import ru.practicum.mainservice.requests.repository.RegistrationOutboxRepository;
import ru.practicum.statistics.client.grpc.UserActionGrpcClient;

@Slf4j
@Service
public class RegistrationOutboxDispatcher {

    private final RegistrationOutboxRepository repository;
    private final UserActionGrpcClient collector;
    private final TransactionTemplate transactionTemplate;

    public RegistrationOutboxDispatcher(RegistrationOutboxRepository repository,
                                       UserActionGrpcClient collector,
                                       PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.collector = collector;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRegistrationQueued(RegistrationOutboxService.RegistrationQueuedEvent event) {
        try {
            publishOne(event.id());
        } catch (RuntimeException exception) {
            log.warn("Immediate REGISTER attempt failed for outbox id={}, scheduler will retry",
                    event.id(), exception);
        }
    }

    /** Sends pending events again after Collector outages or restarts. */
    @Scheduled(fixedDelayString = "${registration.outbox.retry-delay-ms:10000}",
               initialDelayString = "${registration.outbox.initial-delay-ms:10000}")
    public void retryPending() {
        List<Long> ids = repository.findTop50BySentAtIsNullOrderByIdAsc().stream()
                .map(RegistrationOutboxEvent::getId)
                .toList();
        for (Long id : ids) {
            try {
                publishOne(id);
            } catch (RuntimeException exception) {
                log.warn("Outbox delivery failed, will retry id={}", id, exception);
            }
        }
    }

    /** A row lock prevents duplicate sends by a scheduler and an after-commit callback. */
    public void publishOne(Long id) {
        transactionTemplate.executeWithoutResult(ignored -> {
            RegistrationOutboxEvent item = repository.findById(id).orElse(null);
            if (item == null || item.getSentAt() != null) {
                return;
            }
            item.setAttemptCount(item.getAttemptCount() + 1);
            try {
                collector.sendAction(item.getUserId(), item.getEventId(),
                        ActionTypeProto.ACTION_REGISTER, item.getCreatedAt());
                item.setSentAt(Instant.now());
            } catch (RuntimeException exception) {
                log.warn("Collector unavailable for pending REGISTER id={}, attempt={}: {}",
                        id, item.getAttemptCount(), exception.getMessage());
            }
            repository.save(item);
        });
    }
}
