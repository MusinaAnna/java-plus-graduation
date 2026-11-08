package ru.practicum.statistics.client.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import ru.practicum.dto.EndpointHit;
import ru.practicum.dto.ViewStats;
import ru.practicum.statistics.client.controller.api.StatsClientApi;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class StatsClient implements StatsClientApi {

    private final RestTemplate restTemplate;

    @Value("${stats-server.url:http://localhost:9090}")
    private String serverUrl;

    @Override
    public void sendHit(EndpointHit hit) {
        try {
            restTemplate.postForEntity(serverUrl + "/hit", hit, Void.class);
            log.info("Отправлен хит: {}", hit);
        } catch (Exception e) {
            log.error("Ошибка при отправке хита: {}", e.getMessage());
        }
    }

    @Override
    public List<ViewStats> getStats(String start, String end, List<String> uris, Boolean unique) {
        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(serverUrl + "/stats")
                    .queryParam("start", start)
                    .queryParam("end", end)
                    .queryParam("uris", String.join(",", uris))
                    .queryParam("unique", unique);

            String url = builder.build().toUriString();
            log.info("Requesting stats: {}", url);

            ResponseEntity<List<ViewStats>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<ViewStats>>() {
                    }
            );

            log.info("Stats response status: {}", response.getStatusCode());
            log.info("Stats response body: {}", response.getBody());

            return response.getBody() != null ? response.getBody() : List.of();
        } catch (Exception e) {
            log.error("Error getting stats: {}", e.getMessage(), e);
            return List.of();
        }
    }
}
