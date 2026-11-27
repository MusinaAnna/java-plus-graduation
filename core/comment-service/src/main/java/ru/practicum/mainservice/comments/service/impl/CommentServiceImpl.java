package ru.practicum.mainservice.comments.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.common.dto.CommentDto;
import ru.practicum.common.dto.EventInternalDto;
import ru.practicum.common.dto.NewCommentDto;
import ru.practicum.common.dto.UpdateCommentDto;
import ru.practicum.common.dto.UserDto;
import ru.practicum.common.dto.UserShortDto;
import ru.practicum.common.enums.EventState;
import ru.practicum.common.exception.BadRequestException;
import ru.practicum.common.exception.ConflictException;
import ru.practicum.common.exception.ForbiddenException;
import ru.practicum.common.exception.NotFoundException;
import ru.practicum.mainservice.clients.EventServiceClient;
import ru.practicum.mainservice.clients.UserServiceClient;
import ru.practicum.mainservice.comments.entity.Comment;
import ru.practicum.mainservice.comments.mapper.CommentMapper;
import ru.practicum.mainservice.comments.repository.CommentRepository;
import ru.practicum.mainservice.comments.service.CommentService;
import ru.practicum.common.pagination.OffsetPageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentServiceImpl implements CommentService {
    private final CommentRepository commentRepository;
    private final CommentMapper commentMapper;
    private final UserServiceClient userServiceClient;
    private final EventServiceClient eventServiceClient;

    @Override
    @Transactional
    public CommentDto createComment(Long userId, Long eventId, NewCommentDto dto) {
        UserDto author = getUser(userId);
        EventInternalDto event = getEvent(eventId);
        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Добавлять комментарии можно только к опубликованным событиям");
        }
        Comment saved = commentRepository.save(commentMapper.toEntity(dto, eventId, userId));
        return enrichComment(saved, author);
    }

    @Override
    @Transactional
    public CommentDto updateComment(Long userId, Long commentId, UpdateCommentDto dto) {
        UserDto author = getUser(userId);
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Комментарий с id=" + commentId + " не найден"));
        if (!comment.getAuthorId().equals(userId)) {
            throw new ForbiddenException("Пользователь с id=" + userId + " не является автором комментария с id=" + commentId);
        }
        if (comment.getDeleted()) {
            throw new ConflictException("Удалённый комментарий нельзя редактировать");
        }
        commentMapper.update(dto, comment);
        comment.setUpdatedAt(LocalDateTime.now());
        return enrichComment(commentRepository.save(comment), author);
    }

    @Override
    @Transactional
    public void deleteCommentByUser(Long userId, Long commentId) {
        // Report both missing resources rather than obscuring either cause.
        try {
            getUser(userId);
        } catch (NotFoundException missingUser) {
            if (commentRepository.findById(commentId).isEmpty()) {
                throw new NotFoundException("Comment not found: Комментарий с id=" + commentId
                        + " не найден; " + missingUser.getMessage());
            }
            throw missingUser;
        }
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> commentNotFound(commentId));
        if (!comment.getAuthorId().equals(userId)) {
            throw new ForbiddenException("Пользователь с id=" + userId + " не является автором комментария с id=" + commentId);
        }
        if (!comment.getDeleted()) {
            comment.setDeleted(true);
            comment.setUpdatedAt(LocalDateTime.now());
            commentRepository.save(comment);
        }
    }

    @Override
    public List<CommentDto> getEventComments(Long eventId, int from, int size, String sort) {
        EventInternalDto event = getEvent(eventId);
        if (event.getState() != EventState.PUBLISHED) {
            throw new NotFoundException("Событие с id=" + eventId + " не найдено");
        }
        Pageable pageable = createPageable(from, size, sort);
        return enrichComments(commentRepository.findByEventIdAndDeletedFalse(eventId, pageable).getContent());
    }

    @Override
    public CommentDto getComment(Long eventId, Long commentId) {
        Comment comment = commentRepository.findByIdAndDeletedFalse(commentId)
                .orElseThrow(() -> new NotFoundException("Комментарий с id=" + commentId + " не найден"));
        if (!comment.getEventId().equals(eventId)) {
            throw new NotFoundException("Комментарий с id=" + commentId + " не найден");
        }
        EventInternalDto event = getEvent(eventId);
        if (event.getState() != EventState.PUBLISHED) {
            throw new NotFoundException("Событие с id=" + eventId + " не найдено");
        }
        return enrichComment(comment, getUser(comment.getAuthorId()));
    }

    @Override
    public List<CommentDto> getUserCommentsByAdmin(Long userId, int from, int size, String sort) {
        try {
            getUser(userId);
        } catch (NotFoundException missingUser) {
            throw new NotFoundException("User not found: " + missingUser.getMessage());
        }
        Pageable pageable = createPageable(from, size, sort);
        return enrichComments(commentRepository.findAllByAuthorId(userId, pageable).getContent());
    }

    @Override
    @Transactional
    public void deleteCommentByAdmin(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> commentNotFound(commentId));
        commentRepository.delete(comment);
    }

    @Override
    public List<CommentDto> getEventCommentsByAdmin(Long eventId, int from, int size, String sort) {
        try {
            getEvent(eventId);
        } catch (NotFoundException missingEvent) {
            throw new NotFoundException("Event not found: " + missingEvent.getMessage());
        }
        Pageable pageable = createPageable(from, size, sort);
        return enrichComments(commentRepository.findAllByEventId(eventId, pageable).getContent());
    }

    private NotFoundException commentNotFound(Long commentId) {
        return new NotFoundException("Comment not found: Комментарий с id=" + commentId + " не найден");
    }

    private Pageable createPageable(int from, int size, String sort) {
        if (from < 0 || size <= 0) {
            throw new BadRequestException("Invalid comment pagination parameters");
        }
        String field = sort == null || sort.isBlank() ? "createdAt" : sort.trim();
        boolean descending = field.startsWith("-");
        if (descending) {
            field = field.substring(1);
        }
        // The API exposes an author object, but the entity persists an authorId.
        field = switch (field) {
            case "author.id" -> "authorId";
            case "isDeleted" -> "deleted";
            default -> field;
        };
        if (!List.of("id", "text", "eventId", "authorId", "createdAt", "updatedAt", "deleted")
                .contains(field)) {
            throw new BadRequestException("Unknown comment sort field: " + field);
        }
        Sort ordering = Sort.by(descending ? Sort.Direction.DESC : Sort.Direction.ASC, field);
        return new OffsetPageRequest(from, size, "id".equals(field) ? ordering : ordering.and(Sort.by("id")));
    }

    private UserDto getUser(Long userId) {
        UserDto user;
        try {
            user = userServiceClient.getUser(userId);
        } catch (NotFoundException e) {
            throw new NotFoundException("Пользователь с id=" + userId + " не найден");
        }
        if (user == null || user.getId() == null) {
            throw new NotFoundException("Пользователь с id=" + userId + " не найден");
        }
        return user;
    }

    private EventInternalDto getEvent(Long eventId) {
        EventInternalDto event;
        try {
            event = eventServiceClient.getEvent(eventId);
        } catch (NotFoundException e) {
            throw new NotFoundException("Событие с id=" + eventId + " не найдено");
        }
        if (event == null || event.getId() == null) {
            throw new NotFoundException("Событие с id=" + eventId + " не найдено");
        }
        return event;
    }

    private CommentDto enrichComment(Comment comment, UserDto author) {
        CommentDto dto = commentMapper.toDto(comment);
        dto.setAuthor(new UserShortDto(author.getId(), author.getName()));
        return dto;
    }

    private List<CommentDto> enrichComments(List<Comment> comments) {
        if (comments.isEmpty()) {
            return List.of();
        }
        List<Long> authorIds = comments.stream().map(Comment::getAuthorId).distinct().toList();
        Map<Long, UserDto> users = userServiceClient.getUsers(authorIds).stream()
                .collect(Collectors.toMap(UserDto::getId, Function.identity()));
        return comments.stream().map(comment -> {
            UserDto author = users.get(comment.getAuthorId());
            if (author == null) {
                throw new NotFoundException("Пользователь с id=" + comment.getAuthorId() + " не найден");
            }
            return enrichComment(comment, author);
        }).toList();
    }
}
