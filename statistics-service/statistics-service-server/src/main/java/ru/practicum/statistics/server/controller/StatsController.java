package ru.practicum.statistics.server.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.common.exception.BadRequestException;
import ru.practicum.dto.EndpointHit;
import ru.practicum.dto.ViewStats;
import ru.practicum.statistics.server.controller.api.StatsControllerApi;
import ru.practicum.statistics.server.model.HitEntity;
import ru.practicum.statistics.server.service.StatsService;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import static ru.practicum.common.config.JacksonConfig.DATE_TIME_FORMAT;

@RestController
@RequiredArgsConstructor
@Slf4j
public class StatsController implements StatsControllerApi {
    private final StatsService service;

    @Override
    public Collection<ViewStats> getAll(@RequestParam(required = true) @DateTimeFormat(pattern = DATE_TIME_FORMAT) LocalDateTime start,
                                        @RequestParam(required = true) @DateTimeFormat(pattern = DATE_TIME_FORMAT) LocalDateTime end,
                                        @RequestParam(required = false) List<String> uris,
                                        @RequestParam(defaultValue = "false") boolean unique) {
        log.info("GET /stats REQUEST: start= {} end= {} uris_count= {} unique= {}",
                start, end, uris != null ? uris.size() : 0, unique);

        if (start.isAfter(end)) {
            throw new BadRequestException("Start date must be before or equal to end date");
        }

        List<ViewStats> stats = service.getStats(start, end, uris, unique);
        log.info("GET /stats RESPONSE: stats_size= {}", stats.size());
        return stats;
    }

    @Override
    public void post(@RequestBody EndpointHit endpointHit) {
        log.info("POST /hit REQUEST: endpointHit= {}", endpointHit);
        HitEntity hit = service.save(endpointHit);
        log.info("POST / hit RESPONSE: hit= {}", hit);
    }
}
