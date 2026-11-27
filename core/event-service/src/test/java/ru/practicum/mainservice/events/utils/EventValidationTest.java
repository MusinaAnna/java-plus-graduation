package ru.practicum.mainservice.events.utils;

import org.junit.jupiter.api.Test;
import ru.practicum.common.enums.AdminStateAction;
import ru.practicum.common.enums.EventState;
import ru.practicum.common.exception.BadRequestException;
import ru.practicum.common.exception.ConflictException;
import ru.practicum.mainservice.events.dto.NewEventDto;
import ru.practicum.mainservice.events.dto.UpdateEventAdminRequest;
import ru.practicum.mainservice.events.dto.UpdateEventUserRequest;
import ru.practicum.mainservice.events.entity.Event;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EventValidationTest {
    @Test
    void publishingWithoutDateInPatchMustValidateStoredDate() {
        Event event = new Event();
        event.setState(EventState.PENDING);
        event.setEventDate(LocalDateTime.now().plusMinutes(30));
        UpdateEventAdminRequest patch = new UpdateEventAdminRequest();
        patch.setStateAction(AdminStateAction.PUBLISH_EVENT);
        assertThatThrownBy(() -> EventValidation.onAdminUpdate(event, patch))
                .isInstanceOf(ConflictException.class);
        event.setEventDate(LocalDateTime.now().plusDays(2));
        assertThatCode(() -> EventValidation.onAdminUpdate(event, patch)).doesNotThrowAnyException();
    }

    @Test
    void publishingUsesEffectiveUpdatedDateWhenProvided() {
        Event event = new Event();
        event.setState(EventState.PENDING);
        event.setEventDate(LocalDateTime.now().minusDays(1));
        UpdateEventAdminRequest patch = new UpdateEventAdminRequest();
        patch.setStateAction(AdminStateAction.PUBLISH_EVENT);
        patch.setEventDate(LocalDateTime.now().plusDays(2));
        assertThatCode(() -> EventValidation.onAdminUpdate(event, patch)).doesNotThrowAnyException();
    }

    @Test
    void adminUpdateMustValidateStoredLimitWhenNotProvided() {
        Event event = new Event();
        event.setState(EventState.PENDING);
        event.setParticipantLimit(-1);
        UpdateEventAdminRequest patch = new UpdateEventAdminRequest();
        assertThatThrownBy(() -> EventValidation.onAdminUpdate(event, patch))
                .isInstanceOf(BadRequestException.class);
        event.setParticipantLimit(0);
        assertThatCode(() -> EventValidation.onAdminUpdate(event, patch))
                .doesNotThrowAnyException();
    }

    @Test
    void participantLimitCannotBeNegativeAtCreationOrUpdate() {
        NewEventDto dto = new NewEventDto();
        dto.setAnnotation("Тестовая аннотация события");
        dto.setDescription("Достаточно подробное описание для теста");
        dto.setParticipantLimit(-1);
        dto.setEventDate(LocalDateTime.now().plusDays(2));
        assertThatThrownBy(() -> EventValidation.onCreate(dto))
                .isInstanceOf(BadRequestException.class);

        Event event = new Event();
        event.setParticipantLimit(0);
        UpdateEventUserRequest patch = new UpdateEventUserRequest();
        patch.setParticipantLimit(-2);
        assertThatThrownBy(() -> EventValidation.onUserUpdate(event, patch))
                .isInstanceOf(BadRequestException.class);
        event.setParticipantLimit(-3);
        patch.setParticipantLimit(null);
        assertThatThrownBy(() -> EventValidation.onUserUpdate(event, patch))
                .isInstanceOf(BadRequestException.class);
    }
}
