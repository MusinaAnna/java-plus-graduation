package ru.practicum.mainservice.controller.comment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.common.dto.CommentDto;
import ru.practicum.mainservice.comments.service.CommentService;
import ru.practicum.mainservice.controller.api.comment.AdminCommentControllerApi;

import java.util.List;

@Slf4j
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/admin/comments")
public class AdminCommentController implements AdminCommentControllerApi {
    private final CommentService service;

    @Override
    public List<CommentDto> getUserComments(Long userId, int from, int size, String sort) {
        log.info("GET /admin/comments/users/{}?from={}&size={}&sort={}", userId, from, size, sort);
        return service.getUserCommentsByAdmin(userId, from, size, sort);
    }

    @Override
    public List<CommentDto> getEventComments(Long eventId, int from, int size, String sort) {
        log.info("GET /admin/comments/events/{}?from={}&size={}&sort={}", eventId, from, size, sort);
        return service.getEventCommentsByAdmin(eventId, from, size, sort);
    }

    @Override
    public void deleteComment(Long commentId) {
        log.info("DELETE /admin/comments/{}", commentId);
        service.deleteCommentByAdmin(commentId);
    }
}
