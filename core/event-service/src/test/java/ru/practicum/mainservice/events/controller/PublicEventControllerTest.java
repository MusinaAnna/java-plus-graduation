package ru.practicum.mainservice.events.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.common.dto.EventShortDto;
import ru.practicum.mainservice.events.dto.PublicEventQuery;
import ru.practicum.mainservice.events.service.EventService;
import ru.practicum.statistics.client.grpc.UserActionGrpcClient;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class PublicEventControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EventService eventService;

    @MockBean
    private UserActionGrpcClient userActionClient;

    @Test
    void getEvents_shouldReturnListOfEvents() throws Exception {
        List<EventShortDto> events = List.of(createEventShortDto());

        when(eventService.getPublicEvents(eq(new PublicEventQuery(
                null, null, null, null, null, false, null, 0, 10)))).thenReturn(events);

        mockMvc.perform(get("/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L));

        verifyNoInteractions(userActionClient);
    }

    @Test
    void getEvents_shouldUseFirstForwardedAddressWhenBehindGateway() throws Exception {
        when(eventService.getPublicEvents(eq(new PublicEventQuery(
                null, null, null, null, null, false, null, 0, 10)))).thenReturn(List.of());

        mockMvc.perform(get("/events")
                        .header("X-Forwarded-For", "198.51.100.12, 192.0.2.25")
                        .with(request -> {
                            request.setRemoteAddr("10.0.0.5");
                            return request;
                        }))
                .andExpect(status().isOk());

        verifyNoInteractions(userActionClient);
    }

    @Test
    void getEvents_withFilters_shouldReturnFilteredEvents() throws Exception {
        List<EventShortDto> events = List.of(createEventShortDto());

        when(eventService.getPublicEvents(eq(new PublicEventQuery(
                "test", List.of(1L), true, null, null, false, "EVENT_DATE", 0, 10)))).thenReturn(events);

        mockMvc.perform(get("/events")
                        .param("text", "test")
                        .param("categories", "1")
                        .param("paid", "true")
                        .param("sort", "EVENT_DATE"))
                .andExpect(status().isOk());
    }

    @Test
    void getEvent_shouldReturnEvent() throws Exception {
        Long eventId = 1L;

        mockMvc.perform(get("/events/{id}", eventId)
                        .header("X-EWM-USER-ID", "42"))
                .andExpect(status().isOk());

        verify(userActionClient).sendView(42L, 1L);
    }

    @Test
    void missingUserIdHeaderReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/events/{id}", 1L))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(userActionClient);
    }

    private EventShortDto createEventShortDto() {
        EventShortDto dto = new EventShortDto();
        dto.setId(1L);
        dto.setAnnotation("Тестовое событие");
        dto.setTitle("Тестовое событие");
        dto.setPaid(false);
        dto.setEventDate(LocalDateTime.now().plusDays(5));
        return dto;
    }
}
