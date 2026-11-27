package ru.practicum.mainservice.controller.api.user;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.common.dto.UserDto;
import ru.practicum.mainservice.users.dto.NewUserRequest;

import java.util.List;

/**
 * API для административного управления пользователями.
 * Предоставляет методы для получения, создания и удаления пользователей.
 */
public interface AdminUserControllerApi {

    /**
     * Получение списка пользователей с пагинацией.
     *
     * @param ids  список идентификаторов пользователей (опционально)
     * @param from начальная позиция для пагинации
     * @param size размер страницы
     * @return список пользователей
     */
    @GetMapping
    List<UserDto> getUsers(
            @RequestParam(required = false) List<Long> ids,
            @RequestParam(defaultValue = "0") @PositiveOrZero int from,
            @RequestParam(defaultValue = "10") @Positive int size
    );

    /**
     * Создание нового пользователя.
     *
     * @param request данные нового пользователя
     * @return созданный пользователь
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    UserDto createUser(@Valid @RequestBody NewUserRequest request);

    /**
     * Удаление пользователя по идентификатору.
     *
     * @param userId идентификатор пользователя
     */
    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteUser(@PathVariable Long userId);
}