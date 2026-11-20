package ru.practicum.mainservice.comments.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.common.dto.CommentDto;
import ru.practicum.common.dto.UserShortDto;
import ru.practicum.mainservice.comments.service.CommentService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AdminCommentControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private CommentService service;

    private final Long userId = 1L;
    private final Long eventId = 1L;
    private final Long commentId = 1L;

    @Test
    void getUserComments_shouldReturnListOfComments() throws Exception {
        when(service.getUserCommentsByAdmin(eq(userId), eq(0), eq(10), anyString()))
                .thenReturn(List.of(createCommentDto()));

        mvc.perform(get("/admin/comments/users/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].text").value("Admin test comment"))
                .andExpect(jsonPath("$[0].eventId").value(eventId))
                .andExpect(jsonPath("$[0].author.id").value(userId));
    }

    @Test
    void getUserComments_withoutComments_shouldReturnEmptyList() throws Exception {
        when(service.getUserCommentsByAdmin(eq(userId), eq(0), eq(10), anyString()))
                .thenReturn(List.of());

        mvc.perform(get("/admin/comments/users/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getEventComments_shouldReturnListOfComments() throws Exception {
        when(service.getEventCommentsByAdmin(eq(eventId), eq(0), eq(10), anyString()))
                .thenReturn(List.of(createCommentDto()));

        mvc.perform(get("/admin/comments/events/{eventId}", eventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].text").value("Admin test comment"))
                .andExpect(jsonPath("$[0].eventId").value(eventId));
    }

    @Test
    void getEventComments_withoutComments_shouldReturnEmptyList() throws Exception {
        when(service.getEventCommentsByAdmin(eq(eventId), eq(0), eq(10), anyString()))
                .thenReturn(List.of());

        mvc.perform(get("/admin/comments/events/{eventId}", eventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void deleteComment_shouldReturnOk() throws Exception {
        doNothing().when(service).deleteCommentByAdmin(eq(commentId));

        mvc.perform(delete("/admin/comments/{commentId}", commentId))
                .andExpect(status().isNoContent());
    }

    private CommentDto createCommentDto() {
        UserShortDto author = UserShortDto.builder()
                .id(userId)
                .name("AdminUser")
                .build();

        return CommentDto.builder()
                .id(1L)
                .text("Admin test comment")
                .eventId(eventId)
                .author(author)
                .createdAt(LocalDateTime.now())
                .isDeleted(false)
                .build();
    }
}