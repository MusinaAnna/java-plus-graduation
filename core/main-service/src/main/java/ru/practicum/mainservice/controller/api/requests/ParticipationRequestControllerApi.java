package ru.practicum.mainservice.controller.api.requests;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.mainservice.requests.dto.EventRequestStatusUpdateRequest;
import ru.practicum.mainservice.requests.dto.EventRequestStatusUpdateResult;
import ru.practicum.mainservice.requests.dto.ParticipationRequestDto;

import java.util.List;

/**
 * API для управления запросами текущего пользователя на участие в событиях.
 * Предоставляет методы для создания, отмены и изменения статуса запросов.
 */
public interface ParticipationRequestControllerApi {

    /**
     * Получение всех запросов текущего пользователя.
     *
     * @param userId идентификатор пользователя
     * @return список запросов на участие
     */
    @GetMapping("/requests")
    List<ParticipationRequestDto> getUserRequests(@PathVariable @Positive Long userId);

    /**
     * Создание запроса на участие в событии.
     *
     * @param userId  идентификатор пользователя
     * @param eventId идентификатор события
     * @return созданный запрос
     */
    @PostMapping("/requests")
    @ResponseStatus(HttpStatus.CREATED)
    ParticipationRequestDto addRequest(@PathVariable @Positive Long userId,
                                       @RequestParam @Positive Long eventId);

    /**
     * Отмена запроса на участие.
     *
     * @param userId    идентификатор пользователя
     * @param requestId идентификатор запроса
     * @return отмененный запрос
     */
    @PatchMapping("/requests/{requestId}/cancel")
    ParticipationRequestDto cancelRequest(@PathVariable @Positive Long userId,
                                          @PathVariable @Positive Long requestId);

    /**
     * Получение всех запросов для конкретного события инициатора.
     *
     * @param userId  идентификатор пользователя-инициатора
     * @param eventId идентификатор события
     * @return список запросов на участие
     */
    @GetMapping("/events/{eventId}/requests")
    List<ParticipationRequestDto> getEventRequests(@PathVariable @Positive Long userId,
                                                   @PathVariable @Positive Long eventId);

    /**
     * Изменение статуса запросов на участие в событии (подтверждение/отклонение).
     *
     * @param userId        идентификатор пользователя-инициатора
     * @param eventId       идентификатор события
     * @param updateRequest запрос с новыми статусами для заявок
     * @return результат обновления статусов заявок
     */
    @PatchMapping("/events/{eventId}/requests")
    EventRequestStatusUpdateResult updateRequestStatus(@PathVariable @Positive Long userId,
                                                       @PathVariable @Positive Long eventId,
                                                       @RequestBody @Valid EventRequestStatusUpdateRequest updateRequest);
}