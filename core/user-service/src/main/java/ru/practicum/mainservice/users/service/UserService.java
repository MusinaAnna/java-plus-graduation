package ru.practicum.mainservice.users.service;

import org.springframework.data.domain.Pageable;
import ru.practicum.common.dto.UserDto;
import ru.practicum.mainservice.users.dto.NewUserRequest;

import java.util.List;


public interface UserService {

    /**
     * Получение пользователей с фильтрацией по идентификаторам и пагинацией.
     * Если идентификаторы не переданы, возвращаются все пользователи.
     *
     * @param ids      список идентификаторов пользователей (опционально)
     * @param pageable параметры пагинации
     * @return список пользователей
     */
    List<UserDto> getUsers(List<Long> ids, Pageable pageable);

    /**
     * Создание нового пользователя.
     *
     * @param request данные нового пользователя
     * @return созданный пользователь
     */
    UserDto createUser(NewUserRequest request);

    /**
     * Удаление пользователя по идентификатору.
     *
     * @param userId идентификатор пользователя
     */
    void deleteUser(Long userId);

    UserDto getUser(Long userId);

    List<UserDto> getUsersByIds(List<Long> ids);
}