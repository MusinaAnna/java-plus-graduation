package ru.practicum.mainservice.compilations.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.common.dto.CompilationDto;
import ru.practicum.mainservice.compilations.dto.NewCompilationDto;
import ru.practicum.mainservice.compilations.dto.UpdateCompilationRequest;
import ru.practicum.mainservice.compilations.service.CompilationService;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminCompilationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CompilationService compilationService;

    @Test
    void saveCompilation_shouldReturnCreatedCompilation() throws Exception {
        NewCompilationDto request = new NewCompilationDto(
                Set.of(1L, 2L),
                true,
                "Летние события"
        );

        CompilationDto response = new CompilationDto(
                new LinkedHashSet<>(),
                1L,
                true,
                "Летние события"
        );

        when(compilationService.createCompilation(
                any(NewCompilationDto.class)
        )).thenReturn(response);

        mockMvc.perform(post("/admin/compilations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Летние события"))
                .andExpect(jsonPath("$.pinned").value(true))
                .andExpect(jsonPath("$.events").isArray());

        verify(compilationService)
                .createCompilation(any(NewCompilationDto.class));
    }

    @Test
    void saveCompilation_shouldReturnBadRequest_whenTitleIsBlank()
            throws Exception {
        NewCompilationDto request = new NewCompilationDto(
                Set.of(),
                false,
                ""
        );

        mockMvc.perform(post("/admin/compilations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void saveCompilation_shouldReturnBadRequest_whenTitleIsMissing()
            throws Exception {
        NewCompilationDto request = new NewCompilationDto(
                Set.of(),
                false,
                null
        );

        mockMvc.perform(post("/admin/compilations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateCompilation_shouldReturnUpdatedCompilation()
            throws Exception {
        UpdateCompilationRequest request =
                new UpdateCompilationRequest(
                        Set.of(2L),
                        false,
                        "Обновлённая подборка"
                );

        CompilationDto response = new CompilationDto(
                new LinkedHashSet<>(),
                1L,
                false,
                "Обновлённая подборка"
        );

        when(compilationService.updateCompilation(
                eq(1L),
                any(UpdateCompilationRequest.class)
        )).thenReturn(response);

        mockMvc.perform(patch("/admin/compilations/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title")
                        .value("Обновлённая подборка"))
                .andExpect(jsonPath("$.pinned").value(false));

        verify(compilationService).updateCompilation(
                eq(1L),
                any(UpdateCompilationRequest.class)
        );
    }

    @Test
    void updateCompilation_shouldAllowEmptyRequest() throws Exception {
        UpdateCompilationRequest request =
                new UpdateCompilationRequest();

        CompilationDto response = new CompilationDto(
                new LinkedHashSet<>(),
                1L,
                false,
                "Подборка"
        );

        when(compilationService.updateCompilation(
                eq(1L),
                any(UpdateCompilationRequest.class)
        )).thenReturn(response);

        mockMvc.perform(patch("/admin/compilations/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void updateCompilation_shouldReturnBadRequest_whenTitleTooLong()
            throws Exception {
        UpdateCompilationRequest request =
                new UpdateCompilationRequest();

        request.setTitle("а".repeat(51));

        mockMvc.perform(patch("/admin/compilations/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteCompilation_shouldReturnNoContent() throws Exception {
        doNothing().when(compilationService)
                .deleteCompilation(1L);

        mockMvc.perform(delete("/admin/compilations/1"))
                .andExpect(status().isNoContent());

        verify(compilationService).deleteCompilation(1L);
    }

    @Test
    void deleteCompilation_shouldReturnBadRequest_whenIdIsNotNumber()
            throws Exception {
        mockMvc.perform(delete("/admin/compilations/text"))
                .andExpect(status().isBadRequest());
    }
}