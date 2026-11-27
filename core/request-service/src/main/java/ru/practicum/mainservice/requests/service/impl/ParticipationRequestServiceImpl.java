package ru.practicum.mainservice.requests.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.common.dto.EventInternalDto;
import ru.practicum.common.enums.EventState;
import ru.practicum.common.enums.RequestStatus;
import ru.practicum.common.exception.ConflictException;
import ru.practicum.common.exception.NotFoundException;
import ru.practicum.mainservice.clients.EventServiceClient;
import ru.practicum.mainservice.clients.UserServiceClient;
import ru.practicum.mainservice.requests.dto.EventRequestStatusUpdateRequest;
import ru.practicum.mainservice.requests.dto.EventRequestStatusUpdateResult;
import ru.practicum.mainservice.requests.dto.ParticipationRequestDto;
import ru.practicum.mainservice.requests.entity.ParticipationRequest;
import ru.practicum.mainservice.requests.repository.ParticipationRequestRepository;
import ru.practicum.mainservice.requests.service.ParticipationRequestService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ParticipationRequestServiceImpl implements ParticipationRequestService {
    private final ParticipationRequestRepository requestRepository;
    private final EventServiceClient eventServiceClient;
    private final UserServiceClient userServiceClient;

    private final RegistrationOutboxService registrationOutboxService;
    private final EventCapacityLock eventCapacityLock;

    private ParticipationRequestDto toDto(ParticipationRequest request) {
        return new ParticipationRequestDto(request.getId(), request.getEventId(), request.getRequesterId(),
                request.getStatus(), request.getCreated());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParticipationRequestDto> getUserRequests(Long userId) {
        getUser(userId);
        return requestRepository.findByRequesterId(userId).stream().map(this::toDto).toList();
    }

    @Override
    public ParticipationRequestDto addRequest(Long userId, Long eventId) {
        getUser(userId);
        EventInternalDto event = getEvent(eventId);
        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Нельзя участвовать в неопубликованном событии");
        }
        if (event.getInitiatorId().equals(userId)) {
            throw new ConflictException("Инициатор события не может подать заявку на участие");
        }
        if (event.getParticipantLimit() > 0) {
            eventCapacityLock.lock(eventId);
        }
        if (requestRepository.findByEventIdAndRequesterId(eventId, userId).isPresent()) {
            throw new ConflictException("Запрос на участие уже существует");
        }
        long confirmedCount = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
        if (event.getParticipantLimit() > 0 && confirmedCount >= event.getParticipantLimit()) {
            throw new ConflictException("Достигнут лимит участников");
        }

        ParticipationRequest request = new ParticipationRequest();
        request.setEventId(eventId);
        request.setRequesterId(userId);
        request.setStatus(!event.getRequestModeration() || event.getParticipantLimit() == 0
                ? RequestStatus.CONFIRMED : RequestStatus.PENDING);
        request.setCreated(LocalDateTime.now());

        try {
            ParticipationRequest saved = requestRepository.saveAndFlush(request);
            log.info("Добавлен запрос на участие: {}", saved.getId());
            registrationOutboxService.enqueue(userId, eventId);
            return toDto(saved);
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("Запрос на участие уже существует");
        }
    }

    @Override
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        getUser(userId);
        ParticipationRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Запрос не найден"));
        if (!request.getRequesterId().equals(userId)) {
            throw new ConflictException("Нельзя отменить чужой запрос");
        }
        request.setStatus(RequestStatus.CANCELED);
        ParticipationRequest updated = requestRepository.save(request);
        log.info("Отменён запрос: {}", updated.getId());
        return toDto(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId) {
        getUser(userId);
        EventInternalDto event = getEvent(eventId);
        if (!event.getInitiatorId().equals(userId)) {
            throw new ConflictException("Только инициатор события может видеть заявки");
        }
        return requestRepository.findByEventId(eventId).stream().map(this::toDto).toList();
    }

    @Override
    public EventRequestStatusUpdateResult updateRequestStatus(Long userId, Long eventId,
                                                              EventRequestStatusUpdateRequest updateRequest) {
        getUser(userId);
        EventInternalDto event = getEvent(eventId);
        if (!event.getInitiatorId().equals(userId)) {
            throw new ConflictException("Только инициатор может менять статус заявок");
        }
        if (updateRequest.getStatus() == RequestStatus.CONFIRMED && event.getParticipantLimit() > 0) {
            eventCapacityLock.lock(eventId);
        }
        long confirmedCount = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
        if (updateRequest.getStatus() == RequestStatus.CONFIRMED && event.getParticipantLimit() > 0
                && confirmedCount >= event.getParticipantLimit()) {
            throw new ConflictException("Лимит участников уже достигнут");
        }

        List<ParticipationRequest> requests = requestRepository.findByEventIdAndIdIn(
                eventId, updateRequest.getRequestIds());
        if (requests.size() != updateRequest.getRequestIds().size()) {
            throw new NotFoundException("Некоторые заявки не найдены или не принадлежат событию");
        }

        List<ParticipationRequestDto> confirmed = new ArrayList<>();
        List<ParticipationRequestDto> rejected = new ArrayList<>();
        for (ParticipationRequest request : requests) {
            if (request.getStatus() != RequestStatus.PENDING) {
                throw new ConflictException("Можно менять статус только у заявок со статусом PENDING");
            }
            if (updateRequest.getStatus() == RequestStatus.CONFIRMED) {
                if (event.getParticipantLimit() > 0 && confirmedCount >= event.getParticipantLimit()) {
                    request.setStatus(RequestStatus.REJECTED);
                    rejected.add(toDto(request));
                } else {
                    request.setStatus(RequestStatus.CONFIRMED);
                    confirmedCount++;
                    confirmed.add(toDto(request));
                }
            } else if (updateRequest.getStatus() == RequestStatus.REJECTED) {
                request.setStatus(RequestStatus.REJECTED);
                rejected.add(toDto(request));
            }
        }

        if (updateRequest.getStatus() == RequestStatus.CONFIRMED && event.getParticipantLimit() > 0
                && confirmedCount >= event.getParticipantLimit()) {
            List<ParticipationRequest> remaining = requestRepository.findByEventIdAndStatus(
                    eventId, RequestStatus.PENDING);
            for (ParticipationRequest request : remaining) {
                request.setStatus(RequestStatus.REJECTED);
                rejected.add(toDto(request));
            }
            requests.addAll(remaining);
        }
        requestRepository.saveAll(requests);
        log.info("Обновлены статусы заявок для события {}", eventId);
        return new EventRequestStatusUpdateResult(confirmed, rejected);
    }

    private EventInternalDto getEvent(Long eventId) {
        EventInternalDto event = eventServiceClient.getEvent(eventId);
        if (event == null || event.getId() == null) {
            throw new NotFoundException("Событие с id=" + eventId + " не найдено");
        }
        return event;
    }

    private void getUser(Long userId) {
        if (userServiceClient.getUser(userId) == null) {
            throw new NotFoundException("Пользователь с id=" + userId + " не найден");
        }
    }
}
