package ru.practicum.mainservice.events.controller;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.common.dto.EventShortDto;
import ru.practicum.common.dto.LocationDto;
import ru.practicum.common.enums.EventState;
import ru.practicum.mainservice.events.dto.EventFullDto;
import ru.practicum.mainservice.events.dto.NewEventDto;
import ru.practicum.mainservice.events.dto.UpdateEventUserRequest;
import ru.practicum.mainservice.events.service.EventService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class PrivateEventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EventService eventService;

    @Test
    void getUserEvents_ShouldReturnListOfEvents() throws Exception {
        Long userId = 1L;
        List<EventShortDto> events = List.of(createEventShortDto(1L));

        when(eventService.getUserEvents(eq(userId), eq(0), eq(10)))
                .thenReturn(events);

        mockMvc.perform(get("/users/{userId}/events", userId)
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].annotation").value("Test annotation need more simbols"));
    }

    @Test
    void createEvent_ShouldReturnCreatedEvent() throws Exception {
        Long userId = 1L;
        NewEventDto dto = createNewEventDto();
        EventFullDto result = createEventFullDto(1L);

        when(eventService.createEvent(eq(userId), any(NewEventDto.class)))
                .thenReturn(result);

        mockMvc.perform(post("/users/{userId}/events", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.annotation").value("Test annotation need more simbols"));
    }

    @Test
    void getEvent_ShouldReturnEvent() throws Exception {
        Long userId = 1L;
        Long eventId = 1L;
        EventFullDto result = createEventFullDto(eventId);

        when(eventService.getUserEvent(eq(userId), eq(eventId)))
                .thenReturn(result);

        mockMvc.perform(get("/users/{userId}/events/{eventId}", userId, eventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.annotation").value("Test annotation need more simbols"));
    }

    @Test
    void updateEvent_ShouldReturnUpdatedEvent() throws Exception {
        Long userId = 1L;
        Long eventId = 1L;
        UpdateEventUserRequest request = new UpdateEventUserRequest();
        request.setAnnotation("Updated annotation need more simbols");
        EventFullDto result = createEventFullDto(eventId);
        result.setAnnotation("Updated annotation need more simbols");

        when(eventService.updateUserEvent(eq(userId), eq(eventId), any(UpdateEventUserRequest.class)))
                .thenReturn(result);

        mockMvc.perform(patch("/users/{userId}/events/{eventId}", userId, eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.annotation").value("Updated annotation need more simbols"));
    }

    private EventShortDto createEventShortDto(Long id) {
        EventShortDto dto = new EventShortDto();
        dto.setId(id);
        dto.setAnnotation("Test annotation need more simbols");
        dto.setTitle("Test title");
        dto.setPaid(false);
        dto.setEventDate(LocalDateTime.now().plusDays(1));
        return dto;
    }

    private EventFullDto createEventFullDto(Long id) {
        EventFullDto dto = new EventFullDto();
        dto.setId(id);
        dto.setAnnotation("Test annotation need more simbols");
        dto.setTitle("Test title");
        dto.setDescription("Test description need more simbols");
        dto.setPaid(false);
        dto.setParticipantLimit(0);
        dto.setRequestModeration(true);
        dto.setState(EventState.PENDING);
        dto.setEventDate(LocalDateTime.now().plusDays(1));
        dto.setCreatedOn(LocalDateTime.now());
        return dto;
    }

    private NewEventDto createNewEventDto() {
        NewEventDto dto = new NewEventDto();
        dto.setAnnotation("Test annotation need more simbols");
        dto.setTitle("Test title");
        dto.setDescription("Test description need more simbols");
        dto.setCategory(1L);
        dto.setEventDate(LocalDateTime.now().plusDays(2));
        dto.setLocation(new LocationDto(55.75f, 37.62f));
        dto.setPaid(false);
        dto.setParticipantLimit(0);
        dto.setRequestModeration(true);
        return dto;
    }
}
