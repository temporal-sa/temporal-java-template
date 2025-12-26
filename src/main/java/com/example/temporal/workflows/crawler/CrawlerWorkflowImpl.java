package com.example.temporal.workflows.crawler;

import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Async;
import io.temporal.workflow.Promise;
import io.temporal.workflow.Workflow;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import org.slf4j.Logger;

/**
 * Implementation of the crawler workflow.
 *
 * <p>This workflow implements a breadth-first web crawler that discovers links across multiple
 * pages in parallel while respecting the maxLinks limit.
 */
public class CrawlerWorkflowImpl implements CrawlerWorkflow {

  private static final Logger logger = Workflow.getLogger(CrawlerWorkflowImpl.class);

  /** Activity options with a 10-second timeout (matching Python implementation). */
  private final ActivityOptions activityOptions =
      ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(10)).build();

  /** Activity stub for executing crawler activities. */
  private final CrawlerActivities activities =
      Workflow.newActivityStub(CrawlerActivities.class, activityOptions);

  @Override
  public CrawlerWorkflowOutput run(CrawlerWorkflowInput input) {
    logger.info(
        "Starting crawler workflow for URL: {} (maxLinks: {})", input.startUrl(), input.maxLinks());

    Set<String> discoveredLinks = new HashSet<>();
    Set<String> discoveredDomains = new HashSet<>();
    Queue<String> urlsToProcess = new LinkedList<>();

    // Add the start URL to the queue
    urlsToProcess.add(input.startUrl());
    discoveredLinks.add(input.startUrl());

    int linksCrawled = 0;

    while (!urlsToProcess.isEmpty() && linksCrawled < input.maxLinks()) {
      // Process URLs in batches for parallel execution
      List<Promise<ParseLinksOutput>> promises = new ArrayList<>();
      List<String> currentBatch = new ArrayList<>();

      // Build batch of URLs to process in parallel
      while (!urlsToProcess.isEmpty()
          && linksCrawled < input.maxLinks()
          && currentBatch.size() < 10) {
        String url = urlsToProcess.poll();
        currentBatch.add(url);
        linksCrawled++;

        // Execute activity asynchronously
        ParseLinksInput activityInput = new ParseLinksInput(url);
        Promise<ParseLinksOutput> promise =
            Async.function(activities::parseLinksFromUrl, activityInput);
        promises.add(promise);
      }

      // Wait for all promises in the batch to complete
      for (int i = 0; i < promises.size(); i++) {
        ParseLinksOutput output = promises.get(i).get();
        String crawledUrl = currentBatch.get(i);

        // Extract and track domain
        String domain = extractDomain(crawledUrl);
        if (domain != null) {
          discoveredDomains.add(domain);
        }

        // Add new links to the queue
        for (String link : output.links()) {
          if (!discoveredLinks.contains(link)) {
            discoveredLinks.add(link);
            urlsToProcess.add(link);

            // Track domain for the discovered link
            String linkDomain = extractDomain(link);
            if (linkDomain != null) {
              discoveredDomains.add(linkDomain);
            }
          }
        }
      }

      logger.info(
          "Crawled {} URLs so far, discovered {} total links across {} domains",
          linksCrawled,
          discoveredLinks.size(),
          discoveredDomains.size());
    }

    logger.info(
        "Crawler workflow completed. Total links crawled: {}, Total links discovered: {}, "
            + "Total domains: {}",
        linksCrawled,
        discoveredLinks.size(),
        discoveredDomains.size());

    return new CrawlerWorkflowOutput(linksCrawled, discoveredLinks, discoveredDomains);
  }

  /**
   * Extracts the domain from a URL.
   *
   * @param url The URL to extract the domain from
   * @return The domain, or null if extraction fails
   */
  private String extractDomain(String url) {
    try {
      URI uri = new URI(url);
      return uri.getHost();
    } catch (Exception e) {
      logger.debug("Failed to extract domain from URL: {}", url);
      return null;
    }
  }
}
