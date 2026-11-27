package ru.practicum.mainservice.requests.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.common.dto.EventInternalDto;
import ru.practicum.common.dto.UserDto;
import ru.practicum.common.enums.EventState;
import ru.practicum.mainservice.clients.EventServiceClient;
import ru.practicum.mainservice.clients.UserServiceClient;
import ru.practicum.mainservice.requests.entity.ParticipationRequest;
import ru.practicum.mainservice.requests.repository.ParticipationRequestRepository;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestRegistrationActionTest {
    @Mock private ParticipationRequestRepository repository;
    @Mock private EventServiceClient eventClient;
    @Mock private UserServiceClient userClient;
    @Mock private RegistrationOutboxService outbox;
    @InjectMocks private ParticipationRequestServiceImpl service;

    @Test
    void acceptedRequestPublishesRegistration() {
        when(userClient.getUser(5L)).thenReturn(new UserDto(5L, "u@example.org", "User"));
        when(eventClient.getEvent(10L)).thenReturn(new EventInternalDto(10L, 20L, 30L,
                EventState.PUBLISHED, 0, false, LocalDateTime.now().plusDays(1)));
        when(repository.findByEventIdAndRequesterId(10L, 5L)).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any(ParticipationRequest.class))).thenAnswer(call -> call.getArgument(0));

        service.addRequest(5L, 10L);
        verify(outbox).enqueue(5L, 10L);
    }
}
