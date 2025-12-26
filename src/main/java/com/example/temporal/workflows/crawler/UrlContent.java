package com.example.temporal.workflows.crawler;

/**
 * Represents the content fetched from a URL.
 *
 * @param url The URL that was fetched
 * @param htmlContent The HTML content retrieved from the URL
 * @param success Whether the fetch was successful
 */
public record UrlContent(String url, String htmlContent, boolean success) {}
