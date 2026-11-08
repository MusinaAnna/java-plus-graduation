package ru.practicum.statistics.server.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.dto.EndpointHit;
import ru.practicum.dto.ViewStats;
import ru.practicum.statistics.server.mapper.HitEntityMapper;
import ru.practicum.statistics.server.model.HitEntity;
import ru.practicum.statistics.server.repository.StatsRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {

    private final StatsRepository repository;
    private final HitEntityMapper mapper;

    @Override
    public HitEntity save(EndpointHit endpointHit) {
        HitEntity hit = mapper.toEntity(endpointHit);
        HitEntity saveHit = repository.save(hit);
        return saveHit;
    }

    @Override
    public List<ViewStats> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, boolean unique) {
        return unique ? repository.findStatsUnique(start, end, uris) :
                repository.findStats(start, end, uris);
    }
}
