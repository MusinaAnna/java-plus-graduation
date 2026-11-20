package ru.practicum.mainservice.events.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import ru.practicum.common.entity.Location;
import ru.practicum.common.enums.EventState;
import ru.practicum.mainservice.events.entity.Event;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(EventRepositoryCustom.class)
@ActiveProfiles("test")
class EventRepositoryTest {
    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EventRepositoryCustom eventRepositoryCustom;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();
        Location location = new Location(55.75f, 37.61f);
        eventRepository.save(createEvent(101L, 201L, EventState.PENDING, now.plusDays(5), location));
        eventRepository.save(createEvent(102L, 202L, EventState.PUBLISHED, now.plusDays(10), location));
    }

    @Test
    void findAllByAdminFilters_shouldReturnAllEventsWithoutFilters() {
        Page<Event> result = eventRepositoryCustom.findAllByAdminFilters(null, null, null, null, null,
                PageRequest.of(0, 10));
        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    void findAllByAdminFilters_shouldFilterByInitiatorId() {
        Page<Event> result = eventRepositoryCustom.findAllByAdminFilters(List.of(101L), null, null, null, null,
                PageRequest.of(0, 10));
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getInitiatorId()).isEqualTo(101L);
    }

    @Test
    void findAllByAdminFilters_shouldFilterByCategoryId() {
        Page<Event> result = eventRepositoryCustom.findAllByAdminFilters(null, null, List.of(202L), null, null,
                PageRequest.of(0, 10));
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getCategoryId()).isEqualTo(202L);
    }

    @Test
    void findByInitiatorId_shouldReturnOnlyOwnersEvents() {
        Page<Event> result = eventRepository.findByInitiatorId(101L, PageRequest.of(0, 10));
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getInitiatorId()).isEqualTo(101L);
    }

    @Test
    void existsByCategoryId_shouldCheckCategoryReferencesWithoutForeignDatabase() {
        assertThat(eventRepository.existsByCategoryId(202L)).isTrue();
        assertThat(eventRepository.existsByCategoryId(999L)).isFalse();
    }

    private Event createEvent(Long initiatorId, Long categoryId, EventState state,
                              LocalDateTime eventDate, Location location) {
        Event event = new Event();
        event.setAnnotation("Тестовая аннотация события");
        event.setDescription("Тестовое описание события для проверки");
        event.setTitle("Тестовое событие");
        event.setInitiatorId(initiatorId);
        event.setCategoryId(categoryId);
        event.setLocation(location);
        event.setEventDate(eventDate);
        event.setCreatedOn(now);
        event.setState(state);
        event.setPaid(false);
        event.setParticipantLimit(10);
        event.setRequestModeration(true);
        return event;
    }
}
