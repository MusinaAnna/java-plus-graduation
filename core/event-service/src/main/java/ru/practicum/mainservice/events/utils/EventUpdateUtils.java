package ru.practicum.mainservice.events.utils;

import ru.practicum.common.entity.Location;
import ru.practicum.mainservice.events.dto.UpdateEventAdminRequest;
import ru.practicum.mainservice.events.dto.UpdateEventUserRequest;
import ru.practicum.mainservice.events.entity.Event;

/**
 * Утилитный класс для обновления событий.
 * Предоставляет методы для безопасного обновления полей события
 * из административного и пользовательского запросов.
 * <p>
 * Обновляются только переданные поля (не-null значения).
 * </p>
 */
public class EventUpdateUtils {


    public static void updateFromAdmin(Event event, UpdateEventAdminRequest request, Long categoryId) {
        if (request.getAnnotation() != null) {
            event.setAnnotation(request.getAnnotation());
        }
        if (request.getDescription() != null) {
            event.setDescription(request.getDescription());
        }
        if (request.getTitle() != null) {
            event.setTitle(request.getTitle());
        }
        if (categoryId != null) {
            event.setCategoryId(categoryId);
        }
        if (request.getLocation() != null) {
            event.setLocation(
                    new Location(
                            request.getLocation().getLat(),
                            request.getLocation().getLon()
                    )
            );
        }
        if (request.getEventDate() != null) {
            event.setEventDate(request.getEventDate());
        }
        if (request.getPaid() != null) {
            event.setPaid(request.getPaid());
        }
        if (request.getParticipantLimit() != null) {
            event.setParticipantLimit(request.getParticipantLimit());
        }
        if (request.getRequestModeration() != null) {
            event.setRequestModeration(request.getRequestModeration());
        }
    }


    public static void updateFromUser(Event event, UpdateEventUserRequest request, Long categoryId) {
        if (request.getAnnotation() != null) {
            event.setAnnotation(request.getAnnotation());
        }
        if (request.getDescription() != null) {
            event.setDescription(request.getDescription());
        }
        if (request.getTitle() != null) {
            event.setTitle(request.getTitle());
        }
        if (categoryId != null) {
            event.setCategoryId(categoryId);
        }
        if (request.getLocation() != null) {
            event.setLocation(
                    new Location(
                            request.getLocation().getLat(),
                            request.getLocation().getLon()
                    )
            );
        }
        if (request.getEventDate() != null) {
            event.setEventDate(request.getEventDate());
        }
        if (request.getPaid() != null) {
            event.setPaid(request.getPaid());
        }
        if (request.getParticipantLimit() != null) {
            event.setParticipantLimit(request.getParticipantLimit());
        }
        if (request.getRequestModeration() != null) {
            event.setRequestModeration(request.getRequestModeration());
        }
    }
}