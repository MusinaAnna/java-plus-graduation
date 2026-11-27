package ru.practicum.mainservice.events.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import ru.practicum.common.dto.UserDto;
import ru.practicum.common.enums.EventState;
import ru.practicum.common.exception.BadRequestException;
import ru.practicum.mainservice.clients.CategoryServiceClient;
import ru.practicum.mainservice.clients.RequestServiceClient;
import ru.practicum.mainservice.clients.UserServiceClient;
import ru.practicum.mainservice.events.entity.Event;
import ru.practicum.mainservice.events.mapper.EventMapper;
import ru.practicum.mainservice.events.repository.EventRepository;
import ru.practicum.mainservice.events.repository.EventRepositoryCustom;
import ru.practicum.statistics.client.controller.StatsClient;
import ru.practicum.statistics.client.grpc.UserActionGrpcClient;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventLikesTest {
    @Mock private EventRepository repository;
    @Mock private EventMapper mapper;
    @Mock private StatsClient statsClient;
    @Mock private UserServiceClient userServiceClient;
    @Mock private CategoryServiceClient categoryServiceClient;
    @Mock private EventRepositoryCustom eventRepositoryCustom;
    @Mock private RequestServiceClient requestServiceClient;
    @Mock private UserActionGrpcClient actions;
    @InjectMocks private EventServiceImpl service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "userActionClient", actions);
    }

    @Test
    void onlyConfirmedAttendeeCanLikePastPublishedEvent() {
        Event event = new Event();
        event.setId(11L);
        event.setState(EventState.PUBLISHED);
        event.setEventDate(LocalDateTime.now().minusDays(1));
        when(repository.findById(11L)).thenReturn(Optional.of(event));
        when(userServiceClient.getUser(7L)).thenReturn(new UserDto(7L, "u@example.org", "User"));
        when(requestServiceClient.isConfirmedParticipant(11L, 7L)).thenReturn(true);

        service.likeEvent(7L, 11L);
        verify(actions).sendLike(7L, 11L);
    }

    @Test
    void futureEventCannotBeLikedEvenByConfirmedUser() {
        Event event = new Event();
        event.setId(11L);
        event.setState(EventState.PUBLISHED);
        event.setEventDate(LocalDateTime.now().plusDays(1));
        when(repository.findById(11L)).thenReturn(Optional.of(event));

        assertThrows(BadRequestException.class, () -> service.likeEvent(7L, 11L));
        verify(actions, never()).sendLike(7L, 11L);
    }
}
