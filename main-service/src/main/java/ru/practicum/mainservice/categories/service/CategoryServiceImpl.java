package ru.practicum.mainservice.categories.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.common.dto.CategoryDto;
import ru.practicum.common.entity.Category;
import ru.practicum.common.exception.ConflictException;
import ru.practicum.common.exception.NotFoundException;
import ru.practicum.mainservice.categories.dto.NewCategoryDto;
import ru.practicum.mainservice.categories.mapper.CategoryMapper;
import ru.practicum.mainservice.categories.repository.CategoryRepository;
import ru.practicum.mainservice.events.repository.EventRepository;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final EventRepository eventRepository;
    private final CategoryMapper categoryMapper;

    @Override
    @Transactional
    public CategoryDto createCategory(NewCategoryDto dto) {
        log.info("Creating category: {}", dto);

        try {
            Category category = categoryMapper.toEntity(dto);
            category = categoryRepository.save(category);
            log.info("Created category with id: {}", category.getId());
            return categoryMapper.toDto(category);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Category with name '" + dto.getName() + "' already exists");
        }
    }

    @Override
    @Transactional
    public CategoryDto updateCategory(Long catId, CategoryDto dto) {
        log.info("Updating category with id: {}", catId);

        Category category = categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException("Category not found"));

        categoryRepository.findByName(dto.getName())
                .ifPresent(existing -> {
                    if (!existing.getId().equals(catId)) {
                        throw new ConflictException("Category with name '" + dto.getName() + "' already exists");
                    }
                });

        category.setName(dto.getName());
        category = categoryRepository.save(category);
        return categoryMapper.toDto(category);
    }

    @Override
    @Transactional
    public void deleteCategory(Long catId) {
        log.info("Deleting category with id: {}", catId);

        Category category = getCategoryEntity(catId);

        if (eventRepository.existsByCategoryId(catId)) {
            throw new ConflictException("The category is not empty");
        }

        categoryRepository.delete(category);
        log.info("Deleted category with id: {}", catId);
    }

    @Override
    public List<CategoryDto> getCategories(Pageable pageable) {
        log.info("Getting categories with pagination: {}", pageable);

        Page<Category> categories = categoryRepository.findAll(pageable);
        return categories.stream()
                .map(categoryMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public CategoryDto getCategory(Long catId) {
        log.info("Getting category with id: {}", catId);

        Category category = getCategoryEntity(catId);
        return categoryMapper.toDto(category);
    }

    @Override
    public Category getCategoryEntity(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Category with id " + categoryId + " was not found"));
    }
}
