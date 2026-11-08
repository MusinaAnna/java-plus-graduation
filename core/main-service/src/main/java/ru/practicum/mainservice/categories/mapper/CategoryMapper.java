package ru.practicum.mainservice.categories.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.common.dto.CategoryDto;
import ru.practicum.common.entity.Category;
import ru.practicum.mainservice.categories.dto.NewCategoryDto;

/**
 * Маппер для преобразования категорий между сущностями и DTO.
 */
@Mapper(componentModel = "spring")
public interface CategoryMapper {

    /**
     * Преобразование сущности Category в CategoryDto.
     *
     * @param category сущность категории
     * @return DTO категории
     */
    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    CategoryDto toDto(Category category);

    /**
     * Преобразование NewCategoryDto в сущность Category.
     * Идентификатор игнорируется (генерируется БД).
     *
     * @param dto данные новой категории
     * @return сущность категории
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "name", source = "name")
    Category toEntity(NewCategoryDto dto);
}