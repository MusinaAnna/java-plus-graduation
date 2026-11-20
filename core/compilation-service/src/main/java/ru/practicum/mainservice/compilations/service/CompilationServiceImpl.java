
package ru.practicum.mainservice.compilations.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.common.dto.CompilationDto;
import ru.practicum.common.dto.EventShortDto;
import ru.practicum.common.exception.ConflictException;
import ru.practicum.common.exception.NotFoundException;
import ru.practicum.mainservice.clients.EventServiceClient;
import ru.practicum.mainservice.compilations.dto.NewCompilationDto;
import ru.practicum.mainservice.compilations.dto.UpdateCompilationRequest;
import ru.practicum.mainservice.compilations.entity.Compilation;
import ru.practicum.mainservice.compilations.mapper.CompilationMapper;
import ru.practicum.mainservice.compilations.repository.CompilationRepository;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompilationServiceImpl implements CompilationService {

    private final CompilationRepository compilationRepository;
    private final CompilationMapper compilationMapper;
    private final EventServiceClient eventServiceClient;

    @Override
    @Transactional
    public CompilationDto createCompilation(NewCompilationDto dto) {
        List<Long> eventIds = resolveEventIds(dto.getEvents());
        Compilation compilation = compilationMapper.toEntity(dto, eventIds);

        try {
            return toDto(compilationRepository.saveAndFlush(compilation));
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException(
                    "Compilation with title '" + dto.getTitle() + "' already exists"
            );
        }
    }

    @Override
    @Transactional
    public CompilationDto updateCompilation(Long compId, UpdateCompilationRequest dto) {
        Compilation compilation = getCompilationEntity(compId);

        if (dto == null) {
            return toDto(compilation);
        }

        if (dto.getTitle() != null) {
            compilation.setTitle(dto.getTitle());
        }

        if (dto.getPinned() != null) {
            compilation.setPinned(dto.getPinned());
        }

        if (dto.getEvents() != null) {
            List<Long> resolvedEventIds = resolveEventIds(dto.getEvents());

            compilation.getEventIds().clear();
            compilation.getEventIds().addAll(resolvedEventIds);
        }

        try {

            compilationRepository.flush();

            return toDto(compilation);

        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException(
                    "Compilation with title '" + compilation.getTitle()
                            + "' already exists"
            );
        }
    }

    @Override
    @Transactional
    public void deleteCompilation(Long compId) {
        compilationRepository.delete(getCompilationEntity(compId));
    }

    @Override
    public List<CompilationDto> getCompilations(Boolean pinned, Pageable pageable) {
        Page<Long> idPage = compilationRepository.findCompilationIds(pinned, pageable);
        List<Long> ids = idPage.getContent();

        if (ids.isEmpty()) {
            return List.of();
        }

        Map<Long, Compilation> byId = compilationRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(
                        Compilation::getId,
                        Function.identity()
                ));

        List<Compilation> compilations = ids.stream()
                .map(byId::get)
                .filter(java.util.Objects::nonNull)
                .toList();

        return toDtos(compilations);
    }

    @Override
    public CompilationDto getCompilation(Long compId) {
        return toDto(getCompilationEntity(compId));
    }

    private Compilation getCompilationEntity(Long compId) {
        return compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException(
                        "Compilation with id=" + compId + " was not found"
                ));
    }

    private List<Long> resolveEventIds(Set<Long> requestedIds) {
        if (requestedIds == null || requestedIds.isEmpty()) {
            return List.of();
        }

        List<Long> ids = requestedIds.stream()
                .sorted()
                .toList();

        List<EventShortDto> found = eventServiceClient.getEventShorts(ids);

        Set<Long> foundIds = found.stream()
                .map(EventShortDto::getId)
                .collect(Collectors.toSet());

        Set<Long> missing = new LinkedHashSet<>(ids);
        missing.removeAll(foundIds);

        if (!missing.isEmpty()) {
            throw new NotFoundException(
                    "Events with ids " + missing + " were not found"
            );
        }

        return ids;
    }

    private List<CompilationDto> toDtos(List<Compilation> compilations) {
        List<Long> eventIds = compilations.stream()
                .filter(compilation -> compilation.getEventIds() != null)
                .flatMap(compilation -> compilation.getEventIds().stream())
                .distinct()
                .toList();

        Map<Long, EventShortDto> eventsById = eventIds.isEmpty()
                ? Map.of()
                : eventServiceClient.getEventShorts(eventIds).stream()
                .collect(Collectors.toMap(
                        EventShortDto::getId,
                        Function.identity()
                ));

        if (eventsById.size() != eventIds.size()) {
            throw new NotFoundException(
                    "One or more events referenced by compilations were not found"
            );
        }

        return compilations.stream().map(compilation -> {
            List<EventShortDto> events = compilation.getEventIds() == null
                    ? List.of()
                    : compilation.getEventIds().stream()
                    .map(eventsById::get)
                    .filter(java.util.Objects::nonNull)
                    .toList();

            return compilationMapper.toDto(compilation, events);
        }).toList();
    }

    private CompilationDto toDto(Compilation compilation) {
        return toDtos(List.of(compilation)).get(0);
    }
}
