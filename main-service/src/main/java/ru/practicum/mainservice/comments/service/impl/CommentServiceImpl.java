package ru.practicum.mainservice.comments.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.common.dto.CommentDto;
import ru.practicum.common.dto.NewCommentDto;
import ru.practicum.common.dto.UpdateCommentDto;
import ru.practicum.common.entity.User;
import ru.practicum.common.enums.EventState;
import ru.practicum.common.enums.SortType;
import ru.practicum.common.exception.ConflictException;
import ru.practicum.common.exception.ForbiddenException;
import ru.practicum.common.exception.NotFoundException;
import ru.practicum.mainservice.comments.entity.Comment;
import ru.practicum.mainservice.comments.mapper.CommentMapper;
import ru.practicum.mainservice.comments.repository.CommentRepository;
import ru.practicum.mainservice.comments.service.CommentService;
import ru.practicum.mainservice.events.entity.Event;
import ru.practicum.mainservice.events.repository.EventRepository;
import ru.practicum.mainservice.users.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final CommentMapper commentMapper;

    @Override
    @Transactional
    public CommentDto createComment(Long userId, Long eventId, NewCommentDto dto) {
        User author = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id= " + userId + " не найден")
                );

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException(
                        "Событие с id=" + eventId + " не найдено"
                ));

        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Добавлять комментарии можно только к опубликованным событиям");
        }

        Comment comment = commentMapper.toEntity(dto, event, author);

        Comment savedComment = commentRepository.save(comment);

        log.debug("Комментарий создан с id={}", savedComment.getId());
        return commentMapper.toDto(savedComment);
    }

    @Override
    @Transactional
    public CommentDto updateComment(Long userId, Long commentId, UpdateCommentDto dto) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("Пользователь с id=" + userId + " не найден");
        }

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() ->
                        new NotFoundException("Комментарий с id=" + commentId + " не найден")
                );

        if (!comment.getAuthor().getId().equals(userId)) {
            throw new ForbiddenException(
                    "Пользователь с id=" + userId
                            + " не является автором комментария с id="
                            + commentId
            );
        }

        if (comment.getDeleted()) {
            throw new ConflictException(
                    "Удалённый комментарий нельзя редактировать"
            );
        }

        commentMapper.update(dto, comment);
        comment.setUpdatedAt(LocalDateTime.now());

        Comment updatedComment = commentRepository.save(comment);

        return commentMapper.toDto(updatedComment);
    }

    @Override
    @Transactional
    public void deleteCommentByUser(Long userId, Long commentId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException(
                    "Пользователь с id=" + userId + " не найден"
            );
        }

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException(
                        "Комментарий с id=" + commentId + " не найден"
                ));

        if (!comment.getAuthor().getId().equals(userId)) {
            throw new ForbiddenException(
                    "Пользователь с id=" + userId
                            + " не является автором комментария с id="
                            + commentId
            );
        }

        if (!comment.getDeleted()) {
            comment.setDeleted(true);
            comment.setUpdatedAt(LocalDateTime.now());
            commentRepository.save(comment);
        }
    }

    @Override
    public List<CommentDto> getEventComments(Long eventId, int from, int size, String sort) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException(
                        "Событие с id=" + eventId + " не найдено"
                ));

        if (event.getState() != EventState.PUBLISHED) {
            throw new NotFoundException(
                    "Опубликованное событие с id="
                            + eventId
                            + " не найдено"
            );
        }

        Pageable pageable;
        if (sort != null && sort.equalsIgnoreCase(SortType.VIEWS.name())) {
            pageable = PageRequest.of(from / size, size);
        } else {
            pageable = PageRequest.of(from / size, size, Sort.by("createdAt").ascending());
        }

        return commentMapper.toDtoList(
                commentRepository
                        .findByEventIdAndDeletedFalse(eventId, pageable)
                        .getContent()
        );
    }

    @Override
    public CommentDto getComment(Long eventId, Long commentId) {
        Comment comment = commentRepository
                .findByIdAndDeletedFalse(commentId)
                .orElseThrow(() -> new NotFoundException(
                        "Комментарий с id=" + commentId + " не найден"
                ));

        if (comment.getEvent().getState() != EventState.PUBLISHED) {
            throw new NotFoundException(
                    "Опубликованное событие с id="
                            + comment.getEvent().getId()
                            + " не найдено"
            );
        }
        return commentMapper.toDto(comment);
    }

    @Override
    public List<CommentDto> getUserCommentsByAdmin(Long userId, int from, int size, String sort) {

        if (!userRepository.existsById(userId)) {
            throw new NotFoundException(
                    "Пользователь с id=" + userId + " не найден"
            );
        }

        Pageable pageable;
        if (sort != null && sort.equalsIgnoreCase(SortType.VIEWS.name())) {
            pageable = PageRequest.of(from / size, size);
        } else {
            pageable = PageRequest.of(from / size, size, Sort.by("createdAt").ascending());
        }

        return commentRepository
                .findAllByAuthorId(userId, pageable)
                .map(commentMapper::toDto)
                .getContent();
    }

    @Override
    @Transactional
    public void deleteCommentByAdmin(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException(
                        "Комментарий с id=" + commentId + " не найден"
                ));

        commentRepository.delete(comment);
    }

    @Override
    public List<CommentDto> getEventCommentsByAdmin(Long eventId, int from, int size, String sort) {
        if (!eventRepository.existsById(eventId)) {
            throw new NotFoundException(
                    "Событие с id=" + eventId + " не найдено"
            );
        }

        Pageable pageable;
        if (sort != null && sort.equalsIgnoreCase(SortType.VIEWS.name())) {
            pageable = PageRequest.of(from / size, size);
        } else {
            pageable = PageRequest.of(from / size, size, Sort.by("createdAt").ascending());
        }

        return commentRepository
                .findAllByEventId(eventId, pageable)
                .map(commentMapper::toDto)
                .getContent();
    }
}
