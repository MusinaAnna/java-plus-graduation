package ru.practicum.statistics.server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.dto.EndpointHit;
import ru.practicum.dto.ViewStats;
import ru.practicum.statistics.server.service.StatsService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StatsController.class)
public class StatsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StatsService service;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void postHit_shouldReturn201() throws Exception {
        EndpointHit hit = new EndpointHit();
        hit.setApp("test");
        hit.setUri("/test");
        hit.setIp("1.1.1.1");
        hit.setTimestamp(LocalDateTime.now());

        mockMvc.perform(post("/hit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(hit)))
                .andExpect(status().isCreated());
    }

    @Test
    void getStats_shouldReturnStatsList() throws Exception {
        List<ViewStats> expected = List.of(
                new ViewStats("app1", "/uri1", 5L)
        );

        when(service.getStats(any(), any(), any(), anyBoolean())).thenReturn(expected);

        mockMvc.perform(get("/stats")
                        .param("start", "2025-01-01 10:00:00")
                        .param("end", "2025-01-01 12:00:00")
                        .param("uris", "/uri1")
                        .param("unique", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(1))
                .andExpect(jsonPath("$[0].app").value("app1"));

    }
}
