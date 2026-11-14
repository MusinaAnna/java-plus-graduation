package ru.practicum.mainservice.users.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import ru.practicum.common.dto.UserDto;
import ru.practicum.common.entity.User;
import ru.practicum.common.exception.ConflictException;
import ru.practicum.common.exception.NotFoundException;
import ru.practicum.mainservice.users.dto.NewUserRequest;
import ru.practicum.mainservice.users.mapper.UserMapper;
import ru.practicum.mainservice.users.repository.UserRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl userService;

    private User user;
    private UserDto userDto;
    private NewUserRequest newUserRequest;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setName("Иван Иванов");
        user.setEmail("ivan@example.com");

        userDto = new UserDto();
        userDto.setId(1L);
        userDto.setName("Иван Иванов");
        userDto.setEmail("ivan@example.com");

        newUserRequest = new NewUserRequest(
                "ivan@example.com",
                "Иван Иванов"
        );
    }

    @Test
    void getUsers_shouldReturnAllUsers_whenIdsAreNull() {
        PageRequest pageable = PageRequest.of(0, 10);
        Page<User> page = new PageImpl<>(List.of(user));

        when(userRepository.findAll(pageable))
                .thenReturn(page);

        when(userMapper.toDto(user))
                .thenReturn(userDto);

        List<UserDto> result =
                userService.getUsers(null, pageable);

        assertThat(result).containsExactly(userDto);

        verify(userRepository).findAll(pageable);
        verify(userRepository, never())
                .findAllByIdIn(any(), any());
    }

    @Test
    void getUsers_shouldReturnAllUsers_whenIdsAreEmpty() {
        PageRequest pageable = PageRequest.of(0, 10);
        Page<User> page = new PageImpl<>(List.of(user));

        when(userRepository.findAll(pageable))
                .thenReturn(page);

        when(userMapper.toDto(user))
                .thenReturn(userDto);

        List<UserDto> result =
                userService.getUsers(List.of(), pageable);

        assertThat(result).containsExactly(userDto);

        verify(userRepository).findAll(pageable);
        verify(userRepository, never())
                .findAllByIdIn(any(), any());
    }

    @Test
    void getUsers_shouldFilterByIds() {
        PageRequest pageable = PageRequest.of(0, 10);
        List<Long> ids = List.of(1L, 2L);
        Page<User> page = new PageImpl<>(List.of(user));

        when(userRepository.findAllByIdIn(ids, pageable))
                .thenReturn(page);

        when(userMapper.toDto(user))
                .thenReturn(userDto);

        List<UserDto> result =
                userService.getUsers(ids, pageable);

        assertThat(result).containsExactly(userDto);

        verify(userRepository)
                .findAllByIdIn(ids, pageable);

        verify(userRepository, never())
                .findAll(pageable);
    }

    @Test
    void getUsers_shouldReturnEmptyList_whenNothingFound() {
        PageRequest pageable = PageRequest.of(0, 10);

        when(userRepository.findAll(pageable))
                .thenReturn(Page.empty(pageable));

        List<UserDto> result =
                userService.getUsers(null, pageable);

        assertThat(result).isEmpty();
    }

    @Test
    void createUser_shouldCreateUser() {
        when(userMapper.toEntity(newUserRequest))
                .thenReturn(user);

        when(userRepository.saveAndFlush(user))
                .thenReturn(user);

        when(userMapper.toDto(user))
                .thenReturn(userDto);

        UserDto result = userService.createUser(newUserRequest);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Иван Иванов");
        assertThat(result.getEmail()).isEqualTo("ivan@example.com");

        verify(userRepository).saveAndFlush(user);
    }

    @Test
    void createUser_shouldThrowConflict_whenEmailExists() {
        when(userMapper.toEntity(newUserRequest))
                .thenReturn(user);

        when(userRepository.saveAndFlush(user))
                .thenThrow(new DataIntegrityViolationException(
                        "Duplicate email"
                ));

        assertThatThrownBy(() ->
                userService.createUser(newUserRequest))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("ivan@example.com")
                .hasMessageContaining("already exists");
    }

    @Test
    void deleteUser_shouldDeleteUser() {
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        userService.deleteUser(1L);

        verify(userRepository).delete(user);
    }

    @Test
    void deleteUser_shouldThrowNotFound_whenUserMissing() {
        when(userRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                userService.deleteUser(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining(
                        "User with id=999 was not found"
                );

        verify(userRepository, never())
                .delete(any(User.class));
    }

    @Test
    void getUserEntity_shouldReturnUser() {
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        User result = userService.getUserEntity(1L);

        assertThat(result).isEqualTo(user);
    }

    @Test
    void getUserEntity_shouldThrowNotFound_whenUserMissing() {
        when(userRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                userService.getUserEntity(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining(
                        "User with id=999 was not found"
                );
    }
}