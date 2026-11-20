package ru.practicum.statistics.client.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import ru.practicum.dto.EndpointHit;
import ru.practicum.dto.ViewStats;
import ru.practicum.statistics.client.controller.api.StatsClientApi;
import ru.practicum.statistics.client.exception.StatsServerUnavailable;

import java.net.URI;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@RequiredArgsConstructor
public class StatsClient implements StatsClientApi {
    private final RestTemplate restTemplate;
    private final DiscoveryClient discoveryClient;
    private final RetryTemplate statsDiscoveryRetryTemplate;
    private final AtomicInteger nextInstance = new AtomicInteger();

    @Value("${stats.server.service-id:stats-server}")
    private String statsServiceId;

    private URI makeUri(String path) {
        List<ServiceInstance> instances = discoveryClient.getInstances(statsServiceId);
        if (instances == null || instances.isEmpty()) {
            throw new StatsServerUnavailable("No registered instances for service " + statsServiceId);
        }
        int index = Math.floorMod(nextInstance.getAndIncrement(), instances.size());
        ServiceInstance instance = instances.get(index);
        return UriComponentsBuilder.fromUri(instance.getUri()).path(path).build().toUri();
    }

    @Override
    public void sendHit(EndpointHit hit) {
        try {
            // Retry only Eureka discovery: POST /hit is not idempotent and must be sent once.
            URI uri = statsDiscoveryRetryTemplate.execute(context -> makeUri("/hit"));
            restTemplate.postForEntity(uri, hit, Void.class);
            log.info("Statistics hit sent: {}", hit);
        } catch (Exception exception) {
            log.error("Could not send statistics hit", exception);
        }
    }

    @Override
    public List<ViewStats> getStats(String start, String end, List<String> uris, Boolean unique) {
        try {
            return statsDiscoveryRetryTemplate.execute(context -> {
                URI uri = UriComponentsBuilder.fromUri(makeUri("/stats"))
                        .queryParam("start", start)
                        .queryParam("end", end)
                        .queryParamIfPresent("uris", uris == null || uris.isEmpty()
                                ? java.util.Optional.empty()
                                : java.util.Optional.of(String.join(",", uris)))
                        .queryParamIfPresent("unique", java.util.Optional.ofNullable(unique))
                        .build().encode().toUri();

                ResponseEntity<List<ViewStats>> response = restTemplate.exchange(
                        uri, HttpMethod.GET, null, new ParameterizedTypeReference<>() {
                        });
                return response.getBody() == null ? List.of() : response.getBody();
            });
        } catch (Exception exception) {
            log.error("Could not fetch statistics", exception);
            throw new StatsServerUnavailable(
                    "Statistics service is temporarily unavailable",
                    exception
            );
        }
    }
}
