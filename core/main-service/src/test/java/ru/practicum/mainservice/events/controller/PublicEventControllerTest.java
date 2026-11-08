package ru.practicum.mainservice.events.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.common.dto.EventShortDto;
import ru.practicum.mainservice.events.service.EventService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
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

    @Test
    void getEvents_shouldReturnListOfEvents() throws Exception {
        List<EventShortDto> events = List.of(createEventShortDto());

        when(eventService.getPublicEvents(
                isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(false), isNull(), eq(0), eq(10)
        )).thenReturn(events);

        mockMvc.perform(get("/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L));
    }

    @Test
    void getEvents_withFilters_shouldReturnFilteredEvents() throws Exception {
        List<EventShortDto> events = List.of(createEventShortDto());

        when(eventService.getPublicEvents(
                eq("test"), eq(List.of(1L)), eq(true), any(), any(),
                eq(false), eq("EVENT_DATE"), eq(0), eq(10)
        )).thenReturn(events);

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
