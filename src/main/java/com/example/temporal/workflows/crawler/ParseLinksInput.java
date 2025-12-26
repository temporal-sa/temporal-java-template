package com.example.temporal.workflows.crawler;

/**
 * Input data for the parse links activity.
 *
 * @param url The URL to parse links from
 */
public record ParseLinksInput(String url) {}
