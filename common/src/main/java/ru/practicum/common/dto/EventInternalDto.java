package ru.practicum.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.common.enums.EventState;

import java.time.LocalDateTime;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventInternalDto {
    private Long id;
    private Long initiatorId;
    private Long categoryId;
    private EventState state;
    private Integer participantLimit;
    private Boolean requestModeration;
    private LocalDateTime eventDate;
}
