
package ru.practicum.mainservice.compilations.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompilationServiceImplTest {

    @Mock
    private CompilationRepository compilationRepository;

    @Mock
    private CompilationMapper compilationMapper;

    @Mock
    private EventServiceClient eventServiceClient;

    @InjectMocks
    private CompilationServiceImpl compilationService;

    private Compilation compilation;
    private CompilationDto compilationDto;
    private NewCompilationDto newCompilationDto;

    @BeforeEach
    void setUp() {
        compilation = new Compilation();
        compilation.setId(1L);
        compilation.setTitle("Подборка");
        compilation.setPinned(true);
        compilation.setEventIds(new ArrayList<>());

        compilationDto = new CompilationDto(
                new LinkedHashSet<>(), 1L, true, "Подборка"
        );

        newCompilationDto = new NewCompilationDto(
                new LinkedHashSet<>(), true, "Подборка"
        );
    }

    @Test
    void createCompilation_shouldPersistIdsWithoutCrossServiceEntityAccess() {
        when(compilationMapper.toEntity(newCompilationDto, List.of()))
                .thenReturn(compilation);

        when(compilationRepository.saveAndFlush(compilation))
                .thenReturn(compilation);

        when(compilationMapper.toDto(compilation, List.of()))
                .thenReturn(compilationDto);

        CompilationDto result =
                compilationService.createCompilation(newCompilationDto);

        assertThat(result.getId()).isEqualTo(1L);

        verify(eventServiceClient, never()).getEventShorts(any());
        verify(compilationRepository).saveAndFlush(compilation);
    }

    @Test
    void createCompilation_shouldValidateEventIdsThroughFeignContract() {
        newCompilationDto.setEvents(
                new LinkedHashSet<>(Set.of(2L, 1L))
        );

        EventShortDto first = new EventShortDto();
        first.setId(1L);

        EventShortDto second = new EventShortDto();
        second.setId(2L);

        when(eventServiceClient.getEventShorts(List.of(1L, 2L)))
                .thenReturn(List.of(first, second));

        when(compilationMapper.toEntity(
                newCompilationDto, List.of(1L, 2L)
        )).thenReturn(compilation);

        when(compilationRepository.saveAndFlush(compilation))
                .thenReturn(compilation);

        when(compilationMapper.toDto(compilation, List.of()))
                .thenReturn(compilationDto);

        compilationService.createCompilation(newCompilationDto);

        verify(eventServiceClient).getEventShorts(List.of(1L, 2L));

        verify(compilationMapper).toEntity(
                newCompilationDto, List.of(1L, 2L)
        );
    }

    @Test
    void createCompilation_shouldRejectMissingEvent() {
        newCompilationDto.setEvents(
                new LinkedHashSet<>(Set.of(1L, 2L))
        );

        EventShortDto first = new EventShortDto();
        first.setId(1L);

        when(eventServiceClient.getEventShorts(List.of(1L, 2L)))
                .thenReturn(List.of(first));

        assertThatThrownBy(
                () -> compilationService.createCompilation(newCompilationDto)
        )
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("2");

        verify(compilationRepository, never()).saveAndFlush(any());
    }

    @Test
    void createCompilation_shouldTranslateDuplicateTitle() {
        when(compilationMapper.toEntity(newCompilationDto, List.of()))
                .thenReturn(compilation);

        when(compilationRepository.saveAndFlush(compilation))
                .thenThrow(DataIntegrityViolationException.class);

        assertThatThrownBy(
                () -> compilationService.createCompilation(newCompilationDto)
        )
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void updateCompilation_shouldMutateManagedEventCollectionInPlace() {
        List<Long> managedEventIds = new ArrayList<>(List.of(99L));
        compilation.setEventIds(managedEventIds);

        UpdateCompilationRequest request = new UpdateCompilationRequest(
                new LinkedHashSet<>(List.of(2L, 1L)),
                null,
                null
        );

        EventShortDto first = new EventShortDto();
        first.setId(1L);

        EventShortDto second = new EventShortDto();
        second.setId(2L);

        when(compilationRepository.findById(1L))
                .thenReturn(Optional.of(compilation));

        when(eventServiceClient.getEventShorts(List.of(1L, 2L)))
                .thenReturn(List.of(first, second));

        when(compilationMapper.toDto(eq(compilation), anyList()))
                .thenReturn(compilationDto);

        CompilationDto result =
                compilationService.updateCompilation(1L, request);

        assertThat(result).isSameAs(compilationDto);

        assertThat(compilation.getEventIds()).isSameAs(managedEventIds);

        assertThat(compilation.getEventIds())
                .containsExactly(1L, 2L);

        verify(compilationRepository).flush();

        verify(compilationRepository, never()).save(any(Compilation.class));
    }

    @Test
    void getCompilation_shouldThrowWhenMissing() {
        when(compilationRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> compilationService.getCompilation(99L)
        )
                .isInstanceOf(NotFoundException.class);
    }
}
