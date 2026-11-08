package ru.practicum.mainservice.events.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import ru.practicum.common.entity.Category;
import ru.practicum.common.entity.Location;
import ru.practicum.common.entity.User;
import ru.practicum.common.enums.EventState;
import ru.practicum.mainservice.categories.repository.CategoryRepository;
import ru.practicum.mainservice.events.entity.Event;
import ru.practicum.mainservice.users.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
public class EventRepositoryTest {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    private Category category;
    private User initiator;
    private Event event1;
    private Event event2;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();

        category = new Category();
        category.setName("Концерты");
        category = categoryRepository.save(category);

        initiator = new User();
        initiator.setName("Иван Петров");
        initiator.setEmail("ivan@mail.ru");
        initiator = userRepository.save(initiator);

        Location location = new Location();
        location.setLat(55.754167f);
        location.setLon(37.62f);

        event1 = new Event();
        event1.setAnnotation("Тестовое событие 1");
        event1.setDescription("Описание события 1");
        event1.setTitle("Событие 1");
        event1.setCategory(category);
        event1.setInitiator(initiator);
        event1.setLocation(location);
        event1.setEventDate(now.plusDays(5));
        event1.setCreatedOn(now);
        event1.setState(EventState.PENDING);
        event1.setPaid(false);
        event1.setParticipantLimit(10);
        event1.setRequestModeration(true);

        event2 = new Event();
        event2.setAnnotation("Тестовое событие 2");
        event2.setDescription("Описание события 2");
        event2.setTitle("Событие 2");
        event2.setCategory(category);
        event2.setInitiator(initiator);
        event2.setLocation(location);
        event2.setEventDate(now.plusDays(10));
        event2.setCreatedOn(now);
        event2.setState(EventState.PUBLISHED);
        event2.setPaid(true);
        event2.setParticipantLimit(20);
        event2.setRequestModeration(false);

        event1 = eventRepository.save(event1);
        event2 = eventRepository.save(event2);
    }

    @Test
    void findAllByAdminFilters_shouldReturnAllEvents_whenNoFilters() {
        Page<Event> result = eventRepository.findAllByAdminFilters(
                null, null, null, null, null, PageRequest.of(0, 10)
        );

        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    void findAllByAdminFilters_shouldFilterByUsers() {
        Page<Event> result = eventRepository.findAllByAdminFilters(
                List.of(initiator.getId()), null, null, null, null, PageRequest.of(0, 10)
        );

        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    void findAllByAdminFilters_shouldFilterByStates() {
        Page<Event> result = eventRepository.findAllByAdminFilters(
                null, List.of(EventState.PENDING), null, null, null, PageRequest.of(0, 10)
        );

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getState()).isEqualTo(EventState.PENDING);
    }

    @Test
    void findAllByAdminFilters_shouldFilterByCategories() {
        Page<Event> result = eventRepository.findAllByAdminFilters(
                null, null, List.of(category.getId()), null, null, PageRequest.of(0, 10)
        );

        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    void findAllByAdminFilters_shouldFilterByDateRange() {
        Page<Event> result = eventRepository.findAllByAdminFilters(
                null, null, null, now.plusDays(1), now.plusDays(7), PageRequest.of(0, 10)
        );

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getEventDate()).isBefore(now.plusDays(7));
    }

    @Test
    void findAllByAdminFilters_shouldReturnEmpty_whenNoMatches() {
        Page<Event> result = eventRepository.findAllByAdminFilters(
                null, null, null, now.plusDays(20), now.plusDays(30), PageRequest.of(0, 10)
        );

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void findAllByAdminFilters_shouldHandleAllFiltersTogether() {
        Page<Event> result = eventRepository.findAllByAdminFilters(
                List.of(initiator.getId()),
                List.of(EventState.PENDING),
                List.of(category.getId()),
                now.plusDays(1),
                now.plusDays(7),
                PageRequest.of(0, 10)
        );

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(event1.getId());
    }

    @Test
    void findAllByAdminFilters_shouldRespectPagination() {
        Page<Event> page1 = eventRepository.findAllByAdminFilters(
                null, null, null, null, null, PageRequest.of(0, 1)
        );
        Page<Event> page2 = eventRepository.findAllByAdminFilters(
                null, null, null, null, null, PageRequest.of(1, 1)
        );

        assertThat(page1.getContent()).hasSize(1);
        assertThat(page2.getContent()).hasSize(1);
        assertThat(page1.getContent().get(0).getId()).isNotEqualTo(page2.getContent().get(0).getId());
    }

    @Test
    void findByInitiatorId_shouldReturnEventsByUser() {
        Page<Event> result = eventRepository.findByInitiatorId(initiator.getId(), PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getInitiator().getId()).isEqualTo(initiator.getId());
        assertThat(result.getContent().get(1).getInitiator().getId()).isEqualTo(initiator.getId());
    }

    @Test
    void findByInitiatorId_shouldReturnEmpty_whenUserHasNoEvents() {
        User otherUser = new User();
        otherUser.setName("Другой пользователь");
        otherUser.setEmail("other@mail.ru");
        otherUser = userRepository.save(otherUser);

        Page<Event> result = eventRepository.findByInitiatorId(
                otherUser.getId(), PageRequest.of(0, 10)
        );

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void findByInitiatorId_shouldRespectPagination() {
        Location location = new Location();
        location.setLat(55.754167f);
        location.setLon(37.62f);

        Event event3 = new Event();
        event3.setAnnotation("Тестовое событие 3");
        event3.setDescription("Описание события 3");
        event3.setTitle("Событие 3");
        event3.setCategory(category);
        event3.setInitiator(initiator);
        event3.setLocation(location);
        event3.setEventDate(now.plusDays(15));
        event3.setCreatedOn(now);
        event3.setState(EventState.PENDING);
        event3.setPaid(false);
        event3.setParticipantLimit(10);
        event3.setRequestModeration(true);
        eventRepository.save(event3);

        Page<Event> page1 = eventRepository.findByInitiatorId(
                initiator.getId(), PageRequest.of(0, 2)
        );
        Page<Event> page2 = eventRepository.findByInitiatorId(
                initiator.getId(), PageRequest.of(1, 2)
        );

        assertThat(page1.getContent()).hasSize(2);
        assertThat(page2.getContent()).hasSize(1);
        assertThat(page1.getContent().get(0).getId()).isNotEqualTo(page2.getContent().get(0).getId());
    }

    @Test
    void findPublishedEvents_shouldReturnOnlyPublishedEvents() {

        Page<Event> result = eventRepository.findPublishedEvents(
                null, null, null, now.minusDays(1), null, PageRequest.of(0, 10)
        );

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getState()).isEqualTo(EventState.PUBLISHED);
        assertThat(result.getContent().get(0).getId()).isEqualTo(event2.getId());
    }

    @Test
    void findPublishedEvents_shouldFilterByText() {
        Page<Event> result = eventRepository.findPublishedEvents(
                "Тестовое событие 2", null, null, now.minusDays(1), null, PageRequest.of(0, 10)
        );

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getAnnotation()).contains("Тестовое событие 2");
    }

    @Test
    void findPublishedEvents_shouldFilterByCategories() {
        Page<Event> result = eventRepository.findPublishedEvents(
                null, List.of(category.getId()), null, now.minusDays(1), null, PageRequest.of(0, 10)
        );

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void findPublishedEvents_shouldFilterByPaid() {
        Page<Event> result = eventRepository.findPublishedEvents(
                null, null, true, now.minusDays(1), null, PageRequest.of(0, 10)
        );

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getPaid()).isTrue();
    }

    @Test
    void findPublishedEvents_shouldFilterByDateRange() {
        Page<Event> result = eventRepository.findPublishedEvents(
                null, null, null, now.plusDays(5), now.plusDays(15), PageRequest.of(0, 10)
        );

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(event2.getId());
    }

    @Test
    void findPublishedEvents_shouldReturnEmpty_whenNoPublishedEvents() {
        Location location = new Location();
        location.setLat(55.754167f);
        location.setLon(37.62f);

        Event event3 = new Event();
        event3.setAnnotation("Тестовое событие 3");
        event3.setDescription("Описание события 3");
        event3.setTitle("Событие 3");
        event3.setCategory(category);
        event3.setInitiator(initiator);
        event3.setLocation(location);
        event3.setEventDate(now.plusDays(20));
        event3.setCreatedOn(now);
        event3.setState(EventState.PENDING);
        event3.setPaid(false);
        event3.setParticipantLimit(10);
        event3.setRequestModeration(true);
        eventRepository.save(event3);

        Page<Event> result = eventRepository.findPublishedEvents(
                null, null, null, now.plusDays(15), now.plusDays(25), PageRequest.of(0, 10)
        );

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void findPublishedEvents_shouldRespectPagination() {
        Location location = new Location();
        location.setLat(55.754167f);
        location.setLon(37.62f);

        Event event3 = new Event();
        event3.setAnnotation("Тестовое событие 3");
        event3.setDescription("Описание события 3");
        event3.setTitle("Событие 3");
        event3.setCategory(category);
        event3.setInitiator(initiator);
        event3.setLocation(location);
        event3.setEventDate(now.plusDays(15));
        event3.setCreatedOn(now);
        event3.setState(EventState.PUBLISHED);
        event3.setPaid(false);
        event3.setParticipantLimit(10);
        event3.setRequestModeration(true);
        eventRepository.save(event3);

        Page<Event> page1 = eventRepository.findPublishedEvents(
                null, null, null, now.minusDays(1), null, PageRequest.of(0, 1)
        );
        Page<Event> page2 = eventRepository.findPublishedEvents(
                null, null, null, now.minusDays(1), null, PageRequest.of(1, 1)
        );

        assertThat(page1.getContent()).hasSize(1);
        assertThat(page2.getContent()).hasSize(1);
        assertThat(page1.getContent().get(0).getId()).isNotEqualTo(page2.getContent().get(0).getId());
    }
}
