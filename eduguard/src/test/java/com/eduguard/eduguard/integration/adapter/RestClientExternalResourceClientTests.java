package com.eduguard.eduguard.integration.adapter;

import com.eduguard.eduguard.integration.config.IntegrationProviderProperties;
import com.eduguard.eduguard.integration.model.IntegrationType;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RestClientExternalResourceClientTests {
    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void requestContainsOnlyFixedLimitAndDoesNotTransmitSearchOrStudentData() throws Exception {
        AtomicReference<String> requestUri = new AtomicReference<>();
        AtomicReference<String> authorization = new AtomicReference<>();
        startServer(exchange -> {
            requestUri.set(exchange.getRequestURI().toString());
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            respond(exchange, 200, """
                    [{
                      "id":"resource-1",
                      "title":"Study skills",
                      "description":"General learning material",
                      "category":"Study Skills",
                      "provider":"Example Provider",
                      "url":"https://provider.example/resource-1"
                    }]
                    """);
        });
        IntegrationProviderProperties properties = properties();
        properties.setApiKey("test-secret");
        RestClientExternalResourceClient client = new RestClientExternalResourceClient(RestClient.builder());

        var response = client.fetchResources(IntegrationType.ACADEMIC, properties);

        assertEquals("resource-1", response.getFirst().id());
        assertEquals("/resources?limit=50", requestUri.get());
        assertEquals("Bearer test-secret", authorization.get());
    }

    @Test
    void malformedJsonIsClassifiedWithoutLeakingProviderBody() throws Exception {
        startServer(exchange -> respond(exchange, 200, "{ malformed-provider-secret"));
        RestClientExternalResourceClient client = new RestClientExternalResourceClient(RestClient.builder());

        IntegrationCallException exception = assertThrows(IntegrationCallException.class,
                () -> client.fetchResources(IntegrationType.ACADEMIC, properties()));

        assertEquals(IntegrationFailure.MALFORMED_RESPONSE, exception.getFailure());
    }

    @Test
    void upstreamTimeoutIsClassified() throws Exception {
        startServer(exchange -> {
            try {
                Thread.sleep(500);
                respond(exchange, 200, "[]");
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            } catch (IOException ignored) {
                exchange.close();
            }
        });
        IntegrationProviderProperties properties = properties();
        properties.setReadTimeout(Duration.ofMillis(50));
        RestClientExternalResourceClient client = new RestClientExternalResourceClient(RestClient.builder());

        IntegrationCallException exception = assertThrows(IntegrationCallException.class,
                () -> client.fetchResources(IntegrationType.ACADEMIC, properties));

        assertEquals(IntegrationFailure.TIMEOUT, exception.getFailure());
    }

    @Test
    void connectionFailureIsClassifiedAsUnavailable() throws Exception {
        startServer(exchange -> respond(exchange, 200, "[]"));
        int closedPort = server.getAddress().getPort();
        IntegrationProviderProperties properties = properties();
        properties.setBaseUrl("http://127.0.0.1:" + closedPort);
        server.stop(0);
        server = null;
        RestClientExternalResourceClient client = new RestClientExternalResourceClient(RestClient.builder());

        IntegrationCallException exception = assertThrows(IntegrationCallException.class,
                () -> client.fetchResources(IntegrationType.ACADEMIC, properties));

        assertEquals(IntegrationFailure.UNAVAILABLE, exception.getFailure());
    }

    private void startServer(HttpHandler handler) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", handler);
        server.start();
    }

    private void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (var output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    private IntegrationProviderProperties properties() {
        IntegrationProviderProperties properties = new IntegrationProviderProperties();
        properties.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
        properties.setConnectTimeout(Duration.ofSeconds(1));
        properties.setReadTimeout(Duration.ofSeconds(1));
        return properties;
    }
}
