package ru.practicum.mainservice.events.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import ru.practicum.common.dto.UserDto;
import ru.practicum.common.enums.EventState;
import ru.practicum.mainservice.clients.CategoryServiceClient;
import ru.practicum.mainservice.clients.RequestServiceClient;
import ru.practicum.mainservice.clients.UserServiceClient;
import ru.practicum.mainservice.events.dto.AdminEventQuery;
import ru.practicum.mainservice.events.entity.Event;
import ru.practicum.mainservice.events.mapper.EventMapper;
import ru.practicum.mainservice.events.repository.EventRepository;
import ru.practicum.mainservice.events.repository.EventRepositoryCustom;
import ru.practicum.statistics.client.controller.StatsClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventSearchOffsetPaginationTest {
    @Mock
    private EventRepository repository;
    @Mock
    private EventRepositoryCustom custom;
    @Mock
    private EventMapper mapper;
    @Mock
    private StatsClient statsClient;
    @Mock
    private UserServiceClient userClient;
    @Mock
    private CategoryServiceClient categoryClient;
    @Mock
    private RequestServiceClient requestClient;
    @InjectMocks
    private EventServiceImpl service;

    @Test
    void adminListQueriesDatabaseWithRealOffset() {
        when(custom.findAllByAdminFilters(any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<Event>(List.of()));

        assertThat(service.getAdminEvents(new AdminEventQuery(
                null, List.of(EventState.PUBLISHED), null, null, null, 7, 3))).isEmpty();

        ArgumentCaptor<Pageable> page = ArgumentCaptor.forClass(Pageable.class);
        verify(custom).findAllByAdminFilters(any(), any(), any(), any(), any(), page.capture());
        assertThat(page.getValue().getOffset()).isEqualTo(7);
        assertThat(page.getValue().getPageSize()).isEqualTo(3);
    }

    @Test
    void userListUsesActualOffsetInsteadOfFetchingEarlierRows() {
        when(userClient.getUser(7L)).thenReturn(new UserDto(7L, "user@example.com", "User"));
        when(repository.findByInitiatorId(eq(7L), any()))
                .thenReturn(new PageImpl<Event>(List.of()));

        assertThat(service.getUserEvents(7L, 7, 3)).isEmpty();

        ArgumentCaptor<Pageable> page = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findByInitiatorId(eq(7L), page.capture());
        assertThat(page.getValue().getOffset()).isEqualTo(7);
        assertThat(page.getValue().getPageSize()).isEqualTo(3);
    }

    @Test
    void eventInternalEndpointDoesNotCallAnyDownstream() {
        Event event = new Event();
        event.setId(11L);
        event.setCategoryId(3L);
        event.setInitiatorId(7L);
        event.setState(EventState.PUBLISHED);
        event.setParticipantLimit(5);
        event.setRequestModeration(true);
        when(repository.findById(11L)).thenReturn(java.util.Optional.of(event));

        assertThat(service.getInternalEvent(11L).getId()).isEqualTo(11L);
        verifyNoInteractions(userClient, categoryClient, requestClient, statsClient);
    }
}
