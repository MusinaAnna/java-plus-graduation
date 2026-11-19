package ru.practicum.mainservice.controller.internal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.common.dto.EventInternalDto;
import ru.practicum.common.dto.EventShortDto;
import ru.practicum.common.enums.EventState;
import ru.practicum.mainservice.events.service.EventService;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventInternalControllerTest {

    @Mock
    private EventService eventService;

    @InjectMocks
    private EventInternalController controller;

    @Test
    void getEventShouldDelegateToEventService() {
        Long eventId = 15L;
        EventInternalDto expected = new EventInternalDto(
                eventId,
                25L,
                35L,
                EventState.PUBLISHED,
                100,
                true,
                LocalDateTime.of(2026, 12, 20, 18, 0)
        );
        when(eventService.getInternalEvent(eventId)).thenReturn(expected);

        EventInternalDto result = controller.getEvent(eventId);

        assertThat(result).isSameAs(expected);
        verify(eventService).getInternalEvent(eventId);
    }

    @Test
    void getEventShortsShouldDelegateToEventService() {
        List<Long> eventIds = List.of(10L, 20L);
        EventShortDto first = new EventShortDto();
        first.setId(10L);
        EventShortDto second = new EventShortDto();
        second.setId(20L);
        List<EventShortDto> expected = List.of(first, second);
        when(eventService.getInternalEventShorts(eventIds)).thenReturn(expected);

        List<EventShortDto> result = controller.getEventShorts(eventIds);

        assertThat(result).containsExactly(first, second);
        verify(eventService).getInternalEventShorts(eventIds);
    }

    @Test
    void existsByCategoryIdShouldReturnServiceResult() {
        when(eventService.existsByCategoryId(30L)).thenReturn(true);
        when(eventService.existsByCategoryId(40L)).thenReturn(false);

        assertThat(controller.existsByCategoryId(30L)).isTrue();
        assertThat(controller.existsByCategoryId(40L)).isFalse();

        verify(eventService).existsByCategoryId(30L);
        verify(eventService).existsByCategoryId(40L);
    }
}
