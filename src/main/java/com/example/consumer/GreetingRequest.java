package com.example.consumer;

/**
 * Consumer-side request body for POST /api/greetings.
 */
public record GreetingRequest(String fullName) {
}
