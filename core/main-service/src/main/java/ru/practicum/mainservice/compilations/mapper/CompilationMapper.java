package ru.practicum.mainservice.compilations.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.common.dto.CompilationDto;
import ru.practicum.mainservice.compilations.dto.NewCompilationDto;
import ru.practicum.mainservice.compilations.entity.Compilation;
import ru.practicum.mainservice.events.entity.Event;
import ru.practicum.mainservice.events.mapper.EventMapper;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Маппер для преобразования подборок между сущностями и DTO.
 */
@Component
@RequiredArgsConstructor
public class CompilationMapper {

    private final EventMapper eventMapper;

    /**
     * Преобразование NewCompilationDto и списка событий в сущность Compilation.
     *
     * @param dto    данные для создания подборки
     * @param events множество событий, входящих в подборку
     * @return сущность подборки
     */
    public Compilation toEntity(NewCompilationDto dto, Set<Event> events) {
        Compilation compilation = new Compilation();
        compilation.setTitle(dto.getTitle());
        compilation.setPinned(dto.getPinned());
        compilation.setEvents(events == null ? new LinkedHashSet<>() : new LinkedHashSet<>(events));
        return compilation;
    }

    /**
     * Преобразование сущности Compilation в CompilationDto.
     *
     * @param compilation сущность подборки
     * @return DTO подборки с преобразованными событиями
     */
    public CompilationDto toDto(Compilation compilation) {
        return new CompilationDto(
                compilation.getEvents().stream()
                        .map(eventMapper::toShortDto)
                        .collect(Collectors.toCollection(LinkedHashSet::new)),
                compilation.getId(),
                compilation.getPinned(),
                compilation.getTitle()
        );
    }
}