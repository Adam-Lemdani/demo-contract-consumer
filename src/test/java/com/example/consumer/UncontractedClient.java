package com.example.consumer;

import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

/**
 * Test-only client that intentionally sends a payload shape the provider
 * contract does not define, used by {@link BreakingConsumerScenarioTest}.
 */
class UncontractedClient {

    private final RestClient restClient;

    UncontractedClient(String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    String callWrongPayload(String name) {
        return restClient.post()
                .uri("/api/greetings")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("fullName", name))
                .retrieve()
                .body(String.class);
    }
}
