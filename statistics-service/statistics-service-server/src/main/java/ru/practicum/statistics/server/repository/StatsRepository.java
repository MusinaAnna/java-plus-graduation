package ru.practicum.statistics.server.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.practicum.dto.ViewStats;
import ru.practicum.statistics.server.model.HitEntity;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Репозиторий для работы с сущностью HitEntity.
 * Предоставляет методы для получения статистики с группировкой по приложению и URI.
 */
@Repository
public interface StatsRepository extends JpaRepository<HitEntity, Long> {

    /**
     * Получение статистики просмотров за период с фильтрацией по URI.
     * Подсчитывает общее количество хитов для каждого URI.
     *
     * @param start начальная дата и время периода
     * @param end   конечная дата и время периода
     * @param uris  список URI для фильтрации (опционально)
     * @return список статистики просмотров, отсортированный по убыванию количества хитов
     */
    @Query("SELECT new ru.practicum.dto.ViewStats(h.app, h.uri, COUNT(h.ip)) " +
            "FROM HitEntity h " +
            "WHERE h.timestamp BETWEEN :start AND :end " +
            "AND ( :uris IS NULL OR h.uri IN :uris ) " +
            "GROUP BY h.app, h.uri " +
            "ORDER BY COUNT(h.ip) DESC")
    List<ViewStats> findStats(@Param("start") LocalDateTime start,
                              @Param("end") LocalDateTime end,
                              @Param("uris") List<String> uris);

    /**
     * Получение статистики просмотров за период с подсчетом уникальных IP-адресов.
     * Каждый уникальный IP считается один раз для каждого URI.
     *
     * @param start начальная дата и время периода
     * @param end   конечная дата и время периода
     * @param uris  список URI для фильтрации (опционально)
     * @return список статистики просмотров с уникальными IP,
     * отсортированный по убыванию количества уникальных IP
     */
    @Query("SELECT new ru.practicum.dto.ViewStats(h.app, h.uri, COUNT(DISTINCT h.ip)) " +
            "FROM HitEntity h " +
            "WHERE h.timestamp BETWEEN :start AND :end " +
            "AND ( :uris IS NULL OR h.uri IN :uris ) " +
            "GROUP BY h.app, h.uri " +
            "ORDER BY COUNT(DISTINCT h.ip) DESC")
    List<ViewStats> findStatsUnique(@Param("start") LocalDateTime start,
                                    @Param("end") LocalDateTime end,
                                    @Param("uris") List<String> uris);
}