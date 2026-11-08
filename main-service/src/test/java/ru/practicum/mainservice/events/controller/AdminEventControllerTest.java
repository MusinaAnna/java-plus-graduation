package ru.practicum.mainservice.events.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.common.dto.CategoryDto;
import ru.practicum.common.dto.LocationDto;
import ru.practicum.common.dto.UserShortDto;
import ru.practicum.common.entity.Category;
import ru.practicum.common.entity.Location;
import ru.practicum.common.entity.User;
import ru.practicum.common.enums.AdminStateAction;
import ru.practicum.common.enums.EventState;
import ru.practicum.mainservice.events.dto.EventFullDto;
import ru.practicum.mainservice.events.dto.UpdateEventAdminRequest;
import ru.practicum.mainservice.events.service.EventService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AdminEventControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EventService eventService;

    private EventFullDto eventFullDto;
    private UpdateEventAdminRequest updateRequest;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();

        Category category = new Category();
        category.setId(1L);
        category.setName("Концерты");

        CategoryDto categoryDto = new CategoryDto();
        categoryDto.setId(category.getId());
        categoryDto.setName(category.getName());

        User initiator = new User();
        initiator.setId(1L);
        initiator.setName("Иван Петров");
        initiator.setEmail("ivan@mail.ru");

        UserShortDto initiatorDto = new UserShortDto();
        initiatorDto.setId(initiator.getId());
        initiatorDto.setName(initiator.getName());

        Location location = new Location();
        location.setLat(55.754167f);
        location.setLon(37.62f);

        LocationDto locationDto = new LocationDto();
        locationDto.setLat(location.getLat());
        locationDto.setLon(location.getLon());

        eventFullDto = EventFullDto.builder()
                .id(1L)
                .annotation("Тестовое событие")
                .description("Полное описание тестового события")
                .title("Тестовое событие")
                .category(categoryDto)
                .initiator(initiatorDto)
                .location(locationDto)
                .eventDate(now.plusDays(5))
                .createdOn(now)
                .publishedOn(null)
                .state(EventState.PENDING)
                .paid(false)
                .participantLimit(10)
                .requestModeration(true)
                .confirmedRequests(0L)
                .views(0L)
                .build();

        updateRequest = UpdateEventAdminRequest.builder()
                .annotation("Обновленная аннотация")
                .description("Обновленное описание")
                .title("Обновленное название")
                .category(1L)
                .eventDate(now.plusDays(10))
                .paid(true)
                .participantLimit(20)
                .requestModeration(false)
                .stateAction(AdminStateAction.PUBLISH_EVENT)
                .build();
    }

    @Test
    void getEvents_shouldReturnListOfEvents() throws Exception {
        List<EventFullDto> events = List.of(eventFullDto);

        when(eventService.getAdminEvents(
                isNull(), isNull(), isNull(), isNull(), isNull(), anyInt(), anyInt()
        )).thenReturn(events);

        mvc.perform(get("/admin/events")
                        .param("from", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].annotation").value("Тестовое событие"))
                .andExpect(jsonPath("$[0].state").value("PENDING"))
                .andExpect(jsonPath("$[0].category.name").value("Концерты"));
    }

    @Test
    void getEvents_withAllFilters_shouldReturnFilteredEvents() throws Exception {
        List<EventFullDto> events = List.of(eventFullDto);

        when(eventService.getAdminEvents(
                anyList(), anyList(), anyList(), any(), any(), anyInt(), anyInt()
        )).thenReturn(events);

        mvc.perform(get("/admin/events")
                        .param("users", "1", "2")
                        .param("states", "PENDING", "PUBLISHED")
                        .param("categories", "1", "2")
                        .param("rangeStart", now.minusDays(1).toString())
                        .param("rangeEnd", now.plusDays(10).toString())
                        .param("from", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L));
    }

    @Test
    void getEvents_withDefaultPagination_shouldReturnEvents() throws Exception {
        List<EventFullDto> events = List.of(eventFullDto);

        when(eventService.getAdminEvents(
                isNull(), isNull(), isNull(), isNull(), isNull(), eq(0), eq(10)
        )).thenReturn(events);

        mvc.perform(get("/admin/events")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L));
    }

    @Test
    void updateEvent_shouldReturnUpdatedEvent() throws Exception {
        EventFullDto updatedEvent = EventFullDto.builder()
                .id(1L)
                .annotation("Обновленная аннотация")
                .description("Обновленное описание")
                .title("Обновленное название")
                .state(EventState.PUBLISHED)
                .build();

        when(eventService.updateAdminEvent(eq(1L), any(UpdateEventAdminRequest.class)))
                .thenReturn(updatedEvent);

        mvc.perform(patch("/admin/events/{eventId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.annotation").value("Обновленная аннотация"))
                .andExpect(jsonPath("$.state").value("PUBLISHED"));
    }

    @Test
    void updateEvent_withInvalidRequest_shouldReturnBadRequest() throws Exception {
        UpdateEventAdminRequest invalidRequest = UpdateEventAdminRequest.builder()
                .annotation("") // пустая аннотация
                .build();

        mvc.perform(patch("/admin/events/{eventId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateEvent_withNullStateAction_shouldStillUpdate() throws Exception {
        UpdateEventAdminRequest requestWithoutState = UpdateEventAdminRequest.builder()
                .annotation("Новая аннотация для события с валидной длиной")
                .build();

        when(eventService.updateAdminEvent(eq(1L), any(UpdateEventAdminRequest.class)))
                .thenReturn(eventFullDto);

        mvc.perform(patch("/admin/events/{eventId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestWithoutState)))
                .andExpect(status().isOk());
    }
}
