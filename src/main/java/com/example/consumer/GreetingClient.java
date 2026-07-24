package com.example.consumer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Thin HTTP client for the provider's greetings endpoint.
 *
 * <p>The base URL is injected so tests can point it at the Stub Runner port.
 * The path {@code /api/greetings/{name}} is the runtime relationship that the
 * contract protects: changing it here is the canonical breaking consumer change.
 */
@Component
public class GreetingClient {

    private final RestClient restClient;

    public GreetingClient(@Value("${provider.base-url}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public String greetingFor(String name) {
        Greeting greeting = restClient.get()
                .uri("/api/greetings/{name}", name)
                .retrieve()
                .body(Greeting.class);
        return greeting == null ? null : greeting.message();
    }
}
