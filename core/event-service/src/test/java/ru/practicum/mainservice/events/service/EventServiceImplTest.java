
package ru.practicum.mainservice.events.service;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.data.domain.PageImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.common.dto.CategoryDto;
import ru.practicum.common.dto.EventInternalDto;
import ru.practicum.common.dto.EventShortDto;
import ru.practicum.common.dto.UserDto;
import ru.practicum.common.enums.EventState;
import ru.practicum.common.exception.BadRequestException;
import ru.practicum.dto.ViewStats;
import ru.practicum.common.exception.NotFoundException;
import ru.practicum.mainservice.clients.CategoryServiceClient;
import ru.practicum.mainservice.clients.RequestServiceClient;
import ru.practicum.mainservice.clients.UserServiceClient;
import ru.practicum.mainservice.events.dto.PublicEventQuery;
import ru.practicum.mainservice.events.dto.EventFullDto;
import ru.practicum.mainservice.events.dto.NewEventDto;
import ru.practicum.mainservice.events.entity.Event;
import ru.practicum.mainservice.events.mapper.EventMapper;
import ru.practicum.mainservice.events.repository.EventRepository;
import ru.practicum.mainservice.events.repository.EventRepositoryCustom;
import ru.practicum.statistics.client.controller.StatsClient;
import ru.practicum.statistics.client.exception.StatsServerUnavailable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {

    @Mock
    private EventRepository repository;

    @Mock
    private EventRepositoryCustom repositoryCustom;

    @Mock
    private EventMapper mapper;

    @Mock
    private StatsClient statsClient;

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private CategoryServiceClient categoryServiceClient;

    @Mock
    private RequestServiceClient requestServiceClient;

    @InjectMocks
    private EventServiceImpl eventService;

    private Event event;
    private UserDto user;
    private CategoryDto category;
    private EventFullDto fullDto;

    @BeforeEach
    void setUp() {
        user = new UserDto(7L, "user@example.com", "Пользователь");
        category = new CategoryDto(3L, "Концерты");

        event = new Event();
        event.setId(11L);
        event.setAnnotation("Тестовая аннотация события");
        event.setDescription("Тестовое описание события для проверки");
        event.setTitle("Тестовое событие");
        event.setInitiatorId(user.getId());
        event.setCategoryId(category.getId());
        event.setEventDate(LocalDateTime.now().plusDays(5));
        event.setCreatedOn(LocalDateTime.now());
        event.setState(EventState.PENDING);
        event.setPaid(false);
        event.setParticipantLimit(10);
        event.setRequestModeration(true);

        fullDto = new EventFullDto();
        fullDto.setId(event.getId());
        fullDto.setState(EventState.PENDING);
        fullDto.setParticipantLimit(10);
        fullDto.setRequestModeration(true);
        fullDto.setEventDate(event.getEventDate());

        lenient().when(requestServiceClient.getConfirmedCounts(
                        List.of(event.getId())))
                .thenReturn(Map.of());

        lenient().when(statsClient.getStats(
                        anyString(), anyString(), anyList(), eq(true)))
                .thenReturn(List.of());

        lenient().when(userServiceClient.getUsers(List.of(user.getId())))
                .thenReturn(List.of(user));

        lenient().when(categoryServiceClient.getCategories(
                        List.of(category.getId())))
                .thenReturn(List.of(category));
    }

    @Test
    void createEvent_shouldPersistOnlyForeignIdentifiers() {
        NewEventDto request = new NewEventDto();
        request.setAnnotation("Новая аннотация события");
        request.setDescription("Новое подробное описание события");
        request.setTitle("Новое событие");
        request.setCategory(category.getId());
        request.setEventDate(LocalDateTime.now().plusDays(3));
        request.setLocation(
                new ru.practicum.common.dto.LocationDto(55.75f, 37.61f));
        request.setParticipantLimit(10);
        request.setRequestModeration(true);

        when(userServiceClient.getUser(user.getId())).thenReturn(user);
        when(categoryServiceClient.getCategory(category.getId()))
                .thenReturn(category);
        when(mapper.toEntity(request, category.getId(), user.getId()))
                .thenReturn(event);
        when(repository.save(event)).thenReturn(event);
        when(mapper.toFullDto(event)).thenReturn(fullDto);

        EventFullDto result = eventService.createEvent(user.getId(), request);

        assertThat(result.getId()).isEqualTo(event.getId());
        assertThat(result.getCategory()).isEqualTo(category);
        assertThat(result.getInitiator().getId()).isEqualTo(user.getId());
        assertThat(result.getInitiator().getName()).isEqualTo(user.getName());
        assertThat(result.getConfirmedRequests()).isEqualTo(0L);
        assertThat(result.getViews()).isEqualTo(0L);

        assertThat(event.getInitiatorId()).isEqualTo(user.getId());
        assertThat(event.getCategoryId()).isEqualTo(category.getId());

        verify(userServiceClient).getUser(user.getId());
        verify(categoryServiceClient).getCategory(category.getId());
        verify(repository).save(event);
        verify(userServiceClient).getUsers(List.of(user.getId()));
    }

    @Test
    void createEvent_shouldStopWhenCategoryDoesNotExist() {
        NewEventDto request = new NewEventDto();
        request.setAnnotation("Новая аннотация события");
        request.setDescription("Новое подробное описание события");
        request.setTitle("Новое событие");
        request.setCategory(category.getId());
        request.setEventDate(LocalDateTime.now().plusDays(3));
        request.setLocation(
                new ru.practicum.common.dto.LocationDto(55.75f, 37.61f));

        when(userServiceClient.getUser(user.getId())).thenReturn(user);
        when(categoryServiceClient.getCategory(category.getId()))
                .thenReturn(null);

        assertThatThrownBy(
                () -> eventService.createEvent(user.getId(), request))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Category");

        verify(repository, never()).save(any());
    }

    @Test
    void getInternalEvent_shouldReturnOnlyIdentifiersAndState() {
        when(repository.findById(event.getId()))
                .thenReturn(Optional.of(event));

        EventInternalDto result =
                eventService.getInternalEvent(event.getId());

        assertThat(result.getId()).isEqualTo(event.getId());
        assertThat(result.getInitiatorId()).isEqualTo(user.getId());
        assertThat(result.getCategoryId()).isEqualTo(category.getId());
        assertThat(result.getState()).isEqualTo(EventState.PENDING);
    }

    @Test
    void getInternalEvent_shouldThrowWhenMissing() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.getInternalEvent(999L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getInternalEventShorts_shouldEnrichUsingBatchContracts() {
        EventShortDto shortDto = new EventShortDto();
        shortDto.setId(event.getId());

        when(repository.findAllById(List.of(event.getId())))
                .thenReturn(List.of(event));
        when(mapper.toShortDto(event)).thenReturn(shortDto);

        List<EventShortDto> result =
                eventService.getInternalEventShorts(List.of(event.getId()));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCategory()).isEqualTo(category);
        assertThat(result.get(0).getInitiator().getId())
                .isEqualTo(user.getId());

        verify(userServiceClient).getUsers(List.of(user.getId()));
        verify(categoryServiceClient)
                .getCategories(List.of(category.getId()));
    }

    @Test
    void getPublicEvents_shouldSortViewsBeforePagination() {
        Event second = new Event();
        second.setId(12L);
        second.setCategoryId(category.getId());
        second.setInitiatorId(user.getId());
        second.setParticipantLimit(0);
        second.setCreatedOn(LocalDateTime.now());
        second.setEventDate(event.getEventDate());
        second.setState(EventState.PUBLISHED);

        EventShortDto shortDto = new EventShortDto();
        shortDto.setId(second.getId());
        when(repositoryCustom.findPublishedEvents(anyString(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(event, second)));
        when(statsClient.getStats(anyString(), anyString(), anyList(), eq(true)))
                .thenReturn(List.of(new ViewStats("ewm-main-service", "/events/11", 1L),
                        new ViewStats("ewm-main-service", "/events/12", 8L)));
        when(mapper.toShortDto(second)).thenReturn(shortDto);

        List<EventShortDto> result = eventService.getPublicEvents(new PublicEventQuery(null, null, null,
                LocalDateTime.now().minusDays(1), LocalDateTime.now().plusYears(10),
                false, "VIEWS", 0, 1));

        assertThat(result).extracting(EventShortDto::getId).containsExactly(12L);
    }

    @Test
    void getPublicEvents_shouldSkipOrphanedHistoricalCategoriesWithoutLosingPagination() {
        Event orphaned = new Event();
        orphaned.setId(12L);
        orphaned.setCategoryId(735L);
        orphaned.setInitiatorId(user.getId());
        orphaned.setState(EventState.PUBLISHED);
        orphaned.setEventDate(event.getEventDate());
        orphaned.setCreatedOn(event.getCreatedOn());
        orphaned.setParticipantLimit(0);

        EventShortDto validDto = new EventShortDto();
        validDto.setId(event.getId());
        when(repositoryCustom.findPublishedEvents(anyString(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(orphaned)))
                .thenReturn(new PageImpl<>(List.of(orphaned, event)));
        when(categoryServiceClient.getCategories(List.of(735L)))
                .thenReturn(List.of());
        when(categoryServiceClient.getCategories(List.of(735L, category.getId())))
                .thenReturn(List.of(category));
        when(mapper.toShortDto(event)).thenReturn(validDto);

        List<EventShortDto> result = eventService.getPublicEvents(new PublicEventQuery(null, null, null,
                LocalDateTime.now().minusDays(1), LocalDateTime.now().plusYears(10),
                false, null, 0, 1));

        assertThat(result).extracting(EventShortDto::getId).containsExactly(event.getId());
        verify(repositoryCustom, times(2)).findPublishedEvents(anyString(), any(), any(),
                any(), any(), any());
    }

    @Test
    void getPublicEvents_shouldNotMaskDownstreamFailureAsOrphan() {
        when(repositoryCustom.findPublishedEvents(anyString(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(event)));
        when(categoryServiceClient.getCategories(List.of(category.getId())))
                .thenThrow(new ru.practicum.common.exception.ServiceUnavailableException(
                        "category-service unavailable"));

        assertThatThrownBy(() -> eventService.getPublicEvents(new PublicEventQuery(null, null, null,
                LocalDateTime.now().minusDays(1), LocalDateTime.now().plusYears(10),
                false, null, 0, 10)))
                .isInstanceOf(ru.practicum.common.exception.ServiceUnavailableException.class);
    }

    @Test
    void getPublicEvents_shouldRejectZeroPageSize() {
        assertThatThrownBy(() -> eventService.getPublicEvents(new PublicEventQuery(null, null, null,
                null, null, false, null, 0, 0)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void getPublicEvent_shouldKeepWorkingWhenStatisticsAreUnavailable() {
        event.setState(EventState.PUBLISHED);
        when(repository.findById(event.getId())).thenReturn(Optional.of(event));
        when(mapper.toFullDto(event)).thenReturn(fullDto);
        when(statsClient.getStats(anyString(), anyString(), anyList(), eq(true)))
                .thenThrow(new StatsServerUnavailable("Statistics service is temporarily unavailable"));

        EventFullDto result = eventService.getPublicEvent(event.getId());

        assertThat(result.getViews()).isZero();
        assertThat(result.getConfirmedRequests()).isZero();
        assertThat(result.getCategory()).isEqualTo(category);
        assertThat(result.getInitiator().getId()).isEqualTo(user.getId());
    }
}
