package ru.practicum.mainservice.events.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.common.dto.CategoryDto;
import ru.practicum.common.dto.EventInternalDto;
import ru.practicum.common.dto.EventShortDto;
import ru.practicum.common.dto.UserDto;
import ru.practicum.common.dto.UserShortDto;
import ru.practicum.common.enums.AdminStateAction;
import ru.practicum.common.enums.EventState;
import ru.practicum.common.enums.SortType;
import ru.practicum.common.exception.BadRequestException;
import ru.practicum.common.exception.ConflictException;
import ru.practicum.common.exception.NotFoundException;
import ru.practicum.common.exception.ValidationException;
import ru.practicum.dto.ViewStats;
import ru.practicum.mainservice.clients.CategoryServiceClient;
import ru.practicum.mainservice.clients.RequestServiceClient;
import ru.practicum.mainservice.clients.UserServiceClient;
import ru.practicum.mainservice.events.dto.EventFullDto;
import ru.practicum.mainservice.events.dto.NewEventDto;
import ru.practicum.mainservice.events.dto.UpdateEventAdminRequest;
import ru.practicum.mainservice.events.dto.UpdateEventUserRequest;
import ru.practicum.mainservice.events.entity.Event;
import ru.practicum.mainservice.events.mapper.EventMapper;
import ru.practicum.mainservice.events.repository.EventRepository;
import ru.practicum.mainservice.events.repository.EventRepositoryCustom;
import ru.practicum.mainservice.events.utils.EventUpdateUtils;
import ru.practicum.statistics.client.controller.StatsClient;
import ru.practicum.statistics.client.exception.StatsServerUnavailable;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import static ru.practicum.common.config.JacksonConfig.DATE_TIME_FORMAT;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {
    private final EventRepository repository;
    private final EventMapper mapper;
    private final StatsClient statsClient;
    private final UserServiceClient userServiceClient;
    private final CategoryServiceClient categoryServiceClient;
    private final EventRepositoryCustom eventRepositoryCustom;
    private final RequestServiceClient requestServiceClient;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT);

    @Override
    public List<EventFullDto> getAdminEvents(List<Long> users, List<EventState> states, List<Long> categories,
                                             LocalDateTime rangeStart, LocalDateTime rangeEnd, int from, int size) {
        checkPagination(from, size);
        boolean findWithConfirmedRequests = false;
        if (users != null && users.contains(0L)) {
            findWithConfirmedRequests = true;
            users = null;
        }
        if (categories != null && categories.contains(0L)) {
            categories = null;
        }

        Pageable pageable = findWithConfirmedRequests ? Pageable.unpaged() : PageRequest.of(0, from + size);
        List<Event> events = new ArrayList<>(eventRepositoryCustom.findAllByAdminFilters(
                users, states, categories, rangeStart, rangeEnd, pageable).getContent());
        Map<Long, Long> confirmedCounts = getConfirmedRequestsCounts(events);
        if (findWithConfirmedRequests) {
            events.removeIf(event -> confirmedCounts.getOrDefault(event.getId(), 0L) == 0);
        }
        events = takePage(events, from, size);
        Map<Long, Long> viewsCounts = getViewsCounts(events);
        return enrichFullEvents(events, confirmedCounts, viewsCounts);
    }

    @Override
    @Transactional
    public EventFullDto updateAdminEvent(Long eventId, UpdateEventAdminRequest request) {
        Event event = getEventEntity(eventId);
        if (request.getStateAction() == AdminStateAction.PUBLISH_EVENT) {
            if (event.getState() != EventState.PENDING) {
                throw new ConflictException("Cannot publish the event because it's not in PENDING state");
            }
            if (request.getEventDate() != null
                    && request.getEventDate().isBefore(LocalDateTime.now().plusHours(1))) {
                throw new ConflictException("Event date must be at least 1 hour from now");
            }
        }
        if (request.getStateAction() == AdminStateAction.REJECT_EVENT
                && event.getState() == EventState.PUBLISHED) {
            throw new ConflictException("Cannot reject published event");
        }
        if (request.getParticipantLimit() != null && request.getParticipantLimit() < 0) {
            throw new BadRequestException("Participant limit must be positive or zero");
        }

        Long categoryId = request.getCategory();
        if (categoryId != null) {
            getCategory(categoryId);
        }
        EventUpdateUtils.updateFromAdmin(event, request, categoryId);

        if (request.getStateAction() != null) {
            switch (request.getStateAction()) {
                case PUBLISH_EVENT -> {
                    event.setState(EventState.PUBLISHED);
                    event.setPublishedOn(LocalDateTime.now());
                }
                case REJECT_EVENT -> event.setState(EventState.CANCELED);
            }
        }
        return enrichWithViewsAndRequests(repository.save(event));
    }

    @Override
    public List<EventShortDto> getUserEvents(Long userId, int from, int size) {
        checkPagination(from, size);
        getUser(userId);
        Pageable pageable = PageRequest.of(0, from + size);
        List<Event> events = takePage(repository.findByInitiatorId(userId, pageable).getContent(), from, size);
        Map<Long, Long> confirmedCounts = getConfirmedRequestsCounts(events);
        Map<Long, Long> viewsCounts = getViewsCounts(events);
        return enrichShortEvents(events, confirmedCounts, viewsCounts);
    }

    @Override
    @Transactional
    public EventFullDto createEvent(Long userId, NewEventDto dto) {
        if (dto.getDescription() == null || dto.getDescription().isBlank()) {
            throw new ValidationException("description must not be blank");
        }
        if (dto.getAnnotation() == null || dto.getAnnotation().isBlank()) {
            throw new ValidationException("annotation must not be blank");
        }
        if (dto.getParticipantLimit() != null && dto.getParticipantLimit() < 0) {
            throw new BadRequestException("Participant limit cannot be negative");
        }
        getUser(userId);
        getCategory(dto.getCategory());
        if (dto.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new BadRequestException("Event date must be at least 2 hours from now");
        }

        Event event = mapper.toEntity(dto, dto.getCategory(), userId);
        event.setState(EventState.PENDING);
        event.setCreatedOn(LocalDateTime.now());
        event.setPaid(dto.getPaid() != null ? dto.getPaid() : false);
        event.setParticipantLimit(dto.getParticipantLimit() != null ? dto.getParticipantLimit() : 0);
        event.setRequestModeration(dto.getRequestModeration() != null ? dto.getRequestModeration() : true);
        return enrichWithViewsAndRequests(repository.save(event));
    }

    @Override
    public EventFullDto getUserEvent(Long userId, Long eventId) {
        getUser(userId);
        Event event = getEventEntity(eventId);
        if (event.getParticipantLimit() < 0) {
            throw new BadRequestException("Participant limit must not be negative");
        }
        if (!event.getInitiatorId().equals(userId)) {
            throw new NotFoundException("Event with id " + eventId + " not found for user " + userId);
        }
        return enrichWithViewsAndRequests(event);
    }

    @Override
    @Transactional
    public EventFullDto updateUserEvent(Long userId, Long eventId, UpdateEventUserRequest request) {
        getUser(userId);
        Event event = getEventEntity(eventId);
        if (!event.getInitiatorId().equals(userId)) {
            throw new NotFoundException("Event with id " + eventId + " not found for user " + userId);
        }
        if (event.getState() == EventState.PUBLISHED) {
            throw new ConflictException("Only pending or canceled events can be changed");
        }
        if (request.getEventDate() != null
                && request.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new BadRequestException("Event date must be at least 2 hours from now");
        }
        if (request.getParticipantLimit() != null && request.getParticipantLimit() < 0) {
            throw new BadRequestException("Participant Limit must be positive or zero");
        }
        Long categoryId = request.getCategory();
        if (categoryId != null) {
            getCategory(categoryId);
        }
        EventUpdateUtils.updateFromUser(event, request, categoryId);
        if (request.getStateAction() != null) {
            switch (request.getStateAction()) {
                case SEND_TO_REVIEW -> event.setState(EventState.PENDING);
                case CANCEL_REVIEW -> event.setState(EventState.CANCELED);
                default -> throw new BadRequestException("Unknown state action: " + request.getStateAction());
            }
        }
        return enrichWithViewsAndRequests(repository.save(event));
    }

    @Override
    public List<EventShortDto> getPublicEvents(String text, List<Long> categories, Boolean paid,
                                               LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                               Boolean onlyAvailable, String sort, int from, int size) {
        if (rangeStart == null) {
            rangeStart = LocalDateTime.now();
        }
        if (rangeEnd == null) {
            rangeEnd = LocalDateTime.now().plusYears(100);
        }
        if (rangeStart.isAfter(rangeEnd)) {
            throw new BadRequestException("Range end must not be before range start");
        }
        if (text == null) {
            text = "";
        }

        checkPagination(from, size);
        boolean sortByViews = SortType.VIEWS.name().equalsIgnoreCase(sort);
        boolean filterAvailable = Boolean.TRUE.equals(onlyAvailable);
        boolean loadAll = sortByViews || filterAvailable;
        Pageable pageable = loadAll ? Pageable.unpaged() : PageRequest.of(0, from + size);

        List<Event> events = new ArrayList<>(eventRepositoryCustom.findPublishedEvents(
                text, categories, paid, rangeStart, rangeEnd, pageable).getContent());

        // Separate databases can retain events whose category/user disappeared after a reset.
        // Never invent related objects, delete events, or return 404 for the whole public page.
        Map<Long, CategoryDto> knownCategories = loadCategories(events);
        Map<Long, UserDto> knownUsers = loadUsers(events);
        boolean missingRelations = false;
        for (Event event : events) {
            if (!knownCategories.containsKey(event.getCategoryId())
                    || !knownUsers.containsKey(event.getInitiatorId())) {
                missingRelations = true;
                break;
            }
        }
        if (missingRelations) {
            // Fill pagination from all valid events, not merely from the first page.
            if (!loadAll) {
                events = new ArrayList<>(eventRepositoryCustom.findPublishedEvents(
                        text, categories, paid, rangeStart, rangeEnd, Pageable.unpaged()).getContent());
                knownCategories = loadCategories(events);
                knownUsers = loadUsers(events);
            }
            Map<Long, CategoryDto> resolvedCategories = knownCategories;
            Map<Long, UserDto> resolvedUsers = knownUsers;
            List<Long> excludedIds = events.stream()
                    .filter(event -> !resolvedCategories.containsKey(event.getCategoryId())
                            || !resolvedUsers.containsKey(event.getInitiatorId()))
                    .map(Event::getId).toList();
            if (!excludedIds.isEmpty()) {
                log.warn("Skipping public events with missing category/user references: {}", excludedIds);
                events.removeIf(event -> !resolvedCategories.containsKey(event.getCategoryId())
                        || !resolvedUsers.containsKey(event.getInitiatorId()));
            }
            loadAll = true;
        }
        if (!loadAll) {
            events = takePage(events, from, size);
        }
        Map<Long, Long> confirmedCounts = getConfirmedRequestsCounts(events);
        if (filterAvailable) {
            events.removeIf(event -> event.getParticipantLimit() > 0
                    && confirmedCounts.getOrDefault(event.getId(), 0L) >= event.getParticipantLimit());
        }
        Map<Long, Long> viewsCounts = getViewsCounts(events);
        if (sortByViews) {
            events.sort(Comparator.comparingLong((Event event) -> viewsCounts.getOrDefault(event.getId(), 0L))
                    .reversed().thenComparing(Event::getId));
        }
        if (loadAll) {
            events = takePage(events, from, size);
        }
        return enrichShortEvents(events, confirmedCounts, viewsCounts, knownCategories, knownUsers);
    }

    @Override
    public EventFullDto getPublicEvent(Long eventId) {
        Event event = getEventEntity(eventId);
        if (event.getState() != EventState.PUBLISHED) {
            throw new NotFoundException("Event with id " + eventId + " not found");
        }
        return enrichWithViewsAndRequests(event);
    }

    @Override
    public EventInternalDto getInternalEvent(Long eventId) {
        Event event = getEventEntity(eventId);
        return new EventInternalDto(event.getId(), event.getInitiatorId(), event.getCategoryId(), event.getState(),
                event.getParticipantLimit(), event.getRequestModeration(), event.getEventDate());
    }

    @Override
    public List<EventShortDto> getInternalEventShorts(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return List.of();
        }
        List<Event> events = new ArrayList<>(repository.findAllById(eventIds));
        events.sort(Comparator.comparing(Event::getId));
        Map<Long, Long> confirmedCounts = getConfirmedRequestsCounts(events);
        Map<Long, Long> viewsCounts = getViewsCounts(events);
        return enrichShortEvents(events, confirmedCounts, viewsCounts);
    }

    @Override
    public boolean existsByCategoryId(Long categoryId) {
        return repository.existsByCategoryId(categoryId);
    }

    @Override
    public boolean existsByInitiatorId(Long initiatorId) {
        return repository.existsByInitiatorId(initiatorId);
    }

    private void checkPagination(int from, int size) {
        if (from < 0 || size <= 0 || from > Integer.MAX_VALUE - size) {
            throw new BadRequestException("Invalid pagination parameters");
        }
    }

    private List<Event> takePage(List<Event> events, int from, int size) {
        if (from >= events.size()) {
            return List.of();
        }
        return new ArrayList<>(events.subList(from, Math.min(events.size(), from + size)));
    }

    private Event getEventEntity(Long eventId) {
        return repository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id " + eventId + " was not found"));
    }

    private UserDto getUser(Long userId) {
        UserDto user = userServiceClient.getUser(userId);
        if (user == null || user.getId() == null) {
            throw new NotFoundException("User with id " + userId + " was not found");
        }
        return user;
    }

    private CategoryDto getCategory(Long categoryId) {
        CategoryDto category = categoryServiceClient.getCategory(categoryId);
        if (category == null || category.getId() == null) {
            throw new NotFoundException("Category with id " + categoryId + " was not found");
        }
        return category;
    }

    private EventFullDto enrichWithViewsAndRequests(Event event) {
        Map<Long, Long> confirmedCounts = getConfirmedRequestsCounts(List.of(event));
        Map<Long, Long> viewsCounts = getViewsCounts(List.of(event));
        return enrichFullEvents(List.of(event), confirmedCounts, viewsCounts).get(0);
    }

    private List<EventFullDto> enrichFullEvents(List<Event> events, Map<Long, Long> confirmedCounts,
                                                 Map<Long, Long> viewsCounts) {
        Map<Long, CategoryDto> categories = loadCategories(events);
        Map<Long, UserDto> users = loadUsers(events);
        return events.stream().map(event -> {
            EventFullDto dto = mapper.toFullDto(event);
            dto.setCategory(requireCategory(categories, event.getCategoryId()));
            dto.setInitiator(toShortUser(requireUser(users, event.getInitiatorId())));
            dto.setConfirmedRequests(confirmedCounts.getOrDefault(event.getId(), 0L));
            dto.setViews(viewsCounts.getOrDefault(event.getId(), 0L));
            return dto;
        }).toList();
    }

    private List<EventShortDto> enrichShortEvents(List<Event> events, Map<Long, Long> confirmedCounts,
                                                   Map<Long, Long> viewsCounts) {
        return enrichShortEvents(events, confirmedCounts, viewsCounts,
                loadCategories(events), loadUsers(events));
    }

    private List<EventShortDto> enrichShortEvents(List<Event> events, Map<Long, Long> confirmedCounts,
                                                   Map<Long, Long> viewsCounts,
                                                   Map<Long, CategoryDto> categories, Map<Long, UserDto> users) {
        return events.stream().map(event -> {
            EventShortDto dto = mapper.toShortDto(event);
            dto.setCategory(requireCategory(categories, event.getCategoryId()));
            dto.setInitiator(toShortUser(requireUser(users, event.getInitiatorId())));
            dto.setConfirmedRequests(confirmedCounts.getOrDefault(event.getId(), 0L));
            dto.setViews(viewsCounts.getOrDefault(event.getId(), 0L));
            return dto;
        }).toList();
    }

    private Map<Long, CategoryDto> loadCategories(List<Event> events) {
        List<Long> ids = events.stream().map(Event::getCategoryId).filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        List<CategoryDto> result = categoryServiceClient.getCategories(ids);
        return result.stream().collect(Collectors.toMap(CategoryDto::getId, Function.identity()));
    }

    private Map<Long, UserDto> loadUsers(List<Event> events) {
        List<Long> ids = events.stream().map(Event::getInitiatorId).filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        List<UserDto> result = userServiceClient.getUsers(ids);
        return result.stream().collect(Collectors.toMap(UserDto::getId, Function.identity()));
    }

    private CategoryDto requireCategory(Map<Long, CategoryDto> categories, Long id) {
        CategoryDto category = categories.get(id);
        if (category == null) {
            throw new NotFoundException("Category with id " + id + " was not found");
        }
        return category;
    }

    private UserDto requireUser(Map<Long, UserDto> users, Long id) {
        UserDto user = users.get(id);
        if (user == null) {
            throw new NotFoundException("User with id " + id + " was not found");
        }
        return user;
    }

    private UserShortDto toShortUser(UserDto user) {
        return new UserShortDto(user.getId(), user.getName());
    }

    private Map<Long, Long> getConfirmedRequestsCounts(List<Event> events) {
        if (events == null || events.isEmpty()) {
            return Map.of();
        }
        List<Long> eventIds = events.stream().map(Event::getId).toList();
        Map<Long, Long> counts = requestServiceClient.getConfirmedCounts(eventIds);
        return counts == null ? Map.of() : counts;
    }

    private Map<Long, Long> getViewsCounts(List<Event> events) {
        if (events == null || events.isEmpty()) {
            return Map.of();
        }
        List<String> uris = events.stream().map(event -> "/events/" + event.getId()).toList();
        LocalDateTime start = events.stream()
                .map(event -> event.getPublishedOn() != null ? event.getPublishedOn() : event.getCreatedOn())
                .filter(Objects::nonNull)
                .min(LocalDateTime::compareTo)
                .orElseGet(() -> LocalDateTime.now().minusDays(1));
        List<ViewStats> stats;
        try {
            stats = statsClient.getStats(start.format(FORMATTER),
                    LocalDateTime.now().format(FORMATTER), uris, true);
        } catch (StatsServerUnavailable ignored) {
            return Map.of();
        }
        if (stats == null || stats.isEmpty()) {
            return Map.of();
        }
        Map<String, Long> viewsByUri = stats.stream().collect(Collectors.toMap(
                ViewStats::getUri, ViewStats::getHits, (first, ignored) -> first));
        return events.stream().collect(Collectors.toMap(Event::getId,
                event -> viewsByUri.getOrDefault("/events/" + event.getId(), 0L)));
    }
}
