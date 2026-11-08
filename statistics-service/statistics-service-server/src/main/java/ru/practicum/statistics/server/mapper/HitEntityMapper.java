package ru.practicum.statistics.server.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;
import ru.practicum.dto.EndpointHit;
import ru.practicum.statistics.server.model.HitEntity;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface HitEntityMapper {
    HitEntity toEntity(EndpointHit endpointHit);
}
