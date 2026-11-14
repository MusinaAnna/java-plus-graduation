package ru.practicum.mainservice.comments.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import ru.practicum.common.entity.User;
import ru.practicum.mainservice.events.entity.Event;

import java.time.LocalDateTime;

/**
 * Сущность комментария для хранения в базе данных.
 * <p>
 * Представляет собой комментарий пользователя к событию.
 * Используется для сохранения, обновления и извлечения данных о комментариях
 * через JPA/Hibernate.
 *
 * <p>Связи с другими сущностями:
 * <ul>
 *   <li>{@link Event} - комментарий принадлежит событию (ManyToOne)</li>
 *   <li>{@link User} - комментарий принадлежит пользователю-автору (ManyToOne)</li>
 * </ul>
 *
 * <p>Особенности:
 * <ul>
 *   <li>Использует мягкое удаление (soft delete) через флаг {@code isDeleted}</li>
 *   <li>Автоматически проставляет время создания и обновления</li>
 *   <li>Каскадные операции не применяются для сохранения контроля</li>
 * </ul>
 *
 * @see ru.practicum.mainservice.comments.repository.CommentRepository
 * @see ru.practicum.common.dto.CommentDto
 */
@Entity
@Table(name = "comments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Comment {

    /**
     * Уникальный идентификатор комментария.
     * Генерируется автоматически базой данных (IDENTITY).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Текст комментария.
     * Обязательное поле.
     * Максимальная длина: 2000 символов.
     */
    @Column(nullable = false, length = 2000)
    private String text;

    /**
     * Ссылка на событие, к которому относится комментарий.
     * <p>
     * Связь ManyToOne с сущностью Event.
     * При удалении события все его комментарии каскадно удаляются.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    /**
     * Ссылка на автора комментария.
     * <p>
     * Связь ManyToOne с сущностью User.
     * При удалении пользователя все его комментарии каскадно удаляются.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    /**
     * Дата и время создания комментария.
     * <p>
     * Устанавливается автоматически при первом сохранении.
     * Не изменяется при последующих обновлениях.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Дата и время последнего обновления комментария.
     * <p>
     * Обновляется автоматически при каждом изменении сущности.
     * Может быть null, если комментарий не обновлялся после создания.
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Флаг мягкого удаления.
     * <p>
     * Позволяет скрыть комментарий без физического удаления из БД.
     * <ul>
     *   <li>{@code false} (по умолчанию) - комментарий активен</li>
     *   <li>{@code true} - комментарий удален (не отображается публично)</li>
     * </ul>
     * Используется для возможности восстановления комментариев.
     */
    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean deleted = false;
}
