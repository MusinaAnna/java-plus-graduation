package ru.practicum.mainservice.users.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.common.entity.User;

import java.util.Collection;

/**
 * Репозиторий для работы с пользователями.
 * Предоставляет методы для поиска пользователей по списку идентификаторов.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Поиск пользователей по списку идентификаторов с пагинацией.
     *
     * @param ids      коллекция идентификаторов пользователей
     * @param pageable параметры пагинации
     * @return страница пользователей
     */
    Page<User> findAllByIdIn(Collection<Long> ids, Pageable pageable);
}