package com.example.temporal.workflows.crawler;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

/**
 * Workflow interface for web crawling operations.
 *
 * <p>This workflow orchestrates the crawling of multiple web pages, discovering links in a
 * breadth-first manner while respecting the maxLinks limit.
 */
@WorkflowInterface
public interface CrawlerWorkflow {

  /**
   * Executes the crawler workflow to discover and crawl links starting from a URL.
   *
   * @param input The workflow input containing the start URL and max links limit
   * @return The workflow output containing crawl statistics and discovered links/domains
   */
  @WorkflowMethod
  CrawlerWorkflowOutput run(CrawlerWorkflowInput input);
}
