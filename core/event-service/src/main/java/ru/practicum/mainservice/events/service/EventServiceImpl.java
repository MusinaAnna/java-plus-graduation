package ru.practicum.mainservice.events.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
import ru.practicum.common.exception.ServiceUnavailableException;
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
import ru.practicum.statistics.client.grpc.RecommendationsGrpcClient;
import ru.practicum.statistics.client.grpc.UserActionGrpcClient;
import ru.practicum.ewm.stats.grpc.dashboard.RecommendedEventProto;

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

    @Autowired
    private RecommendationsGrpcClient recommendationsClient;

    @Autowired
    private UserActionGrpcClient userActionClient;

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
            events = findConfirmedAdminPage(query, users, categories);
        } else {
            events = eventRepositoryCustom.findAllByAdminFilters(users, query.states(), categories,
                    query.rangeStart(), query.rangeEnd(), offsetPage(query.from(), query.size())).getContent();
        }
        Map<Long, Long> confirmedCounts = getConfirmedRequestsCounts(events);
        return enrichFullEvents(events, confirmedCounts);
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
        return enrichWithRatingsAndRequests(repository.save(event));
    }

    @Override
    public List<EventShortDto> getUserEvents(Long userId, int from, int size) {
        checkPagination(from, size);
        getUser(userId);
        List<Event> events = repository.findByInitiatorId(userId, offsetPage(from, size)).getContent();
        Map<Long, Long> confirmedCounts = getConfirmedRequestsCounts(events);
        return enrichShortEvents(events, confirmedCounts);
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
        return enrichWithRatingsAndRequests(repository.save(event));
    }

    @Override
    public EventFullDto getUserEvent(Long userId, Long eventId) {
        getUser(userId);
        Event event = getEventEntity(eventId);
        if (!event.getInitiatorId().equals(userId)) {
            throw new NotFoundException("Event with id " + eventId + " not found for user " + userId);
        }
        return enrichWithRatingsAndRequests(event);
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
        return enrichWithRatingsAndRequests(repository.save(event));
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
        boolean sortByRating = SortType.RATING.name().equalsIgnoreCase(query.sort());
        boolean filterAvailable = Boolean.TRUE.equals(query.onlyAvailable());
        boolean allCandidates = sortByViews || sortByRating || filterAvailable;
        ResolvedEvents resolved = findPublicCandidates(query, rangeStart, rangeEnd, allCandidates);
        List<Event> events = new ArrayList<>(resolved.events());
        Map<Long, Long> confirmedCounts = getConfirmedRequestsCounts(events);
        if (filterAvailable) {
            events.removeIf(event -> event.getParticipantLimit() > 0
                    && confirmedCounts.getOrDefault(event.getId(), 0L) >= event.getParticipantLimit());
        }
        Map<Long, Long> viewsCounts = sortByViews ? getViewsCounts(events) : Map.of();
        Map<Long, Double> ratings = getRatingScores(events);
        if (sortByViews) {
            events.sort(Comparator.comparingLong((Event event) -> viewsCounts.getOrDefault(event.getId(), 0L))
                    .reversed().thenComparing(Event::getId));
        } else if (sortByRating) {
            events.sort(Comparator.comparingDouble((Event event) -> ratings.getOrDefault(event.getId(), 0.0))
                    .reversed().thenComparing(Event::getId));
        }
        if (allCandidates) {
            events = takePage(events, query.from(), query.size());
        }
        return enrichShortEvents(events, confirmedCounts,
                resolved.categories(), resolved.users(), ratings);
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
        boolean checkedPartialTail = false;
        while (true) {
            List<Event> batch = eventRepositoryCustom.findPublishedEvents(
                    query.text() == null ? "" : query.text(), query.categories(), query.paid(),
                    rangeStart, rangeEnd, offsetPage(offset, batchSize)).getContent();
            if (batch.isEmpty()) {
                break;
            }
            List<Event> uncachedCategories = batch.stream()
                    .filter(event -> !categories.containsKey(event.getCategoryId()))
                    .toList();
            List<Event> uncachedUsers = batch.stream()
                    .filter(event -> !users.containsKey(event.getInitiatorId()))
                    .toList();
            categories.putAll(loadCategories(uncachedCategories));
            users.putAll(loadUsers(uncachedUsers));
            Map<Long, CategoryDto> pageCategories = categories;
            Map<Long, UserDto> pageUsers = users;
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
        return enrichWithRatingsAndRequests(event);
    }

    @Override
    public List<EventShortDto> getRecommendedEvents(Long userId, int size) {
        if (userId == null || userId <= 0 || size <= 0 || size > 100) {
            throw new BadRequestException("Invalid recommendations request");
        }
        if (recommendationsClient == null) {
            throw new ServiceUnavailableException("Recommendations client is not configured");
        }

        final int maxFetch = 1000;
        int fetchLimit = size;
        List<Event> eligible = List.of();
        while (true) {
            List<RecommendedEventProto> candidates =
                    recommendationsClient.getRecommendationsForUser(userId, fetchLimit);
            if (candidates.isEmpty()) {
                break;
            }
            List<Long> ids = candidates.stream().map(RecommendedEventProto::getEventId)
                    .distinct().toList();
            Map<Long, Event> byId = new HashMap<>();
            repository.findAllById(ids).forEach(event -> byId.put(event.getId(), event));
            LocalDateTime now = LocalDateTime.now();
            eligible = ids.stream().map(byId::get).filter(Objects::nonNull)
                    .filter(event -> event.getState() == EventState.PUBLISHED)
                    .filter(event -> event.getEventDate() != null && event.getEventDate().isAfter(now))
                    .limit(size).toList();

            if (eligible.size() == size || candidates.size() < fetchLimit || fetchLimit >= maxFetch) {
                break;
            }
            fetchLimit = Math.min(maxFetch, fetchLimit * 2);
        }
        if (eligible.isEmpty()) {
            return List.of();
        }
        return enrichShortEvents(eligible, getConfirmedRequestsCounts(eligible));
    }

    @Override
    public void likeEvent(Long userId, Long eventId) {
        if (userId == null || userId <= 0 || eventId == null || eventId <= 0) {
            throw new BadRequestException("User and event IDs must be positive");
        }
        Event event = getEventEntity(eventId);
        if (event.getState() != EventState.PUBLISHED) {
            throw new NotFoundException("Event with id " + eventId + " not found");
        }
        if (event.getEventDate() == null || !event.getEventDate().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Likes are allowed only after the event date");
        }
        getUser(userId);
        if (!requestServiceClient.isConfirmedParticipant(eventId, userId)) {
            throw new BadRequestException("Only confirmed participants may like an event");
        }
        if (userActionClient == null) {
            throw new ServiceUnavailableException("Collector client is not configured");
        }
        userActionClient.sendLike(userId, eventId);
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
        return enrichShortEvents(events, confirmedCounts);
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

    private EventFullDto enrichWithRatingsAndRequests(Event event) {
        Map<Long, Long> confirmedCounts = getConfirmedRequestsCounts(List.of(event));
        return enrichFullEvents(List.of(event), confirmedCounts).get(0);
    }

    private List<EventFullDto> enrichFullEvents(List<Event> events, Map<Long, Long> confirmedCounts) {
        Map<Long, Double> ratings = getRatingScores(events);
        Map<Long, CategoryDto> categories = loadCategories(events);
        Map<Long, UserDto> users = loadUsers(events);
        return events.stream().map(event -> {
            EventFullDto dto = mapper.toFullDto(event);
            dto.setCategory(requireCategory(categories, event.getCategoryId()));
            dto.setInitiator(toShortUser(requireUser(users, event.getInitiatorId())));
            dto.setConfirmedRequests(confirmedCounts.getOrDefault(event.getId(), 0L));
            dto.setRating(ratings.getOrDefault(event.getId(), 0.0));
            return dto;
        }).toList();
    }

    private List<EventShortDto> enrichShortEvents(List<Event> events, Map<Long, Long> confirmedCounts) {
        return enrichShortEvents(events, confirmedCounts,
                loadCategories(events), loadUsers(events), getRatingScores(events));
    }

    private List<EventShortDto> enrichShortEvents(List<Event> events, Map<Long, Long> confirmedCounts,
                                                   Map<Long, CategoryDto> categories, Map<Long, UserDto> users,
                                                   Map<Long, Double> ratings) {
        return events.stream().map(event -> {
            EventShortDto dto = mapper.toShortDto(event);
            dto.setCategory(requireCategory(categories, event.getCategoryId()));
            dto.setInitiator(toShortUser(requireUser(users, event.getInitiatorId())));
            dto.setConfirmedRequests(confirmedCounts.getOrDefault(event.getId(), 0L));
            dto.setRating(ratings.getOrDefault(event.getId(), 0.0));
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

    private Map<Long, Double> getRatingScores(List<Event> events) {
        if (events == null || events.isEmpty() || recommendationsClient == null) {
            return Map.of();
        }
        Map<Long, Double> result = new HashMap<>();
        for (int from = 0; from < events.size(); from += 100) {
            List<Long> ids = events.subList(from, Math.min(from + 100, events.size()))
                    .stream().map(Event::getId).toList();
            try {
                List<RecommendedEventProto> scores = recommendationsClient.getInteractionsCount(ids);
                for (RecommendedEventProto score : scores) {
                    result.put(score.getEventId(), score.getScore());
                }
            } catch (RuntimeException exception) {
                throw new ServiceUnavailableException("Could not fetch event ratings from Analyzer", exception);
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
