package ru.practicum.mainservice.compilations.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.common.dto.CompilationDto;
import ru.practicum.mainservice.compilations.service.CompilationService;

import java.util.LinkedHashSet;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PublicCompilationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CompilationService compilationService;

    @Test
    void getCompilations_shouldReturnCompilations() throws Exception {
        CompilationDto first = new CompilationDto(
                new LinkedHashSet<>(),
                1L,
                true,
                "Закреплённая подборка"
        );

        CompilationDto second = new CompilationDto(
                new LinkedHashSet<>(),
                2L,
                false,
                "Обычная подборка"
        );

        when(compilationService.getCompilations(
                eq(null),
                eq(PageRequest.of(0, 10))
        )).thenReturn(List.of(first, second));

        mockMvc.perform(get("/compilations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].title")
                        .value("Закреплённая подборка"))
                .andExpect(jsonPath("$[1].id").value(2L));

        verify(compilationService).getCompilations(
                null,
                PageRequest.of(0, 10)
        );
    }

    @Test
    void getCompilations_shouldPassPinnedFilter() throws Exception {
        CompilationDto compilation = new CompilationDto(
                new LinkedHashSet<>(),
                1L,
                true,
                "Закреплённая подборка"
        );

        when(compilationService.getCompilations(
                true,
                PageRequest.of(0, 10)
        )).thenReturn(List.of(compilation));

        mockMvc.perform(get("/compilations")
                        .param("pinned", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].pinned").value(true));

        verify(compilationService).getCompilations(
                true,
                PageRequest.of(0, 10)
        );
    }

    @Test
    void getCompilations_shouldCalculatePageFromOffset()
            throws Exception {
        when(compilationService.getCompilations(
                false,
                PageRequest.of(2, 5)
        )).thenReturn(List.of());

        mockMvc.perform(get("/compilations")
                        .param("pinned", "false")
                        .param("from", "10")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        verify(compilationService).getCompilations(
                false,
                PageRequest.of(2, 5)
        );
    }

    @Test
    void getCompilations_shouldReturnBadRequest_whenPinnedIsInvalid()
            throws Exception {
        mockMvc.perform(get("/compilations")
                        .param("pinned", "incorrect"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getCompilation_shouldReturnCompilation() throws Exception {
        CompilationDto compilation = new CompilationDto(
                new LinkedHashSet<>(),
                1L,
                true,
                "Подборка"
        );

        when(compilationService.getCompilation(1L))
                .thenReturn(compilation);

        mockMvc.perform(get("/compilations/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Подборка"))
                .andExpect(jsonPath("$.pinned").value(true));

        verify(compilationService).getCompilation(1L);
    }

    @Test
    void getCompilation_shouldReturnBadRequest_whenIdIsNotNumber()
            throws Exception {
        mockMvc.perform(get("/compilations/text"))
                .andExpect(status().isBadRequest());
    }
}