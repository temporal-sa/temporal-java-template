package com.example.temporal.workflows.crawler;

import java.util.Set;

/**
 * Output data from the crawler workflow.
 *
 * @param totalLinksCrawled The total number of links that were crawled
 * @param linksDiscovered The set of all unique links discovered
 * @param domainsDiscovered The set of all unique domains discovered
 */
public record CrawlerWorkflowOutput(
    int totalLinksCrawled, Set<String> linksDiscovered, Set<String> domainsDiscovered) {}
