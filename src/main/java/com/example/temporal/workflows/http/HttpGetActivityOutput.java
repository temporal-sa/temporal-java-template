package com.example.temporal.workflows.http;

/**
 * Output data from the HTTP GET activity.
 *
 * @param responseText The response body as text
 * @param statusCode The HTTP status code
 */
public record HttpGetActivityOutput(String responseText, int statusCode) {}
