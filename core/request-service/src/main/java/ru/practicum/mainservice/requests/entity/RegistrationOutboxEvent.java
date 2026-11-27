package ru.practicum.mainservice.requests.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Durable registration action, saved in the same transaction as the request. */
@Entity
@Table(name = "registration_action_outbox", uniqueConstraints =
        @UniqueConstraint(name = "uq_registration_action", columnNames = {"user_id", "event_id"}))
@Getter
@Setter
@NoArgsConstructor
public class RegistrationOutboxEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    public RegistrationOutboxEvent(Long userId, Long eventId) {
        this.userId = userId;
        this.eventId = eventId;
        this.createdAt = Instant.now();
    }
}
