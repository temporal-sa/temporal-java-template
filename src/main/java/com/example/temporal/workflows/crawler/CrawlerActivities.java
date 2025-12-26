package com.example.temporal.workflows.crawler;

import io.temporal.activity.ActivityInterface;

/**
 * Activity interface for web crawling operations.
 *
 * <p>Activities handle the actual HTTP requests and HTML parsing, which must be done outside of
 * workflows to maintain determinism.
 */
@ActivityInterface
public interface CrawlerActivities {

  /**
   * Parses links from a given URL by fetching the content and extracting all hyperlinks.
   *
   * @param input The input containing the URL to parse
   * @return The output containing the list of discovered links
   */
  ParseLinksOutput parseLinksFromUrl(ParseLinksInput input);
}
