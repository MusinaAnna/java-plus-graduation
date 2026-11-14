package ru.practicum.statistics.client.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import ru.practicum.dto.EndpointHit;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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
    void returnsEmptyStatisticsWhenNoInstancesAreRegistered() {
        when(discoveryClient.getInstances("stats-server")).thenReturn(List.of());

        assertTrue(statsClient.getStats("2026-01-01 00:00:00", "2026-01-02 00:00:00",
                List.of("/events/1"), true).isEmpty());
    }
}
