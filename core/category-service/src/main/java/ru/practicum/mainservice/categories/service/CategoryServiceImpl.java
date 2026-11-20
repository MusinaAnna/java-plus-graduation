package ru.practicum.mainservice.categories.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.common.dto.CategoryDto;
import ru.practicum.common.exception.ConflictException;
import ru.practicum.common.exception.NotFoundException;
import ru.practicum.mainservice.categories.dto.NewCategoryDto;
import ru.practicum.mainservice.categories.entity.Category;
import ru.practicum.mainservice.categories.mapper.CategoryMapper;
import ru.practicum.mainservice.categories.repository.CategoryRepository;
import ru.practicum.mainservice.clients.EventServiceClient;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;
    private final EventServiceClient eventServiceClient;

    @Override
    @Transactional
    public CategoryDto createCategory(NewCategoryDto dto) {
        try {
            Category category = categoryRepository.save(categoryMapper.toEntity(dto));
            return categoryMapper.toDto(category);
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("Category with name '" + dto.getName() + "' already exists");
        }
    }

    @Override
    @Transactional
    public CategoryDto updateCategory(Long catId, CategoryDto dto) {
        Category category = getCategoryEntity(catId);
        categoryRepository.findByName(dto.getName()).ifPresent(existing -> {
            if (!existing.getId().equals(catId)) {
                throw new ConflictException("Category with name '" + dto.getName() + "' already exists");
            }
        });
        category.setName(dto.getName());
        return categoryMapper.toDto(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public void deleteCategory(Long catId) {
        Category category = getCategoryEntity(catId);
        if (eventServiceClient.existsByCategoryId(catId)) {
            throw new ConflictException("The category is not empty");
        }
        categoryRepository.delete(category);
    }

    @Override
    public List<CategoryDto> getCategories(Pageable pageable) {
        return categoryRepository.findAll(pageable).stream().map(categoryMapper::toDto).toList();
    }

    @Override
    public CategoryDto getCategory(Long catId) {
        return categoryMapper.toDto(getCategoryEntity(catId));
    }

    @Override
    public List<CategoryDto> getCategoriesByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return categoryRepository.findAllById(ids).stream().map(categoryMapper::toDto).toList();
    }

    private Category getCategoryEntity(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Category with id " + categoryId + " was not found"));
    }
}
