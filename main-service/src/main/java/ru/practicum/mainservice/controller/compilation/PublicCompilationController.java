package ru.practicum.mainservice.controller.compilation;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.common.dto.CompilationDto;
import ru.practicum.mainservice.compilations.service.CompilationService;
import ru.practicum.mainservice.controller.api.compilation.PublicCompilationControllerApi;

import java.util.List;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/compilations")
public class PublicCompilationController implements PublicCompilationControllerApi {
    private final CompilationService compilationService;

    @Override
    public List<CompilationDto> getCompilations(
            @RequestParam(required = false) Boolean pinned,
            @RequestParam(defaultValue = "0") @PositiveOrZero int from,
            @RequestParam(defaultValue = "10") @Positive int size) {
        log.info("GET /compilations with pinned={}, from={}, size={}", pinned, from, size);
        return compilationService.getCompilations(pinned, PageRequest.of(from / size, size));
    }

    @Override
    public CompilationDto getCompilation(@PathVariable Long compId) {
        log.info("GET /compilations/{}", compId);
        return compilationService.getCompilation(compId);
    }
}
