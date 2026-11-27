package ru.practicum.mainservice.requests.service;

import ru.practicum.mainservice.requests.dto.EventRequestStatusUpdateRequest;
import ru.practicum.mainservice.requests.dto.EventRequestStatusUpdateResult;
import ru.practicum.mainservice.requests.dto.ParticipationRequestDto;

import java.util.List;

/**
 * Сервис для управления запросами пользователей на участие в событиях.
 * Предоставляет методы для получения, создания, отмены и изменения статуса запросов.
 */
public interface ParticipationRequestService {

    /**
     * Получение всех запросов пользователя.
     *
     * @param userId идентификатор пользователя
     * @return список запросов на участие
     */
    List<ParticipationRequestDto> getUserRequests(Long userId);

    /**
     * Создание запроса на участие в событии.
     *
     * @param userId  идентификатор пользователя
     * @param eventId идентификатор события
     * @return созданный запрос
     */
    ParticipationRequestDto addRequest(Long userId, Long eventId);

    /**
     * Отмена запроса на участие.
     *
     * @param userId    идентификатор пользователя
     * @param requestId идентификатор запроса
     * @return отмененный запрос
     */
    ParticipationRequestDto cancelRequest(Long userId, Long requestId);

    /**
     * Получение всех запросов для события инициатора.
     *
     * @param userId  идентификатор пользователя-инициатора
     * @param eventId идентификатор события
     * @return список запросов на участие
     */
    List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId);

    /**
     * Изменение статуса запросов на участие в событии (подтверждение/отклонение).
     *
     * @param userId        идентификатор пользователя-инициатора
     * @param eventId       идентификатор события
     * @param updateRequest запрос с новыми статусами для заявок
     * @return результат обновления статусов заявок
     */
    EventRequestStatusUpdateResult updateRequestStatus(Long userId, Long eventId,
                                                       EventRequestStatusUpdateRequest updateRequest);
}