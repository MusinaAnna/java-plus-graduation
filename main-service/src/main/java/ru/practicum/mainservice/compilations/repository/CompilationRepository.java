package ru.practicum.mainservice.compilations.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.mainservice.compilations.entity.Compilation;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с подборками событий.
 * Предоставляет методы для поиска подборок с пагинацией и загрузкой связанных событий.
 */
public interface CompilationRepository extends JpaRepository<Compilation, Long> {

    /**
     * Получение идентификаторов подборок с фильтрацией по флагу закрепления.
     * Используется для пагинированного поиска.
     *
     * @param pinned   фильтр по закрепленным/незакрепленным подборкам (опционально)
     * @param pageable параметры пагинации
     * @return страница с идентификаторами подборок
     */
    @Query("""
            SELECT c.id
            FROM Compilation c
            WHERE (:pinned IS NULL OR c.pinned = :pinned)
            ORDER BY c.id
            """)
    Page<Long> findCompilationIds(
            @Param("pinned") Boolean pinned,
            Pageable pageable
    );

    /**
     * Получение списка подборок с загрузкой связанных событий и их данных.
     * Выполняет JOIN FETCH для предотвращения N+1 проблемы.
     *
     * @param ids список идентификаторов подборок
     * @return список подборок с предзагруженными событиями, категориями и инициаторами
     */
    @Query("""
            SELECT DISTINCT c
            FROM Compilation c
            LEFT JOIN FETCH c.events e
            LEFT JOIN FETCH e.category
            LEFT JOIN FETCH e.initiator
            WHERE c.id IN :ids
            """)
    List<Compilation> findAllByIdsWithEvents(
            @Param("ids") Collection<Long> ids
    );

    /**
     * Получение подборки по идентификатору с загрузкой связанных событий.
     *
     * @param compId идентификатор подборки
     * @return Optional с подборкой, если найдена
     */
    @Query("""
            SELECT DISTINCT c
            FROM Compilation c
            LEFT JOIN FETCH c.events e
            LEFT JOIN FETCH e.category
            LEFT JOIN FETCH e.initiator
            WHERE c.id = :compId
            """)
    Optional<Compilation> findByIdWithEvents(
            @Param("compId") Long compId
    );
}