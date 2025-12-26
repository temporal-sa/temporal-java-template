package com.example.temporal.workflows.crawler;

import java.util.List;

/**
 * Output data from the parse links activity.
 *
 * @param links The list of links discovered
 */
public record ParseLinksOutput(List<String> links) {}
