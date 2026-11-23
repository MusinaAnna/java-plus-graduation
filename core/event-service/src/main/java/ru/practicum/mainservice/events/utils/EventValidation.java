package ru.practicum.mainservice.events.utils;

import ru.practicum.common.enums.AdminStateAction;
import ru.practicum.common.enums.EventState;
import ru.practicum.common.exception.BadRequestException;
import ru.practicum.common.exception.ConflictException;
import ru.practicum.common.exception.ValidationException;
import ru.practicum.mainservice.events.dto.NewEventDto;
import ru.practicum.mainservice.events.dto.UpdateEventAdminRequest;
import ru.practicum.mainservice.events.dto.UpdateEventUserRequest;
import ru.practicum.mainservice.events.entity.Event;

import java.time.LocalDateTime;

/** Validates event invariants at creation and update, never on a read request. */
public final class EventValidation {
    private EventValidation() {
    }

    public static void onCreate(NewEventDto dto) {
        if (dto.getDescription() == null || dto.getDescription().isBlank()) {
            throw new ValidationException("description must not be blank");
        }
        if (dto.getAnnotation() == null || dto.getAnnotation().isBlank()) {
            throw new ValidationException("annotation must not be blank");
        }
        participantLimit(dto.getParticipantLimit());
        if (dto.getEventDate() == null || dto.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new BadRequestException("Event date must be at least 2 hours from now");
        }
    }

    public static void onAdminUpdate(Event event, UpdateEventAdminRequest request) {
        if (request.getStateAction() == AdminStateAction.PUBLISH_EVENT) {
            if (event.getState() != EventState.PENDING) {
                throw new ConflictException("Cannot publish the event because it's not in PENDING state");
            }
            LocalDateTime effectiveDate = request.getEventDate() == null
                    ? event.getEventDate() : request.getEventDate();
            if (effectiveDate == null || effectiveDate.isBefore(LocalDateTime.now().plusHours(1))) {
                throw new ConflictException("Event date must be at least 1 hour from now");
            }
        }
        if (request.getStateAction() == AdminStateAction.REJECT_EVENT
                && event.getState() == EventState.PUBLISHED) {
            throw new ConflictException("Cannot reject published event");
        }
        participantLimit(request.getParticipantLimit() == null
                ? event.getParticipantLimit() : request.getParticipantLimit());
    }

    public static void onUserUpdate(Event event, UpdateEventUserRequest request) {
        if (request.getEventDate() != null
                && request.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new BadRequestException("Event date must be at least 2 hours from now");
        }
        participantLimit(request.getParticipantLimit() == null
                ? event.getParticipantLimit() : request.getParticipantLimit());
    }

    private static void participantLimit(Integer limit) {
        if (limit != null && limit < 0) {
            throw new BadRequestException("Participant limit must not be negative");
        }
    }
}
