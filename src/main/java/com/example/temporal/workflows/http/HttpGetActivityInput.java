package com.example.temporal.workflows.http;

/**
 * Input data for the HTTP GET activity.
 *
 * @param url The URL to fetch via HTTP GET
 */
public record HttpGetActivityInput(String url) {}
