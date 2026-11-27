package ru.practicum.mainservice.requests.repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import ru.practicum.mainservice.requests.entity.RegistrationOutboxEvent;

public interface RegistrationOutboxRepository extends JpaRepository<RegistrationOutboxEvent, Long> {

    List<RegistrationOutboxEvent> findTop50BySentAtIsNullOrderByIdAsc();

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RegistrationOutboxEvent> findById(Long id);
}
