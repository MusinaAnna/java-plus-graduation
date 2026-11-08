package ru.practicum.mainservice.controller.comment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.common.dto.CommentDto;
import ru.practicum.common.dto.NewCommentDto;
import ru.practicum.common.dto.UpdateCommentDto;
import ru.practicum.mainservice.comments.service.CommentService;
import ru.practicum.mainservice.controller.api.comment.PrivateCommentControllerApi;

@Slf4j
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/users/{userId}/comments")
public class PrivateCommentController implements PrivateCommentControllerApi {
    private final CommentService service;

    @Override
    public CommentDto createComment(Long userId, Long eventId, NewCommentDto request) {
        log.info("POST /users/{}/comments/events/{}", userId, eventId);
        log.debug("Request body: {}", request);
        return service.createComment(userId, eventId, request);
    }

    @Override
    public CommentDto updateComment(Long userId, Long commentId, UpdateCommentDto request) {
        log.info("PATCH /users/{}/comments/{}", userId, commentId);
        log.debug("Request body: {}", request);
        return service.updateComment(userId, commentId, request);
    }

    @Override
    public void deleteComment(Long userId, Long commentId) {
        log.info("DELETE /users/{}/comments/{}", userId, commentId);
        service.deleteCommentByUser(userId, commentId);
    }
}
