package ru.practicum.mainservice.clients;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import ru.practicum.common.exception.ServiceUnavailableException;
import ru.practicum.mainservice.UserServiceApplication;

import java.io.IOException;
import java.net.InetSocketAddress;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Real OpenFeign proxies against a local HTTP failure, without Docker or PostgreSQL. */
@SpringBootTest(classes = UserServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class UserServiceFeignFallbackIntegrationTest {
    private static final HttpServer stubServer = createStub();

    @Autowired
    private EventServiceClient eventServiceClient;

    @Autowired
    private RequestServiceClient requestServiceClient;

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry properties) {
        properties.add("spring.cloud.openfeign.client.config.event-service.url",
                () -> "http://127.0.0.1:" + stubServer.getAddress().getPort());
        properties.add("spring.cloud.openfeign.client.config.request-service.url",
                () -> "http://127.0.0.1:" + stubServer.getAddress().getPort());
        properties.add("spring.cloud.openfeign.circuitbreaker.enabled", () -> "true");
    }

    @AfterAll
    static void stopStub() {
        stubServer.stop(0);
    }

    @Test
    void bothDeletionGuardsRejectUnavailableDependencies() {
        assertThatThrownBy(() -> eventServiceClient.hasEvents(7L))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessageContaining("event-service");
        assertThatThrownBy(() -> requestServiceClient.hasRequests(7L))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessageContaining("request-service");
    }

    private static HttpServer createStub() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/internal/events/exists-by-initiator/",
                    UserServiceFeignFallbackIntegrationTest::unavailable);
            server.createContext("/internal/requests/exists-by-requester/",
                    UserServiceFeignFallbackIntegrationTest::unavailable);
            server.start();
            return server;
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot start local Feign test server", exception);
        }
    }

    private static void unavailable(HttpExchange exchange) throws IOException {
        try {
            exchange.sendResponseHeaders(503, -1);
        } finally {
            exchange.close();
        }
    }
}
