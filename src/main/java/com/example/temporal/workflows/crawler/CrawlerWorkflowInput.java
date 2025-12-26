package com.example.temporal.workflows.crawler;

/**
 * Input data for the crawler workflow.
 *
 * @param startUrl The URL to start crawling from
 * @param maxLinks The maximum number of links to crawl (default: 10)
 */
public record CrawlerWorkflowInput(String startUrl, int maxLinks) {

  /**
   * Constructor with default maxLinks value of 10.
   *
   * @param startUrl The URL to start crawling from
   */
  public CrawlerWorkflowInput(String startUrl) {
    this(startUrl, 10);
  }
}
