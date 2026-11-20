package ru.practicum.mainservice.compilations.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.common.dto.CompilationDto;
import ru.practicum.common.dto.EventShortDto;
import ru.practicum.mainservice.compilations.dto.NewCompilationDto;
import ru.practicum.mainservice.compilations.entity.Compilation;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Component
public class CompilationMapper {
    public Compilation toEntity(NewCompilationDto dto, List<Long> eventIds) {
        Compilation compilation = new Compilation();
        compilation.setTitle(dto.getTitle());
        compilation.setPinned(dto.getPinned() == null ? false : dto.getPinned());
        compilation.setEventIds(eventIds == null ? new ArrayList<>() : new ArrayList<>(eventIds));
        return compilation;
    }

    public CompilationDto toDto(Compilation compilation, List<EventShortDto> events) {
        return new CompilationDto(new LinkedHashSet<>(events), compilation.getId(),
                compilation.getPinned(), compilation.getTitle());
    }
}
