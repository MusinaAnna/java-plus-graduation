package ru.practicum.mainservice.events.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.common.dto.CategoryDto;
import ru.practicum.common.dto.EventInternalDto;
import ru.practicum.common.dto.EventShortDto;
import ru.practicum.common.dto.UserDto;
import ru.practicum.common.dto.UserShortDto;
import ru.practicum.common.enums.EventState;
import ru.practicum.common.enums.SortType;
import org.springframework.data.domain.Sort;
import ru.practicum.common.pagination.OffsetPageRequest;
import ru.practicum.common.exception.BadRequestException;
import ru.practicum.common.exception.ConflictException;
import ru.practicum.common.exception.NotFoundException;
import ru.practicum.dto.ViewStats;
import ru.practicum.mainservice.clients.CategoryServiceClient;
import ru.practicum.mainservice.clients.RequestServiceClient;
import ru.practicum.mainservice.clients.UserServiceClient;
import ru.practicum.mainservice.events.dto.AdminEventQuery;
import ru.practicum.mainservice.events.dto.PublicEventQuery;
import ru.practicum.mainservice.events.dto.EventFullDto;
import ru.practicum.mainservice.events.dto.NewEventDto;
import ru.practicum.mainservice.events.dto.UpdateEventAdminRequest;
import ru.practicum.mainservice.events.dto.UpdateEventUserRequest;
import ru.practicum.mainservice.events.entity.Event;
import ru.practicum.mainservice.events.mapper.EventMapper;
import ru.practicum.mainservice.events.repository.EventRepository;
import ru.practicum.mainservice.events.repository.EventRepositoryCustom;
import ru.practicum.mainservice.events.utils.EventUpdateUtils;
import ru.practicum.mainservice.events.utils.EventValidation;
import ru.practicum.statistics.client.controller.StatsClient;
import ru.practicum.statistics.client.exception.StatsServerUnavailable;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
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
    public List<EventFullDto> getAdminEvents(AdminEventQuery query) {
        checkPagination(query.from(), query.size());
        boolean confirmedOnly = query.users() != null && query.users().contains(0L);
        List<Long> users = confirmedOnly ? null : query.users();
        List<Long> categories = query.categories() != null && query.categories().contains(0L)
                ? null : query.categories();

        List<Event> events;
        if (confirmedOnly) {
            // A remote confirmed-count filter must be applied before logical pagination.
            events = findConfirmedAdminPage(query, users, categories);
        } else {
            events = eventRepositoryCustom.findAllByAdminFilters(users, query.states(), categories,
                    query.rangeStart(), query.rangeEnd(), offsetPage(query.from(), query.size())).getContent();
        }
        Map<Long, Long> confirmedCounts = getConfirmedRequestsCounts(events);
        Map<Long, Long> viewsCounts = getViewsCounts(events);
        return enrichFullEvents(events, confirmedCounts, viewsCounts);
    }

    private List<Event> findConfirmedAdminPage(AdminEventQuery query, List<Long> users, List<Long> categories) {
        List<Event> selected = new ArrayList<>();
        long offset = 0;
        int matched = 0;
        final int batchSize = 100;
        while (selected.size() < query.size()) {
            List<Event> batch = eventRepositoryCustom.findAllByAdminFilters(users, query.states(), categories,
                    query.rangeStart(), query.rangeEnd(), offsetPage(offset, batchSize)).getContent();
            if (batch.isEmpty()) {
                break;
            }
            Map<Long, Long> counts = getConfirmedRequestsCounts(batch);
            for (Event event : batch) {
                if (counts.getOrDefault(event.getId(), 0L) > 0) {
                    if (matched++ >= query.from()) {
                        selected.add(event);
                        if (selected.size() == query.size()) {
                            break;
                        }
                    }
                }
            }
            offset += batch.size();
            if (batch.size() < batchSize) {
                break;
            }
        }
        return selected;
    }

    @Override
    @Transactional
    public EventFullDto updateAdminEvent(Long eventId, UpdateEventAdminRequest request) {
        Event event = getEventEntity(eventId);
        EventValidation.onAdminUpdate(event, request);

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
        List<Event> events = repository.findByInitiatorId(userId, offsetPage(from, size)).getContent();
        Map<Long, Long> confirmedCounts = getConfirmedRequestsCounts(events);
        Map<Long, Long> viewsCounts = getViewsCounts(events);
        return enrichShortEvents(events, confirmedCounts, viewsCounts);
    }

    @Override
    @Transactional
    public EventFullDto createEvent(Long userId, NewEventDto dto) {
        EventValidation.onCreate(dto);
        getUser(userId);
        getCategory(dto.getCategory());

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
        EventValidation.onUserUpdate(event, request);
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
    public List<EventShortDto> getPublicEvents(PublicEventQuery query) {
        checkPagination(query.from(), query.size());
        LocalDateTime rangeStart = query.rangeStart() == null ? LocalDateTime.now() : query.rangeStart();
        LocalDateTime rangeEnd = query.rangeEnd() == null
                ? LocalDateTime.now().plusYears(100) : query.rangeEnd();
        if (rangeStart.isAfter(rangeEnd)) {
            throw new BadRequestException("Range end must not be before range start");
        }

        boolean sortByViews = SortType.VIEWS.name().equalsIgnoreCase(query.sort());
        boolean filterAvailable = Boolean.TRUE.equals(query.onlyAvailable());
        // Global sorting/filtering requires considering candidates before pagination.
        boolean allCandidates = sortByViews || filterAvailable;
        ResolvedEvents resolved = findPublicCandidates(query, rangeStart, rangeEnd, allCandidates);
        List<Event> events = new ArrayList<>(resolved.events());
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
        if (allCandidates) {
            events = takePage(events, query.from(), query.size());
        }
        return enrichShortEvents(events, confirmedCounts, viewsCounts, resolved.categories(), resolved.users());
    }

    private ResolvedEvents findPublicCandidates(PublicEventQuery query, LocalDateTime rangeStart,
                                                LocalDateTime rangeEnd, boolean allCandidates) {
        List<Event> selected = new ArrayList<>();
        Map<Long, CategoryDto> categories = new HashMap<>();
        Map<Long, UserDto> users = new HashMap<>();
        long offset = 0;
        int validSeen = 0;
        int excluded = 0;
        int batchSize = Math.max(100, Math.min(200, query.size()));
        // A second query is allowed for a partial page with missing cross-service references.
        // In production the next query will simply return an empty page if the DB is exhausted.
        boolean checkedPartialTail = false;
        while (true) {
            List<Event> batch = eventRepositoryCustom.findPublishedEvents(
                    query.text() == null ? "" : query.text(), query.categories(), query.paid(),
                    rangeStart, rangeEnd, offsetPage(offset, batchSize)).getContent();
            if (batch.isEmpty()) {
                break;
            }
            Map<Long, CategoryDto> pageCategories = loadCategories(batch);
            Map<Long, UserDto> pageUsers = loadUsers(batch);
            categories.putAll(pageCategories);
            users.putAll(pageUsers);
            boolean missingInPage = false;
            for (Event event : batch) {
                if (!pageCategories.containsKey(event.getCategoryId())
                        || !pageUsers.containsKey(event.getInitiatorId())) {
                    excluded++;
                    missingInPage = true;
                    continue;
                }
                if (allCandidates || validSeen >= query.from()) {
                    selected.add(event);
                }
                validSeen++;
                if (!allCandidates && selected.size() >= query.size()) {
                    break;
                }
            }
            if (!allCandidates && selected.size() >= query.size()) {
                break;
            }
            offset += batch.size();
            if (batch.size() < batchSize) {
                if (!missingInPage || checkedPartialTail) {
                    break;
                }
                checkedPartialTail = true;
            }
        }
        if (excluded > 0) {
            log.warn("Skipped {} events with missing category/user references", excluded);
        }
        return new ResolvedEvents(selected, categories, users);
    }

    private record ResolvedEvents(List<Event> events, Map<Long, CategoryDto> categories,
                                  Map<Long, UserDto> users) {
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

    private OffsetPageRequest offsetPage(long from, int size) {
        return new OffsetPageRequest(from, size, Sort.unsorted());
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
        Map<Long, Long> result = new HashMap<>();
        for (int from = 0; from < events.size(); from += 100) {
            List<Long> eventIds = events.subList(from, Math.min(from + 100, events.size()))
                    .stream().map(Event::getId).toList();
            Map<Long, Long> counts = requestServiceClient.getConfirmedCounts(eventIds);
            if (counts != null) {
                result.putAll(counts);
            }
        }
        return result;
    }

    private Map<Long, Long> getViewsCounts(List<Event> events) {
        if (events == null || events.isEmpty()) {
            return Map.of();
        }
        Map<Long, Long> counts = new HashMap<>();
        for (int from = 0; from < events.size(); from += 100) {
            List<Event> batch = events.subList(from, Math.min(from + 100, events.size()));
            List<String> uris = batch.stream().map(event -> "/events/" + event.getId()).toList();
            LocalDateTime start = batch.stream()
                    .map(event -> event.getPublishedOn() != null ? event.getPublishedOn() : event.getCreatedOn())
                    .filter(Objects::nonNull)
                    .min(LocalDateTime::compareTo)
                    .orElseGet(() -> LocalDateTime.now().minusDays(1));
            List<ViewStats> stats;
            try {
                stats = statsClient.getStats(start.format(FORMATTER),
                        LocalDateTime.now().format(FORMATTER), uris, true);
            } catch (StatsServerUnavailable ignored) {
                continue;
            }
            if (stats != null) {
                Map<String, Long> batchViews = stats.stream().collect(Collectors.toMap(
                        ViewStats::getUri, ViewStats::getHits, (first, ignored) -> first));
                for (Event event : batch) {
                    counts.put(event.getId(), batchViews.getOrDefault("/events/" + event.getId(), 0L));
                }
            }
        }
        return counts;
    }

}
