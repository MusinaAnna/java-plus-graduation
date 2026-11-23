package ru.practicum.mainservice.compilations.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.mainservice.compilations.entity.Compilation;

public interface CompilationRepository extends JpaRepository<Compilation, Long> {
    @Query(value = "SELECT c FROM Compilation c WHERE (:pinned IS NULL OR c.pinned = :pinned) ORDER BY c.id",
            countQuery = "SELECT count(c) FROM Compilation c WHERE (:pinned IS NULL OR c.pinned = :pinned)")
    Page<Compilation> findAllByPinned(@Param("pinned") Boolean pinned, Pageable pageable);
}
