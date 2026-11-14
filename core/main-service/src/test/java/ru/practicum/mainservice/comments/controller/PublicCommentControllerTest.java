package ru.practicum.mainservice.comments.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.common.dto.CommentDto;
import ru.practicum.mainservice.comments.service.CommentService;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class PublicCommentControllerTest {
    @Autowired
    private MockMvc mvc;

    @MockBean
    private CommentService service;

    private final Long eventId = 1L;

    @Test
    void getComments_shouldReturnListOfComments() throws Exception {
        when(service.getEventComments(anyLong(), anyInt(), anyInt(), anyString())).thenReturn(List.of(createCommentDto()));

        mvc.perform(get("/events/{eventId}/comments", eventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].text").value("Wow, so beautiful!!!"));
    }

    @Test
    void getComments_withoutComments_shouldReturnEmptyList() throws Exception {
        when(service.getEventComments(anyLong(), anyInt(), anyInt(), anyString())).thenReturn(List.of());

        mvc.perform(get("/events/{eventId}/comments", eventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }


    @Test
    void getComment_shouldReturnComment() throws Exception {
        when(service.getComment(eventId, 1L)).thenReturn(createCommentDto());

        mvc.perform(get("/events/{eventId}/comments/{commentId}", eventId, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.text").value("Wow, so beautiful!!!"));

    }

    private CommentDto createCommentDto() {
        return CommentDto.builder()
                .id(1L)
                .text("Wow, so beautiful!!!")
                .build();
    }
}
