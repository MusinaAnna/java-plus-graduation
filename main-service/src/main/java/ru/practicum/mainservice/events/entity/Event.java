package ru.practicum.mainservice.events.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.common.entity.Category;
import ru.practicum.common.entity.Location;
import ru.practicum.common.entity.User;
import ru.practicum.common.enums.EventState;

import java.time.LocalDateTime;

/**
 * Сущность события.
 * <p>
 * Содержит полную информацию о событии: аннотацию, описание, заголовок,
 * категорию, инициатора, местоположение, даты и статус жизненного цикла.
 * </p>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Краткое описание события (аннотация). Максимальная длина 2000 символов.
     */
    @Column(name = "annotation", nullable = false, length = 2000)
    private String annotation;

    /**
     * Полное описание события. Максимальная длина 7000 символов.
     */
    @Column(name = "description", nullable = false, length = 7000)
    private String description;

    /**
     * Заголовок события. Максимальная длина 120 символов.
     */
    @Column(name = "title", nullable = false, length = 120)
    private String title;

    /**
     * Категория события. Связь многие-к-одному с таблицей categories.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    /**
     * Пользователь-инициатор события. Связь многие-к-одному с таблицей users.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "initiator_id", nullable = false)
    private User initiator;

    /**
     * Местоположение события (широта и долгота).
     * Встраиваемый объект с переопределением имен колонок.
     */
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "lat", column = @Column(name = "location_lat")),
            @AttributeOverride(name = "lon", column = @Column(name = "location_lon"))
    })
    private Location location;

    /**
     * Дата и время проведения события.
     */
    @Column(name = "event_date", nullable = false)
    private LocalDateTime eventDate;

    /**
     * Дата и время создания события.
     */
    @Column(name = "created_on")
    private LocalDateTime createdOn;

    /**
     * Дата и время публикации события.
     */
    @Column(name = "published_on")
    private LocalDateTime publishedOn;

    /**
     * Флаг платности участия в событии.
     */
    @Column(name = "paid", nullable = false)
    private Boolean paid = false;

    /**
     * Лимит участников события. Значение 0 означает отсутствие ограничений.
     */
    @Column(name = "participant_limit")
    private Integer participantLimit = 0;

    /**
     * Флаг необходимости пре-модерации заявок на участие.
     */
    @Column(name = "request_moderation")
    private Boolean requestModeration = true;

    /**
     * Текущий статус жизненного цикла события.
     * Возможные значения: PENDING, PUBLISHED, CANCELED.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false)
    private EventState state = EventState.PENDING;
}