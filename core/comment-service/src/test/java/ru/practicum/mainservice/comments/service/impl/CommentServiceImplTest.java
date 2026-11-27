package ru.practicum.mainservice.comments.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import ru.practicum.common.dto.CommentDto;
import ru.practicum.common.dto.EventInternalDto;
import ru.practicum.common.dto.NewCommentDto;
import ru.practicum.common.dto.UserDto;
import ru.practicum.common.dto.UserShortDto;
import ru.practicum.common.enums.EventState;
import ru.practicum.common.exception.ConflictException;
import ru.practicum.common.exception.ForbiddenException;
import ru.practicum.common.exception.NotFoundException;
import ru.practicum.common.exception.ServiceUnavailableException;
import ru.practicum.mainservice.clients.EventServiceClient;
import ru.practicum.mainservice.clients.UserServiceClient;
import ru.practicum.mainservice.comments.entity.Comment;
import ru.practicum.mainservice.comments.mapper.CommentMapper;
import ru.practicum.mainservice.comments.repository.CommentRepository;

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
    private static final long EVENT_ID = 10L;
    private static final long COMMENT_ID = 100L;

    @Mock
    private CommentRepository commentRepository;
    @Mock
    private CommentMapper commentMapper;
    @Mock
    private UserServiceClient userServiceClient;
    @Mock
    private EventServiceClient eventServiceClient;
    @InjectMocks
    private CommentServiceImpl commentService;

    private UserDto author;
    private EventInternalDto publishedEvent;
    private Comment comment;
    private CommentDto commentDto;
    private NewCommentDto newCommentDto;

    @BeforeEach
    void setUp() {
        author = new UserDto(USER_ID, "author@example.com", "Автор");
        publishedEvent = new EventInternalDto(EVENT_ID, 2L, 3L, EventState.PUBLISHED,
                10, true, LocalDateTime.now().plusDays(2));
        comment = Comment.builder().id(COMMENT_ID).text("Комментарий").eventId(EVENT_ID)
                .authorId(USER_ID).createdAt(LocalDateTime.now()).deleted(false).build();
        commentDto = CommentDto.builder().id(COMMENT_ID).text("Комментарий").eventId(EVENT_ID)
                .author(new UserShortDto(USER_ID, "Автор")).createdAt(comment.getCreatedAt())
                .isDeleted(false).build();
        newCommentDto = NewCommentDto.builder().text("Комментарий").build();
    }

    @Test
    void createComment_shouldUseServiceContractsAndReturnAuthor() {
        when(userServiceClient.getUser(USER_ID)).thenReturn(author);
        when(eventServiceClient.getEvent(EVENT_ID)).thenReturn(publishedEvent);
        when(commentMapper.toEntity(newCommentDto, EVENT_ID, USER_ID)).thenReturn(comment);
        when(commentRepository.save(comment)).thenReturn(comment);
        when(commentMapper.toDto(comment)).thenReturn(commentDto);

        CommentDto result = commentService.createComment(USER_ID, EVENT_ID, newCommentDto);

        assertThat(result.getAuthor()).isEqualTo(new UserShortDto(USER_ID, "Автор"));
        verify(eventServiceClient).getEvent(EVENT_ID);
        verify(userServiceClient).getUser(USER_ID);
        verify(commentRepository).save(comment);
    }

    @Test
    void createComment_shouldRejectUnpublishedEvent() {
        when(userServiceClient.getUser(USER_ID)).thenReturn(author);
        publishedEvent.setState(EventState.PENDING);
        when(eventServiceClient.getEvent(EVENT_ID)).thenReturn(publishedEvent);

        assertThatThrownBy(() -> commentService.createComment(USER_ID, EVENT_ID, newCommentDto))
                .isInstanceOf(ConflictException.class);
        verify(commentRepository, never()).save(any());
    }

    @Test
    void updateComment_shouldRejectAnotherAuthor() {
        when(userServiceClient.getUser(USER_ID)).thenReturn(author);
        when(commentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(comment));
        comment.setAuthorId(2L);

        assertThatThrownBy(() -> commentService.updateComment(USER_ID, COMMENT_ID,
                ru.practicum.common.dto.UpdateCommentDto.builder().text("Новый текст").build()))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void getEventComments_shouldBatchLoadAuthors() {
        when(eventServiceClient.getEvent(EVENT_ID)).thenReturn(publishedEvent);
        when(commentRepository.findByEventIdAndDeletedFalse(eq(EVENT_ID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(comment)));
        when(userServiceClient.getUsers(List.of(USER_ID))).thenReturn(List.of(author));
        when(commentMapper.toDto(comment)).thenReturn(commentDto);

        List<CommentDto> result = commentService.getEventComments(EVENT_ID, 0, 10, null);

        assertThat(result).containsExactly(commentDto);
        assertThat(result.get(0).getAuthor().getId()).isEqualTo(USER_ID);
    }

    @Test
    void updateComment_shouldLoadAuthorOnlyOnce() {
        when(userServiceClient.getUser(USER_ID)).thenReturn(author);
        when(commentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(comment));
        when(commentRepository.save(comment)).thenReturn(comment);
        when(commentMapper.toDto(comment)).thenReturn(commentDto);
        commentService.updateComment(USER_ID, COMMENT_ID,
                ru.practicum.common.dto.UpdateCommentDto.builder().text("Отредактировано").build());
        verify(userServiceClient, times(1)).getUser(USER_ID);
    }

    @Test
    void getEventComments_shouldSortByTextAndAuthorId() {
        when(eventServiceClient.getEvent(EVENT_ID)).thenReturn(publishedEvent);
        when(commentRepository.findByEventIdAndDeletedFalse(eq(EVENT_ID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        commentService.getEventComments(EVENT_ID, 0, 10, "text");
        commentService.getEventComments(EVENT_ID, 0, 10, "author.id");
        org.mockito.ArgumentCaptor<Pageable> pageCaptor =
                org.mockito.ArgumentCaptor.forClass(Pageable.class);
        verify(commentRepository, times(2))
                .findByEventIdAndDeletedFalse(eq(EVENT_ID), pageCaptor.capture());
        assertThat(pageCaptor.getAllValues().get(0).getSort().getOrderFor("text")).isNotNull();
        assertThat(pageCaptor.getAllValues().get(1).getSort().getOrderFor("authorId")).isNotNull();
    }

    @Test
    void getEventComments_shouldHonorExactNonAlignedOffset() {
        when(eventServiceClient.getEvent(EVENT_ID)).thenReturn(publishedEvent);
        when(commentRepository.findByEventIdAndDeletedFalse(eq(EVENT_ID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        commentService.getEventComments(EVENT_ID, 15, 10, "-createdAt");

        org.mockito.ArgumentCaptor<Pageable> pageable =
                org.mockito.ArgumentCaptor.forClass(Pageable.class);
        verify(commentRepository).findByEventIdAndDeletedFalse(eq(EVENT_ID), pageable.capture());
        assertThat(pageable.getValue().getOffset()).isEqualTo(15L);
        assertThat(pageable.getValue().getPageSize()).isEqualTo(10);
        assertThat(pageable.getValue().getSort().getOrderFor("createdAt").getDirection())
                .isEqualTo(org.springframework.data.domain.Sort.Direction.DESC);
        assertThat(pageable.getValue().getSort().getOrderFor("id")).isNotNull();
    }

    @Test
    void getUserCommentsByAdmin_shouldHonorExactNonAlignedOffset() {
        when(userServiceClient.getUser(USER_ID)).thenReturn(author);
        when(commentRepository.findAllByAuthorId(eq(USER_ID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        commentService.getUserCommentsByAdmin(USER_ID, 13, 10, "id");

        org.mockito.ArgumentCaptor<Pageable> pageable =
                org.mockito.ArgumentCaptor.forClass(Pageable.class);
        verify(commentRepository).findAllByAuthorId(eq(USER_ID), pageable.capture());
        assertThat(pageable.getValue().getOffset()).isEqualTo(13L);
    }

    @Test
    void getEventCommentsByAdmin_shouldHonorExactNonAlignedOffset() {
        when(eventServiceClient.getEvent(EVENT_ID)).thenReturn(publishedEvent);
        when(commentRepository.findAllByEventId(eq(EVENT_ID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        commentService.getEventCommentsByAdmin(EVENT_ID, 21, 10, "updatedAt");

        org.mockito.ArgumentCaptor<Pageable> pageable =
                org.mockito.ArgumentCaptor.forClass(Pageable.class);
        verify(commentRepository).findAllByEventId(eq(EVENT_ID), pageable.capture());
        assertThat(pageable.getValue().getOffset()).isEqualTo(21L);
    }

    @Test
    void getEventComments_shouldRejectUnrecognizedSortField() {
        when(eventServiceClient.getEvent(EVENT_ID)).thenReturn(publishedEvent);
        assertThatThrownBy(() -> commentService.getEventComments(EVENT_ID, 0, 10, "nonExisting"))
                .isInstanceOf(ru.practicum.common.exception.BadRequestException.class);
        verify(commentRepository, never())
                .findByEventIdAndDeletedFalse(any(), any());
    }

    @Test
    void deleteCommentByUser_shouldReportBothMissingResources() {
        when(userServiceClient.getUser(999L)).thenThrow(new NotFoundException("Downstream request failed"));
        when(commentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.deleteCommentByUser(999L, 999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Comment not found")
                .hasMessageContaining("Пользователь с id=999 не найден");
        verify(commentRepository).findById(999L);
    }

    @Test
    void deleteCommentByUser_shouldReportMissingUserForExistingComment() {
        when(userServiceClient.getUser(999L)).thenThrow(new NotFoundException("Downstream request failed"));
        when(commentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(comment));

        assertThatThrownBy(() -> commentService.deleteCommentByUser(999L, COMMENT_ID))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Пользователь с id=999 не найден");
        verify(commentRepository, never()).save(any());
    }

    @Test
    void deleteCommentByUser_shouldReturnComment404ForExistingUser() {
        when(userServiceClient.getUser(USER_ID)).thenReturn(author);
        when(commentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.deleteCommentByUser(USER_ID, 999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Comment not found: Комментарий с id=999 не найден");
    }

    @Test
    void deleteCommentByAdmin_shouldReturnLocalizedNotFoundMessage() {
        when(commentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.deleteCommentByAdmin(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Comment not found: Комментарий с id=999 не найден");
    }

    @Test
    void getEventCommentsByAdmin_shouldAllowExistingEventWithoutComments() {
        when(eventServiceClient.getEvent(EVENT_ID)).thenReturn(publishedEvent);
        when(commentRepository.findAllByEventId(eq(EVENT_ID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        assertThat(commentService.getEventCommentsByAdmin(EVENT_ID, 0, 10, null)).isEmpty();
        verify(eventServiceClient).getEvent(EVENT_ID);
    }

    @Test
    void getUserCommentsByAdmin_shouldPreserveMissingUserMessage() {
        when(userServiceClient.getUser(999L)).thenThrow(new NotFoundException("Downstream request failed"));

        assertThatThrownBy(() -> commentService.getUserCommentsByAdmin(999L, 0, 10, null))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("User not found: Пользователь с id=999 не найден");
    }

    @Test
    void getEventCommentsByAdmin_shouldPreserveMissingEventMessage() {
        when(eventServiceClient.getEvent(999L)).thenThrow(new NotFoundException("Downstream request failed"));

        assertThatThrownBy(() -> commentService.getEventCommentsByAdmin(999L, 0, 10, null))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Event not found: Событие с id=999 не найдено");
    }

    @Test
    void getUserCommentsByAdmin_shouldNotHideServiceUnavailable() {
        when(userServiceClient.getUser(999L)).thenThrow(new ServiceUnavailableException("user-service unavailable"));

        assertThatThrownBy(() -> commentService.getUserCommentsByAdmin(999L, 0, 10, null))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessage("user-service unavailable");
    }
}
