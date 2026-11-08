package ru.practicum.mainservice.events.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import ru.practicum.common.dto.CategoryDto;
import ru.practicum.common.dto.EventShortDto;
import ru.practicum.common.dto.LocationDto;
import ru.practicum.common.dto.UserShortDto;
import ru.practicum.common.entity.Category;
import ru.practicum.common.entity.Location;
import ru.practicum.common.entity.User;
import ru.practicum.common.enums.AdminStateAction;
import ru.practicum.common.enums.EventState;
import ru.practicum.common.enums.UserStateAction;
import ru.practicum.common.exception.BadRequestException;
import ru.practicum.common.exception.ConflictException;
import ru.practicum.common.exception.NotFoundException;
import ru.practicum.dto.EndpointHit;
import ru.practicum.mainservice.categories.repository.CategoryRepository;
import ru.practicum.mainservice.categories.service.CategoryService;
import ru.practicum.mainservice.events.dto.EventFullDto;
import ru.practicum.mainservice.events.dto.NewEventDto;
import ru.practicum.mainservice.events.dto.UpdateEventAdminRequest;
import ru.practicum.mainservice.events.dto.UpdateEventUserRequest;
import ru.practicum.mainservice.events.entity.Event;
import ru.practicum.mainservice.events.mapper.EventMapper;
import ru.practicum.mainservice.events.repository.EventRepository;
import ru.practicum.mainservice.events.repository.EventRepositoryCustom;
import ru.practicum.mainservice.requests.repository.ParticipationRequestRepository;
import ru.practicum.mainservice.users.service.UserService;
import ru.practicum.statistics.client.controller.StatsClient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EventServiceImplTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventRepositoryCustom eventRepositoryCustom;

    @Mock
    private ParticipationRequestRepository participationRequestRepository;

    @Mock
    private EventMapper eventMapper;

    @Mock
    private StatsClient statsClient;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private EventServiceImpl eventService;

    @Mock
    private UserService userService;

    @Mock
    private CategoryService categoryService;

    private Event event;
    private EventFullDto eventFullDto;
    private Category category;
    private User initiator;
    private UpdateEventAdminRequest updateRequest;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();

        category = new Category();
        category.setId(1L);
        category.setName("Концерты");

        CategoryDto categoryDto = new CategoryDto();
        categoryDto.setId(category.getId());
        categoryDto.setName(category.getName());

        initiator = new User();
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

        event = new Event();
        event.setId(1L);
        event.setAnnotation("Новая аннотация для тестового события");
        event.setDescription("Новое описание для проверки работы метода");
        event.setTitle("Тестовое событие");
        event.setCategory(category);
        event.setInitiator(initiator);
        event.setLocation(location);
        event.setEventDate(now.plusDays(5));
        event.setCreatedOn(now);
        event.setPublishedOn(null);
        event.setState(EventState.PENDING);
        event.setPaid(false);
        event.setParticipantLimit(10);
        event.setRequestModeration(true);

        eventFullDto = EventFullDto.builder()
                .id(1L)
                .annotation("Новая аннотация для тестового события")
                .description("Новое описание для проверки работы метода")
                .title("Тестовое событие")
                .category(categoryDto)
                .initiator(initiatorDto)
                .location(locationDto)
                .eventDate(now.plusDays(5))
                .createdOn(now)
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
    void getAdminEvents_shouldReturnListOfEvents() {
        when(eventRepositoryCustom.findAllByAdminFilters(
                any(), any(), any(), any(), any(), any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(event)));

        when(eventMapper.toFullDto(event))
                .thenReturn(eventFullDto);

        List<EventFullDto> result = eventService.getAdminEvents(
                null, null, null, null,
                null, 0, 10
        );

        assertThat(result).containsExactly(eventFullDto);

        verify(eventRepositoryCustom).findAllByAdminFilters(
                any(), any(), any(), any(), any(), any(Pageable.class)
        );

        verify(eventMapper).toFullDto(event);
    }

    @Test
    void getAdminEvents_withFilters_shouldReturnFilteredEvents() {
        when(eventRepositoryCustom.findAllByAdminFilters(
                any(), any(), any(), any(), any(), any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(event)));

        when(eventMapper.toFullDto(event))
                .thenReturn(eventFullDto);

        List<EventFullDto> result = eventService.getAdminEvents(
                List.of(1L, 2L),
                List.of(EventState.PENDING),
                List.of(1L),
                now.minusDays(1),
                now.plusDays(10),
                0,
                10
        );

        assertThat(result).containsExactly(eventFullDto);

        verify(eventRepositoryCustom).findAllByAdminFilters(
                any(), any(), any(), any(), any(), any(Pageable.class)
        );
    }

    @Test
    void getAdminEvents_shouldHandleEmptyResult() {
        when(eventRepositoryCustom.findAllByAdminFilters(
                any(), any(), any(), any(), any(), any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of()));

        List<EventFullDto> result = eventService.getAdminEvents(
                null, null, null, null,
                null, 0, 10
        );

        assertThat(result).isEmpty();
    }


    @Test
    void updateAdminEvent_shouldThrowNotFoundException_whenEventNotFound() {
        when(eventRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.updateAdminEvent(999L, updateRequest))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Event with id 999 was not found");
    }

    @Test
    void updateAdminEvent_shouldThrowConflictException_whenPublishingNonPendingEvent() {
        event.setState(EventState.CANCELED);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> eventService.updateAdminEvent(1L, updateRequest))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Cannot publish the event because it's not in PENDING state");
    }

    @Test
    void updateAdminEvent_shouldThrowConflictException_whenEventDateTooSoon() {
        updateRequest.setEventDate(now.plusMinutes(30));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> eventService.updateAdminEvent(1L, updateRequest))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Event date must be at least 1 hour from now");
    }

    @Test
    void updateAdminEvent_shouldThrowConflictException_whenRejectingPublishedEvent() {
        event.setState(EventState.PUBLISHED);
        updateRequest.setStateAction(AdminStateAction.REJECT_EVENT);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> eventService.updateAdminEvent(1L, updateRequest))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Cannot reject published event");
    }

    @Test
    void updateAdminEvent_shouldUpdateWithoutStateAction() {
        String newAnnotation = "Новая аннотация для тестового события";
        String newDescription = "Новое описание для проверки работы метода";

        UpdateEventAdminRequest requestWithoutState = UpdateEventAdminRequest.builder()
                .annotation(newAnnotation)
                .description(newDescription)
                .build();

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(eventRepository.save(event)).thenReturn(event);
        when(eventMapper.toFullDto(event)).thenReturn(eventFullDto);

        lenient().doAnswer(invocation -> {
            UpdateEventAdminRequest req = invocation.getArgument(0);
            Event eventToUpdate = invocation.getArgument(2);

            if (req.getAnnotation() != null) {
                eventToUpdate.setAnnotation(req.getAnnotation());
            }
            if (req.getDescription() != null) {
                eventToUpdate.setDescription(req.getDescription());
            }
            return null;
        }).when(eventMapper).updateFromAdmin(
                any(UpdateEventAdminRequest.class),
                any(Category.class),
                any(Event.class)
        );

        EventFullDto result = eventService.updateAdminEvent(1L, requestWithoutState);

        assertThat(result).isNotNull();
        assertThat(event.getState()).isEqualTo(EventState.PENDING);
        assertThat(event.getAnnotation()).isEqualTo(newAnnotation);
        assertThat(event.getDescription()).isEqualTo(newDescription);
        verify(eventRepository).save(event);
    }

    @Test
    void getEventEntity_shouldReturnEvent_whenExists() {
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        Event result = eventService.getEventEntity(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void getEventEntity_shouldThrowNotFoundException_whenNotExists() {
        when(eventRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.getEventEntity(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Event with id 999 was not found");
    }

    @Test
    void getUserEvents_shouldReturnListOfEvents() {
        Long userId = 1L;
        Page<Event> eventPage = new PageImpl<>(List.of(event));

        when(eventRepository.findByInitiatorId(eq(userId), any(PageRequest.class)))
                .thenReturn(eventPage);
        when(eventMapper.toShortDto(any(Event.class)))
                .thenReturn(createEventShortDto(event));

        List<EventShortDto> result = eventService.getUserEvents(userId, 0, 10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        verify(eventRepository).findByInitiatorId(eq(userId), any(PageRequest.class));
    }

    @Test
    void getUserEvents_shouldReturnEmpty_whenUserHasNoEvents() {
        Long userId = 1L;
        Page<Event> emptyPage = new PageImpl<>(List.of());

        when(eventRepository.findByInitiatorId(eq(userId), any(PageRequest.class)))
                .thenReturn(emptyPage);

        List<EventShortDto> result = eventService.getUserEvents(userId, 0, 10);

        assertThat(result).isEmpty();
        verify(eventRepository).findByInitiatorId(eq(userId), any(PageRequest.class));
    }

    @Test
    void createEvent_shouldCreateEventSuccessfully() {
        Long userId = 1L;
        NewEventDto newEventDto = createNewEventDto();
        Event newEvent = createNewEvent();

        when(categoryService.getCategoryEntity(1L)).thenReturn(category);
        when(userService.getUserEntity(userId)).thenReturn(initiator);
        when(eventMapper.toEntity(any(NewEventDto.class), any(Category.class), any(User.class)))
                .thenReturn(newEvent);
        when(eventRepository.save(any(Event.class))).thenReturn(event);
        when(eventMapper.toFullDto(any(Event.class))).thenReturn(eventFullDto);

        EventFullDto result = eventService.createEvent(userId, newEventDto);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(eventRepository).save(any(Event.class));
    }

    @Test
    void createEvent_shouldThrowBadRequest_whenEventDateTooSoon() {
        Long userId = 1L;
        NewEventDto newEventDto = createNewEventDto();
        newEventDto.setEventDate(LocalDateTime.now().plusMinutes(30));

        when(userService.getUserEntity(userId)).thenReturn(initiator);
        when(categoryService.getCategoryEntity(1L)).thenReturn(category);

        assertThatThrownBy(() -> eventService.createEvent(userId, newEventDto))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Event date must be at least 2 hours from now");

        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void createEvent_shouldThrowNotFoundException_whenCategoryNotFound() {
        Long userId = 1L;
        NewEventDto newEventDto = createNewEventDto();

        when(userService.getUserEntity(userId)).thenReturn(initiator);
        when(categoryService.getCategoryEntity(1L))
                .thenThrow(new NotFoundException("Category with id 1 was not found"));

        assertThatThrownBy(() -> eventService.createEvent(userId, newEventDto))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Category with id 1 was not found");

        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void getUserEvent_shouldReturnEvent_whenUserIsInitiator() {
        Long userId = 1L;
        Long eventId = 1L;

        when(userService.getUserEntity(userId)).thenReturn(initiator);
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(eventMapper.toFullDto(event)).thenReturn(eventFullDto);

        EventFullDto result = eventService.getUserEvent(userId, eventId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(eventRepository).findById(eventId);
    }

    @Test
    void getUserEvent_shouldThrowNotFoundException_whenEventNotFound() {
        Long userId = 1L;
        Long eventId = 999L;

        when(userService.getUserEntity(userId)).thenReturn(initiator);
        when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.getUserEvent(userId, eventId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Event with id 999 was not found");
    }

    @Test
    void getUserEvent_shouldThrowNotFoundException_whenUserIsNotInitiator() {
        Long userId = 2L;
        Long eventId = 1L;
        User otherUser = new User();
        otherUser.setId(2L);
        otherUser.setName("Другой пользователь");

        when(userService.getUserEntity(userId)).thenReturn(otherUser);
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> eventService.getUserEvent(userId, eventId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Event with id 1 not found for user 2");
    }


    @Test
    void updateUserEvent_shouldThrowNotFoundException_whenUserIsNotInitiator() {
        Long userId = 2L;
        Long eventId = 1L;
        UpdateEventUserRequest request = createUpdateEventUserRequest();
        User otherUser = new User();
        otherUser.setId(2L);

        when(userService.getUserEntity(userId)).thenReturn(otherUser);
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> eventService.updateUserEvent(userId, eventId, request))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Event with id 1 not found for user 2");

        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void updateUserEvent_shouldThrowConflictException_whenEventIsPublished() {
        Long userId = 1L;
        Long eventId = 1L;
        UpdateEventUserRequest request = createUpdateEventUserRequest();

        event.setState(EventState.PUBLISHED);

        when(userService.getUserEntity(userId)).thenReturn(initiator);
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> eventService.updateUserEvent(userId, eventId, request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Only pending or canceled events can be changed");

        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void updateUserEvent_shouldThrowBadRequest_whenEventDateTooSoon() {
        Long userId = 1L;
        Long eventId = 1L;
        UpdateEventUserRequest request = createUpdateEventUserRequest();
        request.setEventDate(LocalDateTime.now().plusMinutes(30));

        when(userService.getUserEntity(userId)).thenReturn(initiator);
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> eventService.updateUserEvent(userId, eventId, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Event date must be at least 2 hours from now");

        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void updateUserEvent_shouldChangeStatusToPending_whenSendToReview() {
        Long userId = 1L;
        Long eventId = 1L;
        UpdateEventUserRequest request = createUpdateEventUserRequest();
        request.setStateAction(UserStateAction.SEND_TO_REVIEW);

        event.setState(EventState.CANCELED);

        when(userService.getUserEntity(userId)).thenReturn(initiator);
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(eventRepository.save(event)).thenReturn(event);
        when(eventMapper.toFullDto(event)).thenReturn(eventFullDto);

        eventService.updateUserEvent(userId, eventId, request);

        assertThat(event.getState()).isEqualTo(EventState.PENDING);
        verify(eventRepository).save(event);
    }

    @Test
    void updateUserEvent_shouldChangeStatusToCanceled_whenCancelReview() {
        Long userId = 1L;
        Long eventId = 1L;
        UpdateEventUserRequest request = createUpdateEventUserRequest();
        request.setStateAction(UserStateAction.CANCEL_REVIEW);

        event.setState(EventState.PENDING);

        when(userService.getUserEntity(userId)).thenReturn(initiator);
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(eventRepository.save(event)).thenReturn(event);
        when(eventMapper.toFullDto(event)).thenReturn(eventFullDto);

        eventService.updateUserEvent(userId, eventId, request);

        assertThat(event.getState()).isEqualTo(EventState.CANCELED);
        verify(eventRepository).save(event);
    }

    @Test
    void getPublicEvents_shouldReturnPublishedEvents() {
        EventShortDto eventShortDto = createEventShortDto(event);

        when(eventRepositoryCustom.findPublishedEvents(
                any(), any(), any(), any(), any(), any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(event)));

        when(eventMapper.toShortDto(event))
                .thenReturn(eventShortDto);

        List<EventShortDto> result = eventService.getPublicEvents(
                null, null, null, null, null, false,
                null, 0, 10
        );

        assertThat(result).containsExactly(eventShortDto);

        verify(eventRepositoryCustom).findPublishedEvents(
                any(), any(), any(), any(), any(), any(Pageable.class)
        );
    }

    @Test
    void getPublicEvents_shouldApplyDateRangeStart_whenNull() {
        when(eventRepositoryCustom.findPublishedEvents(
                any(), any(), any(), any(), any(), any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of()));

        eventService.getPublicEvents(null, null, null, null,
                null, false, null, 0, 10
        );

        verify(eventRepositoryCustom).findPublishedEvents(
                any(), any(), any(), any(), any(), any(Pageable.class)
        );
    }

    @Test
    void getPublicEvents_shouldFilterByText() {
        when(eventRepositoryCustom.findPublishedEvents(
                any(), any(), any(), any(), any(), any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of()));

        eventService.getPublicEvents(
                "test", null, null, null, null,
                false, null, 0, 10
        );

        verify(eventRepositoryCustom).findPublishedEvents(
                any(), any(), any(), any(), any(), any(Pageable.class)
        );
    }

    @Test
    void getPublicEvents_shouldFilterByPaid() {
        when(eventRepositoryCustom.findPublishedEvents(
                any(), any(), any(), any(), any(), any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of()));

        eventService.getPublicEvents(
                null, null, true, null, null,
                false, null, 0, 10
        );

        verify(eventRepositoryCustom).findPublishedEvents(
                any(), any(), any(), any(), any(), any(Pageable.class)
        );
    }

    @Test
    void getPublicEvents_shouldUseSpecificationWhenPaidIsSpecified() {
        when(eventRepositoryCustom.findPublishedEvents(
                any(), any(), any(), any(), any(), any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of()));

        eventService.getPublicEvents(
                null, null, true,
                null, null,
                false, null,
                0, 10
        );

        verify(eventRepositoryCustom).findPublishedEvents(
                any(), any(), any(), any(), any(), any(Pageable.class)
        );
    }

    @Test
    void getPublicEvents_shouldSortByEventDate_whenSortIsNull() {
        when(eventRepositoryCustom.findPublishedEvents(
                any(), any(), any(), any(), any(), any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of()));

        eventService.getPublicEvents(
                null,
                null,
                null,
                null,
                null,
                false,
                null,
                0,
                10
        );

        Pageable expectedPageable = PageRequest.of(
                0,
                10,
                Sort.by("eventDate").ascending()
        );

        verify(eventRepositoryCustom).findPublishedEvents(
                any(), any(), any(), any(), any(), eq(PageRequest.of(0, 10, Sort.by("eventDate").ascending()))
        );
    }

    @Test
    void getPublicEvent_shouldReturnEvent_whenPublished() {
        Long eventId = 1L;
        event.setState(EventState.PUBLISHED);
        event.setPublishedOn(LocalDateTime.now().minusDays(1));

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(eventMapper.toFullDto(event)).thenReturn(eventFullDto);
        when(statsClient.getStats(anyString(), anyString(), anyList(), eq(true)))
                .thenReturn(List.of());

        EventFullDto result = eventService.getPublicEvent(eventId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(statsClient, times(1)).sendHit(any(EndpointHit.class));
    }

    @Test
    void getPublicEvent_shouldThrowNotFoundException_whenNotPublished() {
        Long eventId = 1L;
        event.setState(EventState.PENDING);

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> eventService.getPublicEvent(eventId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Event with id 1 not found");

        verify(statsClient, never()).sendHit(any(EndpointHit.class));
    }

    @Test
    void getPublicEvent_shouldThrowNotFoundException_whenEventNotFound() {
        Long eventId = 999L;

        when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.getPublicEvent(eventId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Event with id 999 was not found");
    }

    private EventShortDto createEventShortDto(Event event) {
        EventShortDto dto = new EventShortDto();
        dto.setId(event.getId());
        dto.setAnnotation(event.getAnnotation());
        dto.setTitle(event.getTitle());
        dto.setPaid(event.getPaid());
        dto.setEventDate(event.getEventDate());
        return dto;
    }

    private NewEventDto createNewEventDto() {
        NewEventDto dto = new NewEventDto();
        dto.setAnnotation("Новое событие");
        dto.setDescription("Описание нового события");
        dto.setTitle("Новое событие");
        dto.setCategory(1L);
        dto.setEventDate(LocalDateTime.now().plusDays(3));
        dto.setLocation(new LocationDto(55.754167f, 37.62f));
        dto.setPaid(false);
        dto.setParticipantLimit(10);
        dto.setRequestModeration(true);
        return dto;
    }

    private Event createNewEvent() {
        Event newEvent = new Event();
        newEvent.setId(1L);
        newEvent.setAnnotation("Новое событие");
        newEvent.setDescription("Описание нового события");
        newEvent.setTitle("Новое событие");
        newEvent.setCategory(category);
        newEvent.setInitiator(initiator);
        newEvent.setLocation(new Location(55.754167f, 37.62f));
        newEvent.setEventDate(LocalDateTime.now().plusDays(3));
        newEvent.setCreatedOn(LocalDateTime.now());
        newEvent.setState(EventState.PENDING);
        newEvent.setPaid(false);
        newEvent.setParticipantLimit(10);
        newEvent.setRequestModeration(true);
        return newEvent;
    }

    private UpdateEventUserRequest createUpdateEventUserRequest() {
        UpdateEventUserRequest request = new UpdateEventUserRequest();
        request.setAnnotation("Обновленная аннотация");
        request.setDescription("Обновленное описание");
        request.setTitle("Обновленное название");
        request.setCategory(2L);
        request.setEventDate(LocalDateTime.now().plusDays(5));
        request.setPaid(true);
        request.setParticipantLimit(20);
        request.setRequestModeration(false);
        request.setStateAction(UserStateAction.SEND_TO_REVIEW);
        return request;
    }
}
