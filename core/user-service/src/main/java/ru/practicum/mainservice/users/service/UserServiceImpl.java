package ru.practicum.mainservice.users.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.common.dto.UserDto;
import ru.practicum.mainservice.users.entity.User;
import ru.practicum.common.exception.ConflictException;
import ru.practicum.common.exception.NotFoundException;
import ru.practicum.mainservice.users.dto.NewUserRequest;
import ru.practicum.mainservice.users.mapper.UserMapper;
import ru.practicum.mainservice.users.repository.UserRepository;
import ru.practicum.mainservice.clients.EventServiceClient;
import ru.practicum.mainservice.clients.RequestServiceClient;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final EventServiceClient eventServiceClient;
    private final RequestServiceClient requestServiceClient;

    @Override
    public List<UserDto> getUsers(
            List<Long> ids,
            Pageable pageable
    ) {
        log.info(
                "Getting users with ids={} and pageable={}",
                ids,
                pageable
        );

        Page<User> users;

        if (ids == null || ids.isEmpty()) {
            users = userRepository.findAll(pageable);
        } else {
            users = userRepository.findAllByIdIn(ids, pageable);
        }

        return users.stream()
                .map(userMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public UserDto createUser(NewUserRequest request) {
        log.info("Creating user: {}", request);

        User user = userMapper.toEntity(request);

        try {
            User savedUser = userRepository.saveAndFlush(user);

            log.info("Created user with id={}", savedUser.getId());

            return userMapper.toDto(savedUser);
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException(
                    "User with email '" + request.getEmail()
                            + "' already exists"
            );
        }
    }

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        log.info("Deleting user with id={}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(
                        "User with id=" + userId + " was not found"
                ));

        if (eventServiceClient.hasEvents(userId)) {
            throw new ConflictException("Cannot delete user with events");
        }
        if (requestServiceClient.hasRequests(userId)) {
            throw new ConflictException("Cannot delete user with participation requests");
        }

        userRepository.delete(user);

        log.info("Deleted user with id={}", userId);
    }

    @Override
    public UserDto getUser(Long userId) {
        return userMapper.toDto(getUserEntity(userId));
    }

    @Override
    public List<UserDto> getUsersByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return userRepository.findAllById(ids).stream().map(userMapper::toDto).toList();
    }

    User getUserEntity(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(
                        "User with id=" + userId + " was not found"
                ));
    }
}
