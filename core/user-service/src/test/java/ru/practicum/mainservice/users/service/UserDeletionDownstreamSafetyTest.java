package ru.practicum.mainservice.users.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.common.exception.ServiceUnavailableException;
import ru.practicum.mainservice.clients.EventServiceClient;
import ru.practicum.mainservice.clients.RequestServiceClient;
import ru.practicum.mainservice.users.entity.User;
import ru.practicum.mainservice.users.mapper.UserMapper;
import ru.practicum.mainservice.users.repository.UserRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDeletionDownstreamSafetyTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserMapper userMapper;
    @Mock
    private EventServiceClient eventServiceClient;
    @Mock
    private RequestServiceClient requestServiceClient;
    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void eventServiceOutageMustNotDeleteUser() {
        User user = new User();
        user.setId(7L);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(eventServiceClient.hasEvents(7L)).thenThrow(new ServiceUnavailableException("event-service unavailable"));

        assertThatThrownBy(() -> userService.deleteUser(7L))
                .isInstanceOf(ServiceUnavailableException.class);
        verify(userRepository, never()).delete(user);
    }

    @Test
    void requestServiceOutageMustNotDeleteUser() {
        User user = new User();
        user.setId(7L);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(requestServiceClient.hasRequests(7L))
                .thenThrow(new ServiceUnavailableException("request-service unavailable"));

        assertThatThrownBy(() -> userService.deleteUser(7L))
                .isInstanceOf(ServiceUnavailableException.class);
        verify(userRepository, never()).delete(user);
    }
}
