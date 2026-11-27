package ru.practicum.mainservice.requests.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.common.dto.EventInternalDto;
import ru.practicum.common.dto.UserDto;
import ru.practicum.common.enums.EventState;
import ru.practicum.common.enums.RequestStatus;
import ru.practicum.common.exception.ConflictException;
import ru.practicum.mainservice.clients.EventServiceClient;
import ru.practicum.mainservice.clients.UserServiceClient;
import ru.practicum.mainservice.requests.dto.EventRequestStatusUpdateRequest;
import ru.practicum.mainservice.requests.dto.EventRequestStatusUpdateResult;
import ru.practicum.mainservice.requests.entity.ParticipationRequest;
import ru.practicum.mainservice.requests.repository.ParticipationRequestRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestRejectionAtCapacityTest {
    @Mock
    private ParticipationRequestRepository repository;
    @Mock
    private EventCapacityLock eventCapacityLock;
    @Mock
    private EventServiceClient eventServiceClient;
    @Mock
    private UserServiceClient userServiceClient;
    @InjectMocks
    private ParticipationRequestServiceImpl service;

    @BeforeEach
    void setUp() {
        when(userServiceClient.getUser(20L)).thenReturn(new UserDto(20L, "owner@example.com", "Owner"));
        when(eventServiceClient.getEvent(10L)).thenReturn(new EventInternalDto(
                10L, 20L, 30L, EventState.PUBLISHED, 5, true, LocalDateTime.now().plusDays(3)));
        when(repository.countByEventIdAndStatus(10L, RequestStatus.CONFIRMED)).thenReturn(5L);
    }

    @Test
    void rejectedRequestIsAllowedEvenWhenEventIsFull() {
        ParticipationRequest pending = new ParticipationRequest();
        pending.setId(101L);
        pending.setEventId(10L);
        pending.setRequesterId(7L);
        pending.setStatus(RequestStatus.PENDING);
        pending.setCreated(LocalDateTime.now());
        when(repository.findByEventIdAndIdIn(10L, List.of(101L)))
                .thenReturn(new ArrayList<>(List.of(pending)));

        EventRequestStatusUpdateResult result = service.updateRequestStatus(20L, 10L,
                new EventRequestStatusUpdateRequest(List.of(101L), RequestStatus.REJECTED));

        assertThat(pending.getStatus()).isEqualTo(RequestStatus.REJECTED);
        assertThat(result.getRejectedRequests()).hasSize(1);
        assertThat(result.getConfirmedRequests()).isEmpty();
        verify(repository).saveAll(any());
        verify(repository, never()).findByEventIdAndStatus(10L, RequestStatus.PENDING);
        org.mockito.Mockito.verifyNoInteractions(eventCapacityLock);
    }

    @Test
    void confirmationIsStillBlockedWhenEventIsFull() {
        assertThatThrownBy(() -> service.updateRequestStatus(20L, 10L,
                new EventRequestStatusUpdateRequest(List.of(101L), RequestStatus.CONFIRMED)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Лимит");
        verify(repository, never()).saveAll(any());
        verify(eventCapacityLock).lock(10L);
    }
}
