package ru.practicum.mainservice.controller.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.common.dto.UserDto;
import ru.practicum.mainservice.users.service.UserService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/users")
public class UserInternalController {
    private final UserService userService;

    @GetMapping("/{userId}")
    public UserDto getUser(@PathVariable("userId") Long userId) {
        return userService.getUser(userId);
    }

    @GetMapping
    public List<UserDto> getUsers(@RequestParam("ids") List<Long> ids) {
        return userService.getUsersByIds(ids);
    }
}
