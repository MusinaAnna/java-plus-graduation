package ru.practicum.mainservice.requests.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.common.entity.User;
import ru.practicum.common.enums.EventState;
import ru.practicum.common.enums.RequestStatus;
import ru.practicum.common.exception.ConflictException;
import ru.practicum.common.exception.NotFoundException;
import ru.practicum.mainservice.events.entity.Event;
import ru.practicum.mainservice.events.service.EventService;
import ru.practicum.mainservice.requests.dto.EventRequestStatusUpdateRequest;
import ru.practicum.mainservice.requests.dto.EventRequestStatusUpdateResult;
import ru.practicum.mainservice.requests.dto.ParticipationRequestDto;
import ru.practicum.mainservice.requests.entity.ParticipationRequest;
import ru.practicum.mainservice.requests.repository.ParticipationRequestRepository;
import ru.practicum.mainservice.requests.service.ParticipationRequestService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ParticipationRequestServiceImpl implements ParticipationRequestService {

    private final ParticipationRequestRepository requestRepository;
    private final EventService eventService;

    private ParticipationRequestDto toDto(ParticipationRequest request) {
        return new ParticipationRequestDto(
                request.getId(),
                request.getEvent().getId(),
                request.getRequester().getId(),
                request.getStatus(),
                request.getCreated()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParticipationRequestDto> getUserRequests(Long userId) {
        return requestRepository.findByRequesterId(userId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public ParticipationRequestDto addRequest(Long userId, Long eventId) {
        Event event = eventService.getEventEntity(eventId);

        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Нельзя участвовать в неопубликованном событии");
        }
        if (event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Инициатор события не может подать заявку на участие");
        }
        if (requestRepository.findByEventIdAndRequesterId(eventId, userId).isPresent()) {
            throw new ConflictException("Запрос на участие уже существует");
        }
        long confirmedCount = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
        if (event.getParticipantLimit() > 0 && confirmedCount >= event.getParticipantLimit()) {
            throw new ConflictException("Достигнут лимит участников");
        }

        ParticipationRequest request = new ParticipationRequest();
        request.setEvent(event);

        User requester = new User();
        requester.setId(userId);
        request.setRequester(requester);

        if (!event.getRequestModeration() || event.getParticipantLimit() == 0) {
            request.setStatus(RequestStatus.CONFIRMED);
        } else {
            request.setStatus(RequestStatus.PENDING);
        }
        request.setCreated(LocalDateTime.now());

        ParticipationRequest saved = requestRepository.save(request);
        log.info("Добавлен запрос на участие: {}", saved.getId());
        return toDto(saved);
    }

    @Override
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        ParticipationRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Запрос не найден"));
        if (!request.getRequester().getId().equals(userId)) {
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
        Event event = eventService.getEventEntity(eventId);
        if (!event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Только инициатор события может видеть заявки");
        }
        return requestRepository.findByEventId(eventId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public EventRequestStatusUpdateResult updateRequestStatus(Long userId, Long eventId,
                                                              EventRequestStatusUpdateRequest updateRequest) {
        Event event = eventService.getEventEntity(eventId);
        if (!event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Только инициатор может менять статус заявок");
        }

        long confirmedCount = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
        if (event.getParticipantLimit() > 0 && confirmedCount >= event.getParticipantLimit()) {
            throw new ConflictException("Лимит участников уже достигнут");
        }

        List<ParticipationRequest> requests = requestRepository.findByEventIdAndIdIn(eventId, updateRequest.getRequestIds());
        if (requests.size() != updateRequest.getRequestIds().size()) {
            throw new NotFoundException("Некоторые заявки не найдены или не принадлежат событию");
        }

        List<ParticipationRequestDto> confirmed = new ArrayList<>();
        List<ParticipationRequestDto> rejected = new ArrayList<>();

        for (ParticipationRequest req : requests) {
            if (!req.getStatus().equals(RequestStatus.PENDING)) {
                throw new ConflictException("Можно менять статус только у заявок со статусом PENDING");
            }

            if (updateRequest.getStatus() == RequestStatus.CONFIRMED) {
                if (event.getParticipantLimit() > 0 && confirmedCount >= event.getParticipantLimit()) {
                    req.setStatus(RequestStatus.REJECTED);
                    rejected.add(toDto(req));
                } else {
                    req.setStatus(RequestStatus.CONFIRMED);
                    confirmedCount++;
                    confirmed.add(toDto(req));
                }
            } else if (updateRequest.getStatus() == RequestStatus.REJECTED) {
                req.setStatus(RequestStatus.REJECTED);
                rejected.add(toDto(req));
            }
        }

        if (event.getParticipantLimit() > 0 && confirmedCount >= event.getParticipantLimit()) {
            List<ParticipationRequest> remaining = requestRepository.findByEventIdAndStatus(eventId, RequestStatus.PENDING);
            for (ParticipationRequest req : remaining) {
                req.setStatus(RequestStatus.REJECTED);
                rejected.add(toDto(req));
            }
        }

        requestRepository.saveAll(requests);
        log.info("Обновлены статусы заявок для события {}", eventId);
        return new EventRequestStatusUpdateResult(confirmed, rejected);
    }
}
