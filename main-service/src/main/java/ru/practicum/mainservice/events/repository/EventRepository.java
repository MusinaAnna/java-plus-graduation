package ru.practicum.mainservice.events.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.common.enums.EventState;
import ru.practicum.mainservice.events.entity.Event;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Репозиторий для работы с событиями.
 * Предоставляет методы для административной фильтрации, поиска по инициатору
 * и публичного поиска опубликованных событий с фильтрацией.
 */
public interface EventRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {

    /**
     * Поиск событий с административной фильтрацией.
     * Все параметры опциональны.
     *
     * @param users      список идентификаторов пользователей (опционально)
     * @param states     список состояний событий (опционально)
     * @param categories список идентификаторов категорий (опционально)
     * @param rangeStart дата и время начала диапазона (опционально)
     * @param rangeEnd   дата и время окончания диапазона (опционально)
     * @param pageable   параметры пагинации
     * @return страница событий
     */
    @Query("SELECT e FROM Event e " +
            "WHERE (:users IS NULL OR e.initiator.id IN :users) " +
            "AND (:states IS NULL OR e.state IN :states) " +
            "AND (:categories IS NULL OR e.category.id IN :categories) " +
            "AND (:rangeStart IS NULL OR e.eventDate >= :rangeStart) " +
            "AND (:rangeEnd IS NULL OR e.eventDate <= :rangeEnd)")
    Page<Event> findAllByAdminFilters(
            @Param("users") List<Long> users,
            @Param("states") List<EventState> states,
            @Param("categories") List<Long> categories,
            @Param("rangeStart") LocalDateTime rangeStart,
            @Param("rangeEnd") LocalDateTime rangeEnd,
            Pageable pageable
    );

    /**
     * Поиск событий по идентификатору инициатора с пагинацией.
     *
     * @param userId   идентификатор пользователя
     * @param pageable параметры пагинации
     * @return страница событий пользователя
     */
    Page<Event> findByInitiatorId(Long userId, Pageable pageable);

    /**
     * Поиск опубликованных событий с фильтрацией для публичного API.
     * Поиск по тексту выполняется без учета регистра.
     *
     * @param text       текст для поиска в аннотации и описании (опционально)
     * @param categories список идентификаторов категорий (опционально)
     * @param paid       фильтр по платности (опционально)
     * @param rangeStart дата и время начала диапазона
     * @param rangeEnd   дата и время окончания диапазона (опционально)
     * @param pageable   параметры пагинации
     * @return страница опубликованных событий
     */
    @Query("SELECT e FROM Event e " +
            "WHERE e.state = 'PUBLISHED' " +
            "AND (:text IS NULL OR LOWER(e.annotation) LIKE LOWER(CONCAT('%', :text, '%')) " +
            "OR LOWER(e.description) LIKE LOWER(CONCAT('%', :text, '%'))) " +
            "AND (:categories IS NULL OR e.category.id IN :categories) " +
            "AND (:paid IS NULL OR e.paid = :paid) " +
            "AND (e.eventDate >= :rangeStart) " +
            "AND (:rangeEnd IS NULL OR e.eventDate <= :rangeEnd)")
    Page<Event> findPublishedEvents(
            @Param("text") String text,
            @Param("categories") List<Long> categories,
            @Param("paid") Boolean paid,
            @Param("rangeStart") LocalDateTime rangeStart,
            @Param("rangeEnd") LocalDateTime rangeEnd,
            Pageable pageable
    );

    /**
     * Проверка существования событий в указанной категории.
     *
     * @param catId идентификатор категории
     * @return true - если есть события в категории, false - если нет
     */
    boolean existsByCategoryId(Long catId);
}