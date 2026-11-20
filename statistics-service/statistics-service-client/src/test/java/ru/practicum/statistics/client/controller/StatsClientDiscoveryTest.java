package ru.practicum.statistics.client.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import ru.practicum.dto.EndpointHit;
import ru.practicum.dto.ViewStats;
import ru.practicum.statistics.client.exception.StatsServerUnavailable;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

class StatsClientDiscoveryTest {

    private final RestTemplate restTemplate = mock(RestTemplate.class);
    private final DiscoveryClient discoveryClient = mock(DiscoveryClient.class);
    private final ServiceInstance serviceInstance = mock(ServiceInstance.class);
    private StatsClient statsClient;

    @BeforeEach
    void setUp() {
        RetryTemplate retry = new RetryTemplate();
        retry.setRetryPolicy(new SimpleRetryPolicy(1));
        statsClient = new StatsClient(restTemplate, discoveryClient, retry);
        ReflectionTestUtils.setField(statsClient, "statsServiceId", "stats-server");
    }

    @Test
    void sendsHitToPortDiscoveredViaEureka() {
        when(discoveryClient.getInstances("stats-server")).thenReturn(List.of(serviceInstance));
        when(serviceInstance.getUri()).thenReturn(URI.create("http://127.0.0.1:51423"));
        EndpointHit hit = new EndpointHit("ewm", "/events/1", "127.0.0.1", LocalDateTime.now());

        statsClient.sendHit(hit);

        verify(discoveryClient).getInstances("stats-server");
        verify(restTemplate).postForEntity(URI.create("http://127.0.0.1:51423/hit"), hit, Void.class);
    }

    @Test
    void retriesMissingEurekaRegistrationButPostsHitOnlyOnce() {
        RetryTemplate retry = new RetryTemplate();
        retry.setRetryPolicy(new SimpleRetryPolicy(3));
        statsClient = new StatsClient(restTemplate, discoveryClient, retry);
        ReflectionTestUtils.setField(statsClient, "statsServiceId", "stats-server");
        when(discoveryClient.getInstances("stats-server"))
                .thenReturn(List.of())
                .thenReturn(List.of(serviceInstance));
        when(serviceInstance.getUri()).thenReturn(URI.create("http://127.0.0.1:51423"));
        EndpointHit hit = new EndpointHit("ewm", "/events/1", "127.0.0.1", LocalDateTime.now());

        statsClient.sendHit(hit);

        verify(discoveryClient, times(2)).getInstances("stats-server");
        verify(restTemplate, times(1)).postForEntity(
                URI.create("http://127.0.0.1:51423/hit"), hit, Void.class);
    }

    @Test
    void doesNotRetryPostingHitAfterAnUncertainHttpFailure() {
        when(discoveryClient.getInstances("stats-server")).thenReturn(List.of(serviceInstance));
        when(serviceInstance.getUri()).thenReturn(URI.create("http://127.0.0.1:51423"));
        EndpointHit hit = new EndpointHit("ewm", "/events/1", "127.0.0.1", LocalDateTime.now());
        when(restTemplate.postForEntity(
                URI.create("http://127.0.0.1:51423/hit"), hit, Void.class))
                .thenThrow(new ResourceAccessException("connection reset"));

        statsClient.sendHit(hit);

        verify(restTemplate, times(1)).postForEntity(
                URI.create("http://127.0.0.1:51423/hit"), hit, Void.class);
    }

    @Test
    void reportsUnavailableStatisticsWhenNoInstancesAreRegistered() {
        when(discoveryClient.getInstances("stats-server")).thenReturn(List.of());

        assertThrows(StatsServerUnavailable.class,
                () -> statsClient.getStats("2026-01-01 00:00:00", "2026-01-02 00:00:00",
                        List.of("/events/1"), true));
    }

    @Test
    void reportsUnavailableStatisticsWhenHttpCallFails() {
        when(discoveryClient.getInstances("stats-server")).thenReturn(List.of(serviceInstance));
        when(serviceInstance.getUri()).thenReturn(URI.create("http://127.0.0.1:51423"));
        when(restTemplate.exchange(
                any(URI.class),
                eq(HttpMethod.GET),
                isNull(),
                org.mockito.ArgumentMatchers.<ParameterizedTypeReference<List<ViewStats>>>any()
        )).thenThrow(new ResourceAccessException("connection refused"));

        assertThrows(StatsServerUnavailable.class,
                () -> statsClient.getStats("2026-01-01 00:00:00", "2026-01-02 00:00:00",
                        List.of("/events/1"), true));
    }

    @Test
    void distributesHitsAcrossRegisteredInstances() {
        ServiceInstance second = mock(ServiceInstance.class);
        when(discoveryClient.getInstances("stats-server")).thenReturn(List.of(serviceInstance, second));
        when(serviceInstance.getUri()).thenReturn(URI.create("http://127.0.0.1:51423"));
        when(second.getUri()).thenReturn(URI.create("http://127.0.0.1:51424"));
        EndpointHit hit = new EndpointHit("ewm", "/events/1", "127.0.0.1", LocalDateTime.now());

        statsClient.sendHit(hit);
        statsClient.sendHit(hit);

        verify(restTemplate).postForEntity(URI.create("http://127.0.0.1:51423/hit"), hit, Void.class);
        verify(restTemplate).postForEntity(URI.create("http://127.0.0.1:51424/hit"), hit, Void.class);
    }

    @Test
    void retriesGetStatsAfterTemporaryHttpFailure() {
        RetryTemplate retry = new RetryTemplate();
        retry.setRetryPolicy(new SimpleRetryPolicy(3));
        statsClient = new StatsClient(restTemplate, discoveryClient, retry);
        ReflectionTestUtils.setField(statsClient, "statsServiceId", "stats-server");
        when(discoveryClient.getInstances("stats-server")).thenReturn(List.of(serviceInstance));
        when(serviceInstance.getUri()).thenReturn(URI.create("http://127.0.0.1:51423"));
        when(restTemplate.exchange(
                any(URI.class),
                eq(HttpMethod.GET),
                isNull(),
                org.mockito.ArgumentMatchers.<ParameterizedTypeReference<List<ViewStats>>>any()
        )).thenThrow(new ResourceAccessException("connection refused"))
                .thenReturn(ResponseEntity.ok(List.of()));

        statsClient.getStats("2026-01-01 00:00:00", "2026-01-02 00:00:00",
                List.of("/events/1"), true);

        verify(restTemplate, times(2)).exchange(
                any(URI.class),
                eq(HttpMethod.GET),
                isNull(),
                org.mockito.ArgumentMatchers.<ParameterizedTypeReference<List<ViewStats>>>any()
        );
    }
}
