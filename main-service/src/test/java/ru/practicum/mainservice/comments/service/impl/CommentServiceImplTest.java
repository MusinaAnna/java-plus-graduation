package ru.practicum.mainservice.comments.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import ru.practicum.common.dto.CommentDto;
import ru.practicum.common.dto.NewCommentDto;
import ru.practicum.common.dto.UpdateCommentDto;
import ru.practicum.common.dto.UserShortDto;
import ru.practicum.common.entity.User;
import ru.practicum.common.enums.EventState;
import ru.practicum.common.enums.SortType;
import ru.practicum.common.exception.ConflictException;
import ru.practicum.common.exception.ForbiddenException;
import ru.practicum.common.exception.NotFoundException;
import ru.practicum.mainservice.comments.entity.Comment;
import ru.practicum.mainservice.comments.mapper.CommentMapper;
import ru.practicum.mainservice.comments.repository.CommentRepository;
import ru.practicum.mainservice.events.entity.Event;
import ru.practicum.mainservice.events.repository.EventRepository;
import ru.practicum.mainservice.users.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceImplTest {

    private static final long USER_ID = 1L;
    private static final long ANOTHER_USER_ID = 2L;
    private static final long EVENT_ID = 10L;
    private static final long COMMENT_ID = 100L;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private CommentMapper commentMapper;

    @InjectMocks
    private CommentServiceImpl commentService;

    private User author;
    private User anotherUser;
    private Event publishedEvent;
    private Event pendingEvent;
    private Comment comment;
    private CommentDto commentDto;
    private NewCommentDto newCommentDto;
    private UpdateCommentDto updateCommentDto;

    @BeforeEach
    void setUp() {
        author = new User(USER_ID, "author@example.com", "Автор");
        anotherUser = new User(ANOTHER_USER_ID, "another@example.com", "Другой пользователь");

        publishedEvent = new Event();
        publishedEvent.setId(EVENT_ID);
        publishedEvent.setState(EventState.PUBLISHED);

        pendingEvent = new Event();
        pendingEvent.setId(EVENT_ID);
        pendingEvent.setState(EventState.PENDING);

        comment = Comment.builder()
                .id(COMMENT_ID)
                .text("Исходный текст")
                .event(publishedEvent)
                .author(author)
                .createdAt(LocalDateTime.now().minusHours(1))
                .deleted(false)
                .build();

        commentDto = CommentDto.builder()
                .id(COMMENT_ID)
                .text("Исходный текст")
                .eventId(EVENT_ID)
                .author(new UserShortDto(USER_ID, "Автор"))
                .createdAt(comment.getCreatedAt())
                .isDeleted(false)
                .build();

        newCommentDto = NewCommentDto.builder()
                .text("Новый комментарий")
                .build();

        updateCommentDto = UpdateCommentDto.builder()
                .text("Обновлённый комментарий")
                .build();
    }

    @Nested
    class CreateCommentTests {

        @Test
        void createComment_shouldCreateAndReturnComment() {
            Comment newComment = Comment.builder()
                    .text(newCommentDto.getText())
                    .event(publishedEvent)
                    .author(author)
                    .deleted(false)
                    .build();

            Comment savedComment = Comment.builder()
                    .id(COMMENT_ID)
                    .text(newCommentDto.getText())
                    .event(publishedEvent)
                    .author(author)
                    .createdAt(LocalDateTime.now())
                    .deleted(false)
                    .build();

            CommentDto savedDto = CommentDto.builder()
                    .id(COMMENT_ID)
                    .text(newCommentDto.getText())
                    .eventId(EVENT_ID)
                    .author(new UserShortDto(USER_ID, author.getName()))
                    .createdAt(savedComment.getCreatedAt())
                    .isDeleted(false)
                    .build();

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(author));
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(publishedEvent));
            when(commentMapper.toEntity(newCommentDto, publishedEvent, author)).thenReturn(newComment);
            when(commentRepository.save(newComment)).thenReturn(savedComment);
            when(commentMapper.toDto(savedComment)).thenReturn(savedDto);

            CommentDto result = commentService.createComment(USER_ID, EVENT_ID, newCommentDto);

            assertThat(result).isSameAs(savedDto);
            assertThat(result.getId()).isEqualTo(COMMENT_ID);
            assertThat(result.getText()).isEqualTo(newCommentDto.getText());

            verify(commentMapper).toEntity(newCommentDto, publishedEvent, author);
            verify(commentRepository).save(newComment);
            verify(commentMapper).toDto(savedComment);
        }

        @Test
        void createComment_shouldThrowNotFound_whenUserDoesNotExist() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> commentService.createComment(USER_ID, EVENT_ID, newCommentDto))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Пользователь")
                    .hasMessageContaining(String.valueOf(USER_ID));

            verifyNoInteractions(eventRepository, commentRepository, commentMapper);
        }

        @Test
        void createComment_shouldThrowNotFound_whenEventDoesNotExist() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(author));
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> commentService.createComment(USER_ID, EVENT_ID, newCommentDto))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Событие")
                    .hasMessageContaining(String.valueOf(EVENT_ID));

            verifyNoInteractions(commentRepository, commentMapper);
        }

        @Test
        void createComment_shouldThrowConflict_whenEventIsNotPublished() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(author));
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(pendingEvent));

            assertThatThrownBy(() -> commentService.createComment(USER_ID, EVENT_ID, newCommentDto))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("только к опубликованным событиям");

            verifyNoInteractions(commentRepository, commentMapper);
        }
    }

    @Nested
    class UpdateCommentTests {

        @Test
        void updateComment_shouldUpdateAndReturnComment() {
            LocalDateTime beforeUpdate = LocalDateTime.now();
            CommentDto updatedDto = CommentDto.builder()
                    .id(COMMENT_ID)
                    .text(updateCommentDto.getText())
                    .eventId(EVENT_ID)
                    .author(new UserShortDto(USER_ID, author.getName()))
                    .isDeleted(false)
                    .build();

            when(userRepository.existsById(USER_ID)).thenReturn(true);
            when(commentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(comment));
            doAnswer(invocation -> {
                UpdateCommentDto source = invocation.getArgument(0);
                Comment target = invocation.getArgument(1);
                target.setText(source.getText());
                return null;
            }).when(commentMapper).update(updateCommentDto, comment);
            when(commentRepository.save(comment)).thenReturn(comment);
            when(commentMapper.toDto(comment)).thenReturn(updatedDto);

            CommentDto result = commentService.updateComment(USER_ID, COMMENT_ID, updateCommentDto);

            LocalDateTime afterUpdate = LocalDateTime.now();

            assertThat(result).isSameAs(updatedDto);
            assertThat(comment.getText()).isEqualTo(updateCommentDto.getText());
            assertThat(comment.getUpdatedAt())
                    .isNotNull()
                    .isBetween(beforeUpdate, afterUpdate);

            verify(commentMapper).update(updateCommentDto, comment);
            verify(commentRepository).save(comment);
            verify(commentMapper).toDto(comment);
        }

        @Test
        void updateComment_shouldThrowNotFound_whenUserDoesNotExist() {
            when(userRepository.existsById(USER_ID)).thenReturn(false);

            assertThatThrownBy(() -> commentService.updateComment(USER_ID, COMMENT_ID, updateCommentDto))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Пользователь")
                    .hasMessageContaining(String.valueOf(USER_ID));

            verifyNoInteractions(commentRepository, commentMapper);
        }

        @Test
        void updateComment_shouldThrowNotFound_whenCommentDoesNotExist() {
            when(userRepository.existsById(USER_ID)).thenReturn(true);
            when(commentRepository.findById(COMMENT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> commentService.updateComment(USER_ID, COMMENT_ID, updateCommentDto))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Комментарий")
                    .hasMessageContaining(String.valueOf(COMMENT_ID));

            verify(commentRepository, never()).save(any(Comment.class));
            verifyNoInteractions(commentMapper);
        }

        @Test
        void updateComment_shouldThrowForbidden_whenUserIsNotAuthor() {
            comment.setAuthor(anotherUser);
            when(userRepository.existsById(USER_ID)).thenReturn(true);
            when(commentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(comment));

            assertThatThrownBy(() -> commentService.updateComment(USER_ID, COMMENT_ID, updateCommentDto))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("не является автором");

            verify(commentRepository, never()).save(any(Comment.class));
            verifyNoInteractions(commentMapper);
        }

        @Test
        void updateComment_shouldThrowConflict_whenCommentIsDeleted() {
            comment.setDeleted(true);
            when(userRepository.existsById(USER_ID)).thenReturn(true);
            when(commentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(comment));

            assertThatThrownBy(() -> commentService.updateComment(USER_ID, COMMENT_ID, updateCommentDto))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("Удалённый комментарий нельзя редактировать");

            verify(commentRepository, never()).save(any(Comment.class));
            verifyNoInteractions(commentMapper);
        }
    }

    @Nested
    class DeleteCommentByUserTests {

        @Test
        void deleteCommentByUser_shouldSoftDeleteComment() {
            LocalDateTime beforeDelete = LocalDateTime.now();
            when(userRepository.existsById(USER_ID)).thenReturn(true);
            when(commentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(comment));
            commentService.deleteCommentByUser(USER_ID, COMMENT_ID);

            LocalDateTime afterDelete = LocalDateTime.now();

            assertThat(comment.getDeleted()).isTrue();
            assertThat(comment.getUpdatedAt())
                    .isNotNull()
                    .isBetween(beforeDelete, afterDelete);
            verify(commentRepository).save(comment);
            verify(commentRepository, never()).delete(any(Comment.class));
        }

        @Test
        void deleteCommentByUser_shouldDoNothing_whenCommentAlreadyDeleted() {
            comment.setDeleted(true);
            when(userRepository.existsById(USER_ID)).thenReturn(true);
            when(commentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(comment));

            commentService.deleteCommentByUser(USER_ID, COMMENT_ID);

            assertThat(comment.getDeleted()).isTrue();
            verify(commentRepository, never()).save(any(Comment.class));
            verify(commentRepository, never()).delete(any(Comment.class));
        }

        @Test
        void deleteCommentByUser_shouldThrowNotFound_whenUserDoesNotExist() {
            when(userRepository.existsById(USER_ID)).thenReturn(false);

            assertThatThrownBy(() -> commentService.deleteCommentByUser(USER_ID, COMMENT_ID))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Пользователь");

            verifyNoInteractions(commentRepository);
        }

        @Test
        void deleteCommentByUser_shouldThrowNotFound_whenCommentDoesNotExist() {
            when(userRepository.existsById(USER_ID)).thenReturn(true);
            when(commentRepository.findById(COMMENT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> commentService.deleteCommentByUser(USER_ID, COMMENT_ID))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Комментарий");

            verify(commentRepository, never()).save(any(Comment.class));
        }

        @Test
        void deleteCommentByUser_shouldThrowForbidden_whenUserIsNotAuthor() {
            comment.setAuthor(anotherUser);
            when(userRepository.existsById(USER_ID)).thenReturn(true);
            when(commentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(comment));

            assertThatThrownBy(() -> commentService.deleteCommentByUser(USER_ID, COMMENT_ID))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("не является автором");

            verify(commentRepository, never()).save(any(Comment.class));
            verify(commentRepository, never()).delete(any(Comment.class));
        }
    }

    @Nested
    class GetEventCommentsTests {

        @Test
        void getEventComments_shouldReturnCommentsWithDefaultSorting() {
            int from = 10;
            int size = 5;
            Pageable pageable = PageRequest.of(
                    2,
                    size,
                    Sort.by("createdAt").ascending()
            );
            List<Comment> comments = List.of(comment);
            List<CommentDto> expected = List.of(commentDto);

            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(publishedEvent));
            when(commentRepository.findByEventIdAndDeletedFalse(EVENT_ID, pageable))
                    .thenReturn(new PageImpl<>(comments, pageable, comments.size()));
            when(commentMapper.toDtoList(comments)).thenReturn(expected);

            List<CommentDto> result = commentService.getEventComments(EVENT_ID, from, size, null);

            assertThat(result).containsExactlyElementsOf(expected);
            verify(commentRepository).findByEventIdAndDeletedFalse(EVENT_ID, pageable);
            verify(commentMapper).toDtoList(comments);
        }

        @Test
        void getEventComments_shouldUseUnsortedPage_whenSortIsViews() {
            int from = 10;
            int size = 5;
            Pageable pageable = PageRequest.of(2, size);
            List<Comment> comments = List.of(comment);

            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(publishedEvent));
            when(commentRepository.findByEventIdAndDeletedFalse(EVENT_ID, pageable))
                    .thenReturn(new PageImpl<>(comments, pageable, comments.size()));
            when(commentMapper.toDtoList(comments)).thenReturn(List.of(commentDto));

            List<CommentDto> result = commentService.getEventComments(
                    EVENT_ID,
                    from,
                    size,
                    SortType.VIEWS.name().toLowerCase()
            );

            assertThat(result).containsExactly(commentDto);
            assertThat(pageable.getSort().isUnsorted()).isTrue();
            verify(commentRepository).findByEventIdAndDeletedFalse(EVENT_ID, pageable);
        }

        @Test
        void getEventComments_shouldThrowNotFound_whenEventDoesNotExist() {
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> commentService.getEventComments(EVENT_ID, 0, 10, null))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Событие");

            verifyNoInteractions(commentRepository, commentMapper);
        }

        @Test
        void getEventComments_shouldThrowNotFound_whenEventIsNotPublished() {
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(pendingEvent));

            assertThatThrownBy(() -> commentService.getEventComments(EVENT_ID, 0, 10, null))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Опубликованное событие");

            verifyNoInteractions(commentRepository, commentMapper);
        }
    }

    @Nested
    class GetCommentTests {

        @Test
        void getComment_shouldReturnActiveCommentOfPublishedEvent() {
            when(commentRepository.findByIdAndDeletedFalse(COMMENT_ID))
                    .thenReturn(Optional.of(comment));
            when(commentMapper.toDto(comment)).thenReturn(commentDto);

            CommentDto result = commentService.getComment(EVENT_ID, COMMENT_ID);

            assertThat(result).isSameAs(commentDto);
            verify(commentMapper).toDto(comment);
        }

        @Test
        void getComment_shouldThrowNotFound_whenCommentDoesNotExistOrDeleted() {
            when(commentRepository.findByIdAndDeletedFalse(COMMENT_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> commentService.getComment(EVENT_ID, COMMENT_ID))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Комментарий");

            verifyNoInteractions(commentMapper);
        }

        @Test
        void getComment_shouldThrowNotFound_whenCommentEventIsNotPublished() {
            comment.setEvent(pendingEvent);
            when(commentRepository.findByIdAndDeletedFalse(COMMENT_ID))
                    .thenReturn(Optional.of(comment));

            assertThatThrownBy(() -> commentService.getComment(EVENT_ID, COMMENT_ID))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Опубликованное событие");

            verifyNoInteractions(commentMapper);
        }
    }

    @Nested
    class AdminCommentTests {

        @Test
        void getUserCommentsByAdmin_shouldReturnCommentsWithDefaultSorting() {
            int from = 10;
            int size = 5;
            Pageable pageable = PageRequest.of(
                    2,
                    size,
                    Sort.by("createdAt").ascending()
            );

            when(userRepository.existsById(USER_ID)).thenReturn(true);
            when(commentRepository.findAllByAuthorId(USER_ID, pageable))
                    .thenReturn(new PageImpl<>(List.of(comment), pageable, 1));
            when(commentMapper.toDto(comment)).thenReturn(commentDto);

            List<CommentDto> result = commentService.getUserCommentsByAdmin(
                    USER_ID,
                    from,
                    size,
                    null
            );

            assertThat(result).containsExactly(commentDto);
            verify(commentRepository).findAllByAuthorId(USER_ID, pageable);
            verify(commentMapper).toDto(comment);
        }

        @Test
        void getUserCommentsByAdmin_shouldUseUnsortedPage_whenSortIsViews() {
            int from = 10;
            int size = 5;
            Pageable pageable = PageRequest.of(2, size);

            when(userRepository.existsById(USER_ID)).thenReturn(true);
            when(commentRepository.findAllByAuthorId(USER_ID, pageable))
                    .thenReturn(new PageImpl<>(List.of(comment), pageable, 1));
            when(commentMapper.toDto(comment)).thenReturn(commentDto);

            List<CommentDto> result = commentService.getUserCommentsByAdmin(
                    USER_ID,
                    from,
                    size,
                    SortType.VIEWS.name()
            );

            assertThat(result).containsExactly(commentDto);
            verify(commentRepository).findAllByAuthorId(USER_ID, pageable);
        }

        @Test
        void getUserCommentsByAdmin_shouldThrowNotFound_whenUserDoesNotExist() {
            when(userRepository.existsById(USER_ID)).thenReturn(false);

            assertThatThrownBy(() -> commentService.getUserCommentsByAdmin(USER_ID, 0, 10, null))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Пользователь");

            verifyNoInteractions(commentRepository, commentMapper);
        }

        @Test
        void deleteCommentByAdmin_shouldDeleteComment() {
            when(commentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(comment));

            commentService.deleteCommentByAdmin(COMMENT_ID);

            verify(commentRepository).delete(comment);
        }

        @Test
        void deleteCommentByAdmin_shouldThrowNotFound_whenCommentDoesNotExist() {
            when(commentRepository.findById(COMMENT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> commentService.deleteCommentByAdmin(COMMENT_ID))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Комментарий");

            verify(commentRepository, never()).delete(any(Comment.class));
        }

        @Test
        void getEventCommentsByAdmin_shouldReturnCommentsWithDefaultSorting() {
            int from = 10;
            int size = 5;
            Pageable pageable = PageRequest.of(
                    2,
                    size,
                    Sort.by("createdAt").ascending()
            );

            when(eventRepository.existsById(EVENT_ID)).thenReturn(true);
            when(commentRepository.findAllByEventId(EVENT_ID, pageable))
                    .thenReturn(new PageImpl<>(List.of(comment), pageable, 1));
            when(commentMapper.toDto(comment)).thenReturn(commentDto);

            List<CommentDto> result = commentService.getEventCommentsByAdmin(
                    EVENT_ID,
                    from,
                    size,
                    null
            );

            assertThat(result).containsExactly(commentDto);
            verify(commentRepository).findAllByEventId(EVENT_ID, pageable);
            verify(commentMapper).toDto(comment);
        }

        @Test
        void getEventCommentsByAdmin_shouldUseUnsortedPage_whenSortIsViews() {
            int from = 10;
            int size = 5;
            Pageable pageable = PageRequest.of(2, size);

            when(eventRepository.existsById(EVENT_ID)).thenReturn(true);
            when(commentRepository.findAllByEventId(EVENT_ID, pageable))
                    .thenReturn(new PageImpl<>(List.of(comment), pageable, 1));
            when(commentMapper.toDto(comment)).thenReturn(commentDto);

            List<CommentDto> result = commentService.getEventCommentsByAdmin(
                    EVENT_ID,
                    from,
                    size,
                    SortType.VIEWS.name().toLowerCase()
            );

            assertThat(result).containsExactly(commentDto);
            verify(commentRepository).findAllByEventId(EVENT_ID, pageable);
        }

        @Test
        void getEventCommentsByAdmin_shouldThrowNotFound_whenEventDoesNotExist() {
            when(eventRepository.existsById(EVENT_ID)).thenReturn(false);

            assertThatThrownBy(() -> commentService.getEventCommentsByAdmin(EVENT_ID, 0, 10, null))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Событие");

            verifyNoInteractions(commentRepository, commentMapper);
        }
    }
}
