package ru.practicum.mainservice.events.utils;

import org.junit.jupiter.api.Test;
import ru.practicum.common.dto.LocationDto;
import ru.practicum.common.entity.Location;
import ru.practicum.mainservice.events.dto.UpdateEventAdminRequest;
import ru.practicum.mainservice.events.dto.UpdateEventUserRequest;
import ru.practicum.mainservice.events.entity.Event;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class EventUpdateUtilsTest {

    private static final Long ORIGINAL_CATEGORY_ID = 3L;
    private static final Long UPDATED_CATEGORY_ID = 99L;
    private static final LocalDateTime ORIGINAL_EVENT_DATE = LocalDateTime.of(2026, 1, 10, 12, 0);
    private static final LocalDateTime UPDATED_EVENT_DATE = LocalDateTime.of(2026, 2, 20, 18, 30);

    @Test
    void updateFromAdminShouldApplyEveryProvidedField() {
        Event event = createEvent();
        UpdateEventAdminRequest request = new UpdateEventAdminRequest();
        request.setAnnotation("Updated annotation");
        request.setDescription("Updated description");
        request.setTitle("Updated title");
        request.setCategory(UPDATED_CATEGORY_ID);
        request.setLocation(new LocationDto(10.5f, 20.5f));
        request.setEventDate(UPDATED_EVENT_DATE);
        request.setPaid(true);
        request.setParticipantLimit(42);
        request.setRequestModeration(false);

        EventUpdateUtils.updateFromAdmin(event, request, UPDATED_CATEGORY_ID);

        assertThat(event.getAnnotation()).isEqualTo("Updated annotation");
        assertThat(event.getDescription()).isEqualTo("Updated description");
        assertThat(event.getTitle()).isEqualTo("Updated title");
        assertThat(event.getCategoryId()).isEqualTo(UPDATED_CATEGORY_ID);
        assertThat(event.getLocation()).isEqualTo(new Location(10.5f, 20.5f));
        assertThat(event.getEventDate()).isEqualTo(UPDATED_EVENT_DATE);
        assertThat(event.getPaid()).isTrue();
        assertThat(event.getParticipantLimit()).isEqualTo(42);
        assertThat(event.getRequestModeration()).isFalse();
    }

    @Test
    void updateFromAdminShouldKeepExistingFieldsWhenRequestIsEmpty() {
        Event event = createEvent();

        EventUpdateUtils.updateFromAdmin(event, new UpdateEventAdminRequest(), null);

        assertOriginalValues(event);
    }

    @Test
    void updateFromUserShouldApplyEveryProvidedField() {
        Event event = createEvent();
        UpdateEventUserRequest request = new UpdateEventUserRequest();
        request.setAnnotation("Updated annotation");
        request.setDescription("Updated description");
        request.setTitle("Updated title");
        request.setCategory(UPDATED_CATEGORY_ID);
        request.setLocation(new LocationDto(10.5f, 20.5f));
        request.setEventDate(UPDATED_EVENT_DATE);
        request.setPaid(true);
        request.setParticipantLimit(42);
        request.setRequestModeration(false);

        EventUpdateUtils.updateFromUser(event, request, UPDATED_CATEGORY_ID);

        assertThat(event.getAnnotation()).isEqualTo("Updated annotation");
        assertThat(event.getDescription()).isEqualTo("Updated description");
        assertThat(event.getTitle()).isEqualTo("Updated title");
        assertThat(event.getCategoryId()).isEqualTo(UPDATED_CATEGORY_ID);
        assertThat(event.getLocation()).isEqualTo(new Location(10.5f, 20.5f));
        assertThat(event.getEventDate()).isEqualTo(UPDATED_EVENT_DATE);
        assertThat(event.getPaid()).isTrue();
        assertThat(event.getParticipantLimit()).isEqualTo(42);
        assertThat(event.getRequestModeration()).isFalse();
    }

    @Test
    void updateFromUserShouldKeepExistingFieldsWhenRequestIsEmpty() {
        Event event = createEvent();

        EventUpdateUtils.updateFromUser(event, new UpdateEventUserRequest(), null);

        assertOriginalValues(event);
    }

    private Event createEvent() {
        Event event = new Event();
        event.setAnnotation("Original annotation");
        event.setDescription("Original description");
        event.setTitle("Original title");
        event.setCategoryId(ORIGINAL_CATEGORY_ID);
        event.setLocation(new Location(1.5f, 2.5f));
        event.setEventDate(ORIGINAL_EVENT_DATE);
        event.setPaid(false);
        event.setParticipantLimit(10);
        event.setRequestModeration(true);
        return event;
    }

    private void assertOriginalValues(Event event) {
        assertThat(event.getAnnotation()).isEqualTo("Original annotation");
        assertThat(event.getDescription()).isEqualTo("Original description");
        assertThat(event.getTitle()).isEqualTo("Original title");
        assertThat(event.getCategoryId()).isEqualTo(ORIGINAL_CATEGORY_ID);
        assertThat(event.getLocation()).isEqualTo(new Location(1.5f, 2.5f));
        assertThat(event.getEventDate()).isEqualTo(ORIGINAL_EVENT_DATE);
        assertThat(event.getPaid()).isFalse();
        assertThat(event.getParticipantLimit()).isEqualTo(10);
        assertThat(event.getRequestModeration()).isTrue();
    }
}
