package ru.practicum.mainservice.comments.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.mainservice.comments.entity.Comment;

import java.util.Optional;

/**
 * Репозиторий для работы с комментариями.
 */
public interface CommentRepository extends JpaRepository<Comment, Long> {

    /**
     * Получение неудалённых комментариев события.
     *
     * @param eventId  идентификатор события
     * @param pageable параметры пагинации и сортировки
     * @return страница неудалённых комментариев
     */
    Page<Comment> findByEventIdAndDeletedFalse(Long eventId, Pageable pageable);

    /**
     * Получение неудалённого комментария по идентификатору.
     *
     * @param commentId идентификатор комментария
     * @return найденный неудалённый комментарий
     */
    Optional<Comment> findByIdAndDeletedFalse(Long commentId);

    /**
     * Получение всех комментариев пользователя, включая удалённые.
     *
     * @param authorId идентификатор автора
     * @param pageable параметры пагинации и сортировки
     * @return страница комментариев пользователя
     */
    Page<Comment> findAllByAuthorId(Long authorId, Pageable pageable);

    /**
     * Получение всех комментариев события, включая удалённые.
     *
     * @param eventId  идентификатор события
     * @param pageable параметры пагинации и сортировки
     * @return страница комментариев события
     */
    Page<Comment> findAllByEventId(Long eventId, Pageable pageable);

}
