package ru.practicum.mainservice.events.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import ru.practicum.common.dto.CategoryDto;
import ru.practicum.common.dto.EventShortDto;
import ru.practicum.common.dto.UserDto;
import ru.practicum.common.enums.EventState;
import ru.practicum.common.exception.ServiceUnavailableException;
import ru.practicum.ewm.stats.grpc.dashboard.RecommendedEventProto;
import ru.practicum.mainservice.clients.CategoryServiceClient;
import ru.practicum.mainservice.clients.RequestServiceClient;
import ru.practicum.mainservice.clients.UserServiceClient;
import ru.practicum.mainservice.events.entity.Event;
import ru.practicum.mainservice.events.mapper.EventMapper;
import ru.practicum.mainservice.events.repository.EventRepository;
import ru.practicum.mainservice.events.repository.EventRepositoryCustom;
import ru.practicum.statistics.client.controller.StatsClient;
import ru.practicum.statistics.client.grpc.RecommendationsGrpcClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventRecommendationsResilienceTest {

    @Mock private EventRepository repository;
    @Mock private EventMapper mapper;
    @Mock private StatsClient statsClient;
    @Mock private UserServiceClient userServiceClient;
    @Mock private CategoryServiceClient categoryServiceClient;
    @Mock private EventRepositoryCustom eventRepositoryCustom;
    @Mock private RequestServiceClient requestServiceClient;
    @Mock private RecommendationsGrpcClient analyzer;
    @InjectMocks private EventServiceImpl service;

    @BeforeEach
    void configureClient() {
        ReflectionTestUtils.setField(service, "recommendationsClient", analyzer);
    }

    @Test
    void fillsRequestedSizeAfterFilteringPastEvents() {
        Event past = event(10L, LocalDateTime.now().minusDays(2));
        Event futureA = event(11L, LocalDateTime.now().plusDays(2));
        Event futureB = event(12L, LocalDateTime.now().plusDays(3));

        when(analyzer.getRecommendationsForUser(7L, 2))
                .thenReturn(List.of(proto(10L), proto(11L)));
        when(analyzer.getRecommendationsForUser(7L, 4))
                .thenReturn(List.of(proto(10L), proto(11L), proto(12L)));
        when(repository.findAllById(any())).thenReturn(List.of(past, futureA, futureB));
        when(requestServiceClient.getConfirmedCounts(anyList())).thenReturn(Map.of());
        when(categoryServiceClient.getCategories(anyList())).thenReturn(List.of(new CategoryDto(3L, "Test")));
        when(userServiceClient.getUsers(anyList())).thenReturn(List.of(new UserDto(4L, "user@example.com", "User")));
        when(analyzer.getInteractionsCount(anyList())).thenReturn(List.of());
        when(mapper.toShortDto(any(Event.class))).thenAnswer(invocation -> {
            Event source = invocation.getArgument(0);
            EventShortDto dto = new EventShortDto();
            dto.setId(source.getId());
            return dto;
        });

        List<EventShortDto> result = service.getRecommendedEvents(7L, 2);

        assertThat(result).extracting(EventShortDto::getId).containsExactly(11L, 12L);
        verify(analyzer).getRecommendationsForUser(7L, 2);
        verify(analyzer).getRecommendationsForUser(7L, 4);
    }

    @Test
    void analyzerFailureDoesNotSilentlyTurnRatingsIntoZero() {
        Event published = event(17L, LocalDateTime.now().plusDays(2));
        when(repository.findById(17L)).thenReturn(Optional.of(published));
        when(requestServiceClient.getConfirmedCounts(anyList())).thenReturn(Map.of());
        when(analyzer.getInteractionsCount(anyList()))
                .thenThrow(new IllegalStateException("Analyzer unavailable"));

        assertThatThrownBy(() -> service.getPublicEvent(17L))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessageContaining("Analyzer");
    }

    private static RecommendedEventProto proto(long id) {
        return RecommendedEventProto.newBuilder().setEventId(id).setScore(0.8).build();
    }

    private static Event event(long id, LocalDateTime date) {
        Event event = new Event();
        event.setId(id);
        event.setCategoryId(3L);
        event.setInitiatorId(4L);
        event.setState(EventState.PUBLISHED);
        event.setEventDate(date);
        return event;
    }
}
