package ru.practicum.mainservice.categories.repository;

import jakarta.validation.constraints.Size;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.common.entity.Category;

import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    /**
     * Поиск категории по названию.
     *
     * @param name название категории (максимум 50 символов)
     * @return Optional с категорией, если найдена
     */
    Optional<Category> findByName(@Size(max = 50) String name);

}
