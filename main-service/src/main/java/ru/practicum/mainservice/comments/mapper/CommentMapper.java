package ru.practicum.mainservice.comments.mapper;

import org.mapstruct.*;
import ru.practicum.common.dto.CommentDto;
import ru.practicum.common.dto.NewCommentDto;
import ru.practicum.common.dto.UpdateCommentDto;
import ru.practicum.common.entity.User;
import ru.practicum.mainservice.comments.entity.Comment;
import ru.practicum.mainservice.events.entity.Event;

import java.util.List;

/**
 * Маппер для преобразования комментариев между сущностями и DTO.
 * Использует MapStruct для автоматической генерации реализации.
 * <p>
 * Особенности:
 * <ul>
 *   <li>При создании комментария устанавливается текущая дата создания</li>
 *   <li>Новый комментарий по умолчанию считается неудалённым</li>
 *   <li>При обновлении игнорируются null-значения</li>
 * </ul>
 */
@Mapper(componentModel = "spring")
public interface CommentMapper {

    /**
     * Преобразование сущности Comment в CommentDto.
     *
     * @param comment сущность комментария
     * @return DTO с информацией о комментарии
     */
    @Mapping(target = "eventId", source = "event.id")
    @Mapping(target = "isDeleted", source = "deleted")
    CommentDto toDto(Comment comment);

    /**
     * Преобразование списка сущностей Comment в список CommentDto.
     *
     * @param comments список сущностей комментариев
     * @return список DTO с информацией о комментариях
     */
    List<CommentDto> toDtoList(List<Comment> comments);

    /**
     * Преобразование NewCommentDto в сущность Comment.
     *
     * @param dto    данные нового комментария
     * @param event  событие, к которому относится комментарий
     * @param author автор комментария
     * @return сущность комментария с текущей датой создания
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(
            target = "createdAt",
            expression = "java(java.time.LocalDateTime.now())"
    )
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", constant = "false")
    Comment toEntity(
            NewCommentDto dto,
            Event event,
            User author
    );

    /**
     * Обновление комментария переданными пользователем данными.
     * Обновляются только переданные поля (null игнорируются).
     *
     * @param dto     данные для обновления
     * @param comment обновляемая сущность комментария
     */
    @BeanMapping(
            nullValuePropertyMappingStrategy =
                    NullValuePropertyMappingStrategy.IGNORE
    )
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "event", ignore = true)
    @Mapping(target = "author", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    void update(
            UpdateCommentDto dto,
            @MappingTarget Comment comment
    );
}
