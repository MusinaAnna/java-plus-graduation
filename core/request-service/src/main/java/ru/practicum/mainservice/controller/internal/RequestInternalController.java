package ru.practicum.mainservice.controller.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.common.enums.RequestStatus;
import ru.practicum.mainservice.requests.repository.ParticipationRequestRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/requests")
public class RequestInternalController {
    private final ParticipationRequestRepository repository;

    @GetMapping("/confirmed-counts")
    public Map<Long, Long> getConfirmedCounts(@RequestParam("eventIds") List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Map.of();
        }
        return repository.countByEventIdsAndStatus(eventIds, RequestStatus.CONFIRMED).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
    }

    @GetMapping("/confirmed-participation")
    public boolean isConfirmedParticipant(@RequestParam("eventId") Long eventId,
                                          @RequestParam("userId") Long userId) {
        return repository.existsByEventIdAndRequesterIdAndStatus(eventId, userId, RequestStatus.CONFIRMED);
    }

    @GetMapping("/confirmed-count")
    public long getConfirmedCount(@RequestParam("eventId") Long eventId) {
        return repository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
    }

    @GetMapping("/exists-by-requester/{userId}")
    public boolean existsByRequesterId(@PathVariable("userId") Long userId) {
        return repository.existsByRequesterId(userId);
    }
}
