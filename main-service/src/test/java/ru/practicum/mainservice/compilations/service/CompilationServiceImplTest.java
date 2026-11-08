package ru.practicum.mainservice.compilations.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import ru.practicum.common.dto.CompilationDto;
import ru.practicum.common.exception.ConflictException;
import ru.practicum.common.exception.NotFoundException;
import ru.practicum.mainservice.compilations.dto.NewCompilationDto;
import ru.practicum.mainservice.compilations.dto.UpdateCompilationRequest;
import ru.practicum.mainservice.compilations.entity.Compilation;
import ru.practicum.mainservice.compilations.mapper.CompilationMapper;
import ru.practicum.mainservice.compilations.repository.CompilationRepository;
import ru.practicum.mainservice.events.entity.Event;
import ru.practicum.mainservice.events.repository.EventRepository;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompilationServiceImplTest {

    @Mock
    private CompilationRepository compilationRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private CompilationMapper compilationMapper;

    @InjectMocks
    private CompilationServiceImpl compilationService;

    private Compilation compilation;
    private CompilationDto compilationDto;
    private NewCompilationDto newCompilationDto;

    @BeforeEach
    void setUp() {
        compilation = new Compilation();
        compilation.setId(1L);
        compilation.setTitle("Летние события");
        compilation.setPinned(true);
        compilation.setEvents(new LinkedHashSet<>());

        compilationDto = new CompilationDto(
                new LinkedHashSet<>(),
                1L,
                true,
                "Летние события"
        );

        newCompilationDto = new NewCompilationDto(
                new LinkedHashSet<>(),
                true,
                "Летние события"
        );
    }

    @Test
    void createCompilation_shouldCreateCompilationWithoutEvents() {
        when(compilationMapper.toEntity(
                newCompilationDto,
                new LinkedHashSet<>()
        )).thenReturn(compilation);

        when(compilationRepository.saveAndFlush(compilation))
                .thenReturn(compilation);

        when(compilationMapper.toDto(compilation))
                .thenReturn(compilationDto);

        CompilationDto result =
                compilationService.createCompilation(newCompilationDto);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("Летние события");
        assertThat(result.getPinned()).isTrue();

        verify(compilationRepository)
                .saveAndFlush(compilation);

        verify(eventRepository, never())
                .findAllById(any());
    }

    @Test
    void createCompilation_shouldCreateCompilationWithEvents() {
        Event secondEvent = new Event();
        secondEvent.setId(2L);

        Event firstEvent = new Event();
        firstEvent.setId(1L);

        newCompilationDto.setEvents(
                new LinkedHashSet<>(Set.of(2L, 1L))
        );

        when(eventRepository.findAllById(
                newCompilationDto.getEvents()
        )).thenReturn(List.of(secondEvent, firstEvent));

        when(compilationMapper.toEntity(
                any(NewCompilationDto.class),
                anySet()
        )).thenReturn(compilation);

        when(compilationRepository.saveAndFlush(compilation))
                .thenReturn(compilation);

        when(compilationMapper.toDto(compilation))
                .thenReturn(compilationDto);

        compilationService.createCompilation(newCompilationDto);

        ArgumentCaptor<Set<Event>> eventsCaptor =
                ArgumentCaptor.forClass(Set.class);

        verify(compilationMapper).toEntity(
                any(NewCompilationDto.class),
                eventsCaptor.capture()
        );

        assertThat(eventsCaptor.getValue())
                .extracting(Event::getId)
                .containsExactly(1L, 2L);
    }

    @Test
    void createCompilation_shouldThrowNotFound_whenEventMissing() {
        Event event = new Event();
        event.setId(1L);

        newCompilationDto.setEvents(
                new LinkedHashSet<>(Set.of(1L, 2L))
        );

        when(eventRepository.findAllById(
                newCompilationDto.getEvents()
        )).thenReturn(List.of(event));

        assertThatThrownBy(() ->
                compilationService.createCompilation(
                        newCompilationDto
                ))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("2")
                .hasMessageContaining("were not found");

        verify(compilationRepository, never())
                .saveAndFlush(any(Compilation.class));
    }

    @Test
    void createCompilation_shouldThrowConflict_whenTitleExists() {
        when(compilationMapper.toEntity(
                newCompilationDto,
                new LinkedHashSet<>()
        )).thenReturn(compilation);

        when(compilationRepository.saveAndFlush(compilation))
                .thenThrow(new DataIntegrityViolationException(
                        "Duplicate title"
                ));

        assertThatThrownBy(() ->
                compilationService.createCompilation(
                        newCompilationDto
                ))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Летние события")
                .hasMessageContaining("already exists");
    }

    @Test
    void updateCompilation_shouldUpdateAllFields() {
        UpdateCompilationRequest request =
                new UpdateCompilationRequest(
                        new LinkedHashSet<>(),
                        false,
                        "Новая подборка"
                );

        when(compilationRepository.findById(1L))
                .thenReturn(Optional.of(compilation));

        when(compilationRepository.save(compilation))
                .thenReturn(compilation);

        CompilationDto updatedDto = new CompilationDto(
                new LinkedHashSet<>(),
                1L,
                false,
                "Новая подборка"
        );

        when(compilationMapper.toDto(compilation))
                .thenReturn(updatedDto);

        CompilationDto result =
                compilationService.updateCompilation(1L, request);

        assertThat(result.getTitle())
                .isEqualTo("Новая подборка");

        assertThat(result.getPinned()).isFalse();
        assertThat(compilation.getTitle())
                .isEqualTo("Новая подборка");
        assertThat(compilation.getPinned()).isFalse();
        assertThat(compilation.getEvents()).isEmpty();

        verify(compilationRepository).save(compilation);
    }

    @Test
    void updateCompilation_shouldUpdateOnlyProvidedFields() {
        UpdateCompilationRequest request =
                new UpdateCompilationRequest();

        request.setPinned(false);

        when(compilationRepository.findById(1L))
                .thenReturn(Optional.of(compilation));

        when(compilationRepository.save(compilation))
                .thenReturn(compilation);

        when(compilationMapper.toDto(compilation))
                .thenReturn(compilationDto);

        compilationService.updateCompilation(1L, request);

        assertThat(compilation.getTitle())
                .isEqualTo("Летние события");

        assertThat(compilation.getPinned()).isFalse();
    }

    @Test
    void updateCompilation_shouldReplaceEvents() {
        Event secondEvent = new Event();
        secondEvent.setId(2L);

        Event firstEvent = new Event();
        firstEvent.setId(1L);

        UpdateCompilationRequest request =
                new UpdateCompilationRequest();

        request.setEvents(
                new LinkedHashSet<>(Set.of(2L, 1L))
        );

        when(compilationRepository.findById(1L))
                .thenReturn(Optional.of(compilation));

        when(eventRepository.findAllById(request.getEvents()))
                .thenReturn(List.of(secondEvent, firstEvent));

        when(compilationRepository.save(compilation))
                .thenReturn(compilation);

        when(compilationMapper.toDto(compilation))
                .thenReturn(compilationDto);

        compilationService.updateCompilation(1L, request);

        assertThat(compilation.getEvents())
                .extracting(Event::getId)
                .containsExactly(1L, 2L);
    }

    @Test
    void updateCompilation_shouldClearEvents_whenEmptySetProvided() {
        Event event = new Event();
        event.setId(1L);

        compilation.setEvents(
                new LinkedHashSet<>(Set.of(event))
        );

        UpdateCompilationRequest request =
                new UpdateCompilationRequest();

        request.setEvents(new LinkedHashSet<>());

        when(compilationRepository.findById(1L))
                .thenReturn(Optional.of(compilation));

        when(compilationRepository.save(compilation))
                .thenReturn(compilation);

        when(compilationMapper.toDto(compilation))
                .thenReturn(compilationDto);

        compilationService.updateCompilation(1L, request);

        assertThat(compilation.getEvents()).isEmpty();

        verify(eventRepository, never())
                .findAllById(any());
    }

    @Test
    void updateCompilation_shouldThrowNotFound_whenCompilationMissing() {
        UpdateCompilationRequest request =
                new UpdateCompilationRequest();

        when(compilationRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                compilationService.updateCompilation(
                        999L,
                        request
                ))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining(
                        "Compilation with id=999 was not found"
                );

        verify(compilationRepository, never())
                .save(any(Compilation.class));
    }

    @Test
    void updateCompilation_shouldThrowNotFound_whenEventMissing() {
        Event event = new Event();
        event.setId(1L);

        UpdateCompilationRequest request =
                new UpdateCompilationRequest();

        request.setEvents(
                new LinkedHashSet<>(Set.of(1L, 2L))
        );

        when(compilationRepository.findById(1L))
                .thenReturn(Optional.of(compilation));

        when(eventRepository.findAllById(request.getEvents()))
                .thenReturn(List.of(event));

        assertThatThrownBy(() ->
                compilationService.updateCompilation(
                        1L,
                        request
                ))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("2");

        verify(compilationRepository, never())
                .save(any(Compilation.class));
    }

    @Test
    void updateCompilation_shouldThrowConflict_whenTitleExists() {
        UpdateCompilationRequest request =
                new UpdateCompilationRequest();

        request.setTitle("Повторяющаяся подборка");

        when(compilationRepository.findById(1L))
                .thenReturn(Optional.of(compilation));

        when(compilationRepository.save(compilation))
                .thenThrow(new DataIntegrityViolationException(
                        "Duplicate title"
                ));

        assertThatThrownBy(() ->
                compilationService.updateCompilation(
                        1L,
                        request
                ))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining(
                        "Повторяющаяся подборка"
                );
    }

    @Test
    void deleteCompilation_shouldDeleteCompilation() {
        when(compilationRepository.findById(1L))
                .thenReturn(Optional.of(compilation));

        compilationService.deleteCompilation(1L);

        verify(compilationRepository).delete(compilation);
    }

    @Test
    void deleteCompilation_shouldThrowNotFound_whenMissing() {
        when(compilationRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                compilationService.deleteCompilation(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining(
                        "Compilation with id=999 was not found"
                );

        verify(compilationRepository, never())
                .delete(any(Compilation.class));
    }

    @Test
    void getCompilations_shouldReturnAll_whenPinnedIsNull() {
        PageRequest pageable = PageRequest.of(0, 10);
        List<Long> ids = List.of(1L);
        Page<Long> idsPage = new PageImpl<>(ids, pageable, 1);

        when(compilationRepository.findCompilationIds(
                null,
                pageable
        )).thenReturn(idsPage);

        when(compilationRepository.findAllByIdsWithEvents(ids))
                .thenReturn(List.of(compilation));

        when(compilationMapper.toDto(compilation))
                .thenReturn(compilationDto);

        List<CompilationDto> result = compilationService.getCompilations(
                null, pageable
        );

        assertThat(result).containsExactly(compilationDto);

        verify(compilationRepository).findCompilationIds(null, pageable);

        verify(compilationRepository).findAllByIdsWithEvents(ids);

        verify(compilationMapper).toDto(compilation);
    }

    @Test
    void getCompilations_shouldFilterByPinned() {
        PageRequest pageable = PageRequest.of(0, 10);
        List<Long> ids = List.of(1L);
        Page<Long> idsPage = new PageImpl<>(ids, pageable, 1);

        when(compilationRepository.findCompilationIds(
                true,
                pageable
        )).thenReturn(idsPage);

        when(compilationRepository.findAllByIdsWithEvents(ids))
                .thenReturn(List.of(compilation));

        when(compilationMapper.toDto(compilation))
                .thenReturn(compilationDto);

        List<CompilationDto> result =
                compilationService.getCompilations(
                        true,
                        pageable
                );

        assertThat(result).containsExactly(compilationDto);

        verify(compilationRepository).findCompilationIds(true, pageable);

        verify(compilationRepository).findAllByIdsWithEvents(ids);

        verify(compilationMapper).toDto(compilation);
    }

    @Test
    void getCompilation_shouldReturnCompilation() {
        when(compilationRepository.findById(1L))
                .thenReturn(Optional.of(compilation));

        when(compilationMapper.toDto(compilation))
                .thenReturn(compilationDto);

        CompilationDto result =
                compilationService.getCompilation(1L);

        assertThat(result).isEqualTo(compilationDto);
    }

    @Test
    void getCompilation_shouldThrowNotFound_whenMissing() {
        when(compilationRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                compilationService.getCompilation(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining(
                        "Compilation with id=999 was not found"
                );
    }
}