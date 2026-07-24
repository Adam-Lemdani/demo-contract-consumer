package com.example.consumer;

/**
 * Consumer-side view of the provider response. The consumer only depends on
 * the {@code message} field of GET /api/greetings/{name}.
 */
public record Greeting(String message) {
}
