package ru.practicum.mainservice.categories.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import ru.practicum.common.dto.CategoryDto;
import ru.practicum.common.entity.Category;
import ru.practicum.common.exception.ConflictException;
import ru.practicum.common.exception.NotFoundException;
import ru.practicum.mainservice.categories.dto.NewCategoryDto;
import ru.practicum.mainservice.categories.mapper.CategoryMapper;
import ru.practicum.mainservice.categories.repository.CategoryRepository;
import ru.practicum.mainservice.events.repository.EventRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private Category category;
    private CategoryDto categoryDto;
    private NewCategoryDto newCategoryDto;

    @BeforeEach
    void setUp() {
        category = new Category();
        category.setId(1L);
        category.setName("Концерты");

        categoryDto = new CategoryDto();
        categoryDto.setId(1L);
        categoryDto.setName("Концерты");

        newCategoryDto = new NewCategoryDto();
        newCategoryDto.setName("Концерты");
    }

    @Test
    void createCategory_shouldReturnCreatedCategory() {
        when(categoryMapper.toEntity(newCategoryDto)).thenReturn(category);
        when(categoryRepository.save(any(Category.class))).thenReturn(category);
        when(categoryMapper.toDto(category)).thenReturn(categoryDto);

        CategoryDto result = categoryService.createCategory(newCategoryDto);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Концерты");
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void createCategory_shouldThrowConflictException_whenNameExists() {
        when(categoryMapper.toEntity(newCategoryDto)).thenReturn(category);
        when(categoryRepository.save(any(Category.class)))
                .thenThrow(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> categoryService.createCategory(newCategoryDto))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Category with name 'Концерты' already exists");
    }

    @Test
    void updateCategory_shouldReturnUpdatedCategory() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryRepository.save(any(Category.class))).thenReturn(category);
        when(categoryMapper.toDto(category)).thenReturn(categoryDto);

        CategoryDto result = categoryService.updateCategory(1L, categoryDto);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void updateCategory_shouldThrowNotFoundException_whenCategoryNotFound() {
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.updateCategory(999L, categoryDto))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Category not found");
    }

    @Test
    void deleteCategory_shouldDeleteSuccessfully() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(eventRepository.existsByCategoryId(1L)).thenReturn(false);

        categoryService.deleteCategory(1L);

        verify(categoryRepository).delete(category);
    }

    @Test
    void deleteCategory_shouldThrowConflictException_whenCategoryHasEvents() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(eventRepository.existsByCategoryId(1L)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.deleteCategory(1L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("The category is not empty");

        verify(categoryRepository, never()).delete(any(Category.class));
    }

    @Test
    void deleteCategory_shouldThrowNotFoundException_whenCategoryNotFound() {
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.deleteCategory(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Category with id 999 was not found");
    }

    @Test
    void getCategories_shouldReturnListOfCategories() {
        Page<Category> page = new PageImpl<>(List.of(category));
        when(categoryRepository.findAll(any(PageRequest.class))).thenReturn(page);
        when(categoryMapper.toDto(category)).thenReturn(categoryDto);

        List<CategoryDto> result = categoryService.getCategories(PageRequest.of(0, 10));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
    }

    @Test
    void getCategory_shouldReturnCategory() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryMapper.toDto(category)).thenReturn(categoryDto);

        CategoryDto result = categoryService.getCategory(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void getCategory_shouldThrowNotFoundException_whenNotFound() {
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.getCategory(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Category with id 999 was not found");
    }

    @Test
    void getCategoryEntity_shouldReturnCategory() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        Category result = categoryService.getCategoryEntity(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void getCategoryEntity_shouldThrowNotFoundException_whenNotFound() {
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.getCategoryEntity(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Category with id 999 was not found");
    }
}
