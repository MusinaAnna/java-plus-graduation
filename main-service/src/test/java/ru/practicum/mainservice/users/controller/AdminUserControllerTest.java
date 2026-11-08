package ru.practicum.mainservice.users.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.common.dto.UserDto;
import ru.practicum.mainservice.users.dto.NewUserRequest;
import ru.practicum.mainservice.users.repository.UserRepository;
import ru.practicum.mainservice.users.service.UserService;
import ru.practicum.statistics.client.controller.StatsClient;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private StatsClient statsClient;

    @MockBean
    private UserRepository userRepository;

    @Test
    void getUsers_shouldReturnUsers() throws Exception {
        UserDto firstUser = new UserDto();
        firstUser.setId(1L);
        firstUser.setName("Иван Иванов");
        firstUser.setEmail("ivan@example.com");

        UserDto secondUser = new UserDto();
        secondUser.setId(2L);
        secondUser.setName("Пётр Петров");
        secondUser.setEmail("petr@example.com");

        when(userService.getUsers(
                null,
                PageRequest.of(0, 10)
        )).thenReturn(List.of(firstUser, secondUser));

        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].name")
                        .value("Иван Иванов"))
                .andExpect(jsonPath("$[0].email")
                        .value("ivan@example.com"))
                .andExpect(jsonPath("$[1].id").value(2L));

        verify(userService).getUsers(
                null,
                PageRequest.of(0, 10)
        );
    }

    @Test
    void getUsers_shouldPassIdsAndPagination() throws Exception {
        UserDto user = new UserDto();
        user.setId(2L);
        user.setName("Пётр Петров");
        user.setEmail("petr@example.com");

        when(userService.getUsers(
                List.of(1L, 2L),
                PageRequest.of(2, 5)
        )).thenReturn(List.of(user));

        mockMvc.perform(get("/admin/users")
                        .param("ids", "1", "2")
                        .param("from", "10")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(2L));

        verify(userService).getUsers(
                List.of(1L, 2L),
                PageRequest.of(2, 5)
        );
    }

    @Test
    void getUsers_shouldReturnBadRequest_whenIdIsNotNumber()
            throws Exception {
        mockMvc.perform(get("/admin/users")
                        .param("ids", "text"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createUser_shouldReturnCreatedUser() throws Exception {
        NewUserRequest request = new NewUserRequest(
                "ivan@example.com",
                "Иван Иванов"
        );

        UserDto response = new UserDto();
        response.setId(1L);
        response.setName("Иван Иванов");
        response.setEmail("ivan@example.com");

        when(userService.createUser(
                any(NewUserRequest.class)
        )).thenReturn(response);

        mockMvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                request
                        )))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name")
                        .value("Иван Иванов"))
                .andExpect(jsonPath("$.email")
                        .value("ivan@example.com"));

        verify(userService)
                .createUser(any(NewUserRequest.class));
    }

    @Test
    void createUser_shouldReturnBadRequest_whenEmailInvalid()
            throws Exception {
        NewUserRequest request = new NewUserRequest(
                "invalid-email",
                "Иван Иванов"
        );

        mockMvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                request
                        )))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createUser_shouldReturnBadRequest_whenEmailBlank()
            throws Exception {
        NewUserRequest request = new NewUserRequest(
                "",
                "Иван Иванов"
        );

        mockMvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                request
                        )))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createUser_shouldReturnBadRequest_whenNameTooShort()
            throws Exception {
        NewUserRequest request = new NewUserRequest(
                "ivan@example.com",
                "И"
        );

        mockMvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                request
                        )))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteUser_shouldReturnNoContent() throws Exception {
        doNothing().when(userService).deleteUser(1L);

        mockMvc.perform(delete("/admin/users/1"))
                .andExpect(status().isNoContent());

        verify(userService).deleteUser(1L);
    }

    @Test
    void deleteUser_shouldReturnBadRequest_whenIdIsNotNumber()
            throws Exception {
        mockMvc.perform(delete("/admin/users/text"))
                .andExpect(status().isBadRequest());
    }
}
