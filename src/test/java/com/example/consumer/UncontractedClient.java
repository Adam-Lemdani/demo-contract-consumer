package com.example.consumer;

import org.springframework.web.client.RestClient;

/**
 * Test-only client that intentionally calls a path the provider contract does
 * not define, used by {@link BreakingConsumerScenarioTest}.
 */
class UncontractedClient {

    private final RestClient restClient;

    UncontractedClient(String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    String callWrongPath(String name) {
        return restClient.get()
                .uri("/api/salutations/{name}", name)
                .retrieve()
                .body(String.class);
    }
}
