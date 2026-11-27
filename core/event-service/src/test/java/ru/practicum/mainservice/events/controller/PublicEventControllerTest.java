package ru.practicum.mainservice.events.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.mockito.ArgumentCaptor;
import ru.practicum.common.dto.EventShortDto;
import ru.practicum.dto.EndpointHit;
import ru.practicum.mainservice.events.dto.PublicEventQuery;
import ru.practicum.mainservice.events.service.EventService;
import ru.practicum.statistics.client.controller.StatsClient;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
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
    private StatsClient statsClient;

    @Test
    void getEvents_shouldReturnListOfEvents() throws Exception {
        List<EventShortDto> events = List.of(createEventShortDto());

        when(eventService.getPublicEvents(eq(new PublicEventQuery(
                null, null, null, null, null, false, null, 0, 10)))).thenReturn(events);

        mockMvc.perform(get("/events").with(request -> {
                    request.setRemoteAddr("192.0.2.25");
                    return request;
                }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L));

        ArgumentCaptor<EndpointHit> hitCaptor = ArgumentCaptor.forClass(EndpointHit.class);
        verify(statsClient).sendHit(hitCaptor.capture());
        org.assertj.core.api.Assertions.assertThat(hitCaptor.getValue().getUri()).isEqualTo("/events");
        org.assertj.core.api.Assertions.assertThat(hitCaptor.getValue().getIp()).isEqualTo("192.0.2.25");
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

        ArgumentCaptor<EndpointHit> hitCaptor = ArgumentCaptor.forClass(EndpointHit.class);
        verify(statsClient).sendHit(hitCaptor.capture());
        org.assertj.core.api.Assertions.assertThat(hitCaptor.getValue().getIp()).isEqualTo("198.51.100.12");
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

        mockMvc.perform(get("/events/{id}", eventId))
                .andExpect(status().isOk());

        ArgumentCaptor<EndpointHit> hitCaptor = ArgumentCaptor.forClass(EndpointHit.class);
        verify(statsClient).sendHit(hitCaptor.capture());
        org.assertj.core.api.Assertions.assertThat(hitCaptor.getValue().getUri()).isEqualTo("/events/1");
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
