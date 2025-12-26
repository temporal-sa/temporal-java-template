package com.example.temporal.workflows.http;

/**
 * Output data from the HTTP workflow.
 *
 * @param responseText The response body as text
 * @param url The URL that was fetched
 * @param statusCode The HTTP status code
 */
public record HttpWorkflowOutput(String responseText, String url, int statusCode) {}
