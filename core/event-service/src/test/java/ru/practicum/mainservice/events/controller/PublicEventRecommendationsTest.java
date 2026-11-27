package ru.practicum.mainservice.events.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import ru.practicum.common.dto.EventShortDto;
import ru.practicum.common.exception.BadRequestException;
import ru.practicum.mainservice.controller.event.PublicEventController;
import ru.practicum.mainservice.events.dto.EventFullDto;
import ru.practicum.mainservice.events.service.EventService;
import ru.practicum.statistics.client.grpc.UserActionGrpcClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicEventRecommendationsTest {
    @Mock
    private EventService eventService;
    @Mock
    private UserActionGrpcClient actions;
    @InjectMocks
    private PublicEventController controller;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(controller, "userActionClient", actions);
    }

    @Test
    void publishedEventViewSendsRealUserIdFromHeader() {
        EventFullDto event = new EventFullDto();
        when(eventService.getPublicEvent(5L)).thenReturn(event);

        assertEquals(event, controller.getEvent(5L, 12L));
        verify(actions).sendView(12L, 5L);
    }

    @Test
    void invalidUserIdIsRejected() {
        assertThrows(BadRequestException.class, () -> controller.getEvent(5L, -1L));
        verify(actions, never()).sendView(-1L, 5L);
    }

    @Test
    void getEventWithoutConfiguredCollectorFails() {
        ReflectionTestUtils.setField(controller, "userActionClient", null);
        when(eventService.getPublicEvent(5L)).thenReturn(new EventFullDto());
        assertThrows(ru.practicum.common.exception.ServiceUnavailableException.class,
                () -> controller.getEvent(5L, 12L));
    }

    @Test
    void recommendationsAndLikeDelegateToEventService() {
        EventShortDto event = new EventShortDto();
        event.setId(42L);
        when(eventService.getRecommendedEvents(12L, 5)).thenReturn(List.of(event));
        assertEquals(List.of(event), controller.getRecommendations(12L, 5));
        controller.likeEvent(42L, 12L);
        verify(eventService).likeEvent(12L, 42L);
    }
}
