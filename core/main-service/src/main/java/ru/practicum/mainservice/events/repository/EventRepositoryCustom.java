package ru.practicum.mainservice.events.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import ru.practicum.common.enums.EventState;
import ru.practicum.mainservice.events.entity.Event;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Кастомный репозиторий для сложных запросов к событиям с использованием Criteria API.
 * <p>
 * Используется для динамического построения запросов с опциональными параметрами фильтрации.
 * </p>
 */
@Slf4j
@Repository
public class EventRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Поиск опубликованных событий с фильтрацией для публичного API.
     * Использует динамическое построение запроса через Criteria API.
     *
     * @param text       текст для поиска в аннотации и описании (опционально)
     * @param categories список идентификаторов категорий (опционально)
     * @param paid       фильтр по платности (опционально)
     * @param rangeStart дата и время начала диапазона (опционально)
     * @param rangeEnd   дата и время окончания диапазона (опционально)
     * @param pageable   параметры пагинации
     * @return страница опубликованных событий
     */
    public Page<Event> findPublishedEvents(
            String text,
            List<Long> categories,
            Boolean paid,
            LocalDateTime rangeStart,
            LocalDateTime rangeEnd,
            Pageable pageable
    ) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        // Запрос для подсчета количества
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Event> countRoot = countQuery.from(Event.class);
        countQuery.select(cb.count(countRoot));

        List<Predicate> countPredicates = buildPredicates(cb, countRoot, text, categories, paid, rangeStart, rangeEnd);
        if (!countPredicates.isEmpty()) {
            countQuery.where(countPredicates.toArray(new Predicate[0]));
        }
        Long total = entityManager.createQuery(countQuery).getSingleResult();

        // Запрос для получения данных
        CriteriaQuery<Event> query = cb.createQuery(Event.class);
        Root<Event> root = query.from(Event.class);
        query.select(root);

        List<Predicate> predicates = buildPredicates(cb, root, text, categories, paid, rangeStart, rangeEnd);
        if (!predicates.isEmpty()) {
            query.where(predicates.toArray(new Predicate[0]));
        }

        query.orderBy(cb.asc(root.get("id")));

        TypedQuery<Event> typedQuery = entityManager.createQuery(query);
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());

        List<Event> events = typedQuery.getResultList();

        log.info("EventRepositoryCustom: total={}, returned={}", total, events.size());
        if (!events.isEmpty()) {
            List<Long> ids = events.stream().map(Event::getId).toList();
            log.info("IDs: {}", ids);
        }

        return new PageImpl<>(events, pageable, total);
    }

    /**
     * Построение предикатов для фильтрации опубликованных событий.
     *
     * @param cb         CriteriaBuilder
     * @param root       корневой элемент запроса
     * @param text       текст для поиска
     * @param categories список категорий
     * @param paid       флаг платности
     * @param rangeStart начало диапазона дат
     * @param rangeEnd   конец диапазона дат
     * @return список предикатов для WHERE-условия
     */
    private List<Predicate> buildPredicates(
            CriteriaBuilder cb,
            Root<Event> root,
            String text,
            List<Long> categories,
            Boolean paid,
            LocalDateTime rangeStart,
            LocalDateTime rangeEnd
    ) {
        List<Predicate> predicates = new ArrayList<>();

        // Всегда фильтруем только опубликованные события
        predicates.add(cb.equal(root.get("state"), EventState.PUBLISHED));

        // Поиск по тексту (без учета регистра)
        if (text != null && !text.isEmpty()) {
            String searchPattern = "%" + text + "%";
            Predicate annotationPredicate = cb.like(
                    cb.lower(root.get("annotation")),
                    searchPattern.toLowerCase()
            );
            Predicate descriptionPredicate = cb.like(
                    cb.lower(root.get("description")),
                    searchPattern.toLowerCase()
            );
            predicates.add(cb.or(annotationPredicate, descriptionPredicate));
        }

        // Фильтр по категориям
        if (categories != null && !categories.isEmpty()) {
            predicates.add(root.get("category").get("id").in(categories));
        }

        // Фильтр по платности
        if (paid != null) {
            predicates.add(cb.equal(root.get("paid"), paid));
        }

        // Фильтр по диапазону дат
        if (rangeStart != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("eventDate"), rangeStart));
        }
        if (rangeEnd != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("eventDate"), rangeEnd));
        }

        return predicates;
    }

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
    public Page<Event> findAllByAdminFilters(
            List<Long> users,
            List<EventState> states,
            List<Long> categories,
            LocalDateTime rangeStart,
            LocalDateTime rangeEnd,
            Pageable pageable
    ) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        // Запрос для подсчета количества
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Event> countRoot = countQuery.from(Event.class);
        countQuery.select(cb.count(countRoot));

        List<Predicate> countPredicates = buildAdminPredicates(cb, countRoot, users, states, categories, rangeStart, rangeEnd);
        if (!countPredicates.isEmpty()) {
            countQuery.where(countPredicates.toArray(new Predicate[0]));
        }
        Long total = entityManager.createQuery(countQuery).getSingleResult();

        // Запрос для получения данных
        CriteriaQuery<Event> query = cb.createQuery(Event.class);
        Root<Event> root = query.from(Event.class);
        query.select(root);

        List<Predicate> predicates = buildAdminPredicates(cb, root, users, states, categories, rangeStart, rangeEnd);
        if (!predicates.isEmpty()) {
            query.where(predicates.toArray(new Predicate[0]));
        }

        query.orderBy(cb.desc(root.get("createdOn")));

        TypedQuery<Event> typedQuery = entityManager.createQuery(query);
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());

        List<Event> events = typedQuery.getResultList();
        return new PageImpl<>(events, pageable, total);
    }

    /**
     * Построение предикатов для административной фильтрации событий.
     *
     * @param cb         CriteriaBuilder
     * @param root       корневой элемент запроса
     * @param users      список пользователей
     * @param states     список статусов
     * @param categories список категорий
     * @param rangeStart начало диапазона дат
     * @param rangeEnd   конец диапазона дат
     * @return список предикатов для WHERE-условия
     */
    private List<Predicate> buildAdminPredicates(
            CriteriaBuilder cb,
            Root<Event> root,
            List<Long> users,
            List<EventState> states,
            List<Long> categories,
            LocalDateTime rangeStart,
            LocalDateTime rangeEnd
    ) {
        List<Predicate> predicates = new ArrayList<>();

        // Фильтр по инициаторам
        if (users != null && !users.isEmpty()) {
            predicates.add(root.get("initiator").get("id").in(users));
        }

        // Фильтр по статусам
        if (states != null && !states.isEmpty()) {
            predicates.add(root.get("state").in(states));
        }

        // Фильтр по категориям
        if (categories != null && !categories.isEmpty()) {
            predicates.add(root.get("category").get("id").in(categories));
        }

        // Фильтр по диапазону дат
        if (rangeStart != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("eventDate"), rangeStart));
        }

        if (rangeEnd != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("eventDate"), rangeEnd));
        }

        return predicates;
    }
}