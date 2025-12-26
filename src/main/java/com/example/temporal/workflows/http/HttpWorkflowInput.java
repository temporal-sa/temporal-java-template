package com.example.temporal.workflows.http;

/**
 * Input data for the HTTP workflow.
 *
 * @param url The URL to fetch via HTTP GET
 */
public record HttpWorkflowInput(String url) {}
