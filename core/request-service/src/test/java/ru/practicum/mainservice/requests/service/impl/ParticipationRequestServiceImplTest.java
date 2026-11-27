package ru.practicum.mainservice.requests.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import ru.practicum.common.dto.EventInternalDto;
import ru.practicum.common.dto.UserDto;
import ru.practicum.common.enums.EventState;
import ru.practicum.common.enums.RequestStatus;
import ru.practicum.common.exception.ConflictException;
import ru.practicum.mainservice.clients.EventServiceClient;
import ru.practicum.mainservice.clients.UserServiceClient;
import ru.practicum.mainservice.requests.dto.ParticipationRequestDto;
import ru.practicum.mainservice.requests.entity.ParticipationRequest;
import ru.practicum.mainservice.requests.repository.ParticipationRequestRepository;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ParticipationRequestServiceImplTest {
    @Mock
    private ParticipationRequestRepository repository;
    @Mock
    private EventServiceClient eventServiceClient;
    @Mock
    private UserServiceClient userServiceClient;
    @InjectMocks
    private ParticipationRequestServiceImpl service;

    private EventInternalDto event;
    private UserDto user;

    @BeforeEach
    void setUp() {
        event = new EventInternalDto(10L, 20L, 30L, EventState.PUBLISHED, 5, true,
                LocalDateTime.now().plusDays(3));
        user = new UserDto(7L, "user@example.com", "Пользователь");
    }

    @Test
    void addRequest_shouldPersistForeignIdsOnly() {
        when(userServiceClient.getUser(7L)).thenReturn(user);
        when(eventServiceClient.getEvent(10L)).thenReturn(event);
        when(repository.findByEventIdAndRequesterId(10L, 7L)).thenReturn(Optional.empty());
        when(repository.countByEventIdAndStatus(10L, RequestStatus.CONFIRMED)).thenReturn(2L);
        when(repository.saveAndFlush(any(ParticipationRequest.class))).thenAnswer(invocation -> {
            ParticipationRequest saved = invocation.getArgument(0);
            saved.setId(100L);
            return saved;
        });

        ParticipationRequestDto result = service.addRequest(7L, 10L);

        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getEvent()).isEqualTo(10L);
        assertThat(result.getRequester()).isEqualTo(7L);
        assertThat(result.getStatus()).isEqualTo(RequestStatus.PENDING);
        verify(eventServiceClient).getEvent(10L);
        verify(userServiceClient).getUser(7L);
    }

    @Test
    void addRequest_shouldRejectInitiator() {
        when(userServiceClient.getUser(20L)).thenReturn(new UserDto(20L, "owner@example.com", "Владелец"));
        when(eventServiceClient.getEvent(10L)).thenReturn(event);

        assertThatThrownBy(() -> service.addRequest(20L, 10L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Инициатор");
        verify(repository, never()).save(any());
    }

    @Test
    void addRequest_shouldRejectConcurrentDuplicateAtDatabaseLevel() {
        when(userServiceClient.getUser(7L)).thenReturn(user);
        when(eventServiceClient.getEvent(10L)).thenReturn(event);
        when(repository.findByEventIdAndRequesterId(10L, 7L)).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any(ParticipationRequest.class)))
                .thenThrow(new DataIntegrityViolationException("Unique constraint"));

        assertThatThrownBy(() -> service.addRequest(7L, 10L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("уже существует");
    }

    @Test
    void addRequest_shouldAutoConfirmWhenModerationIsDisabled() {
        event.setRequestModeration(false);
        when(userServiceClient.getUser(7L)).thenReturn(user);
        when(eventServiceClient.getEvent(10L)).thenReturn(event);
        when(repository.findByEventIdAndRequesterId(10L, 7L)).thenReturn(Optional.empty());
        when(repository.countByEventIdAndStatus(10L, RequestStatus.CONFIRMED)).thenReturn(0L);
        when(repository.saveAndFlush(any(ParticipationRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(service.addRequest(7L, 10L).getStatus()).isEqualTo(RequestStatus.CONFIRMED);
    }
}
