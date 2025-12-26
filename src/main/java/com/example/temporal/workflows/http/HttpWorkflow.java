package com.example.temporal.workflows.http;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

/**
 * Workflow interface for HTTP GET operations.
 *
 * <p>This workflow orchestrates an HTTP GET request through activities.
 */
@WorkflowInterface
public interface HttpWorkflow {

  /**
   * Executes the HTTP workflow to fetch content from a URL.
   *
   * @param input The workflow input containing the URL to fetch
   * @return The workflow output containing the response text, URL, and status code
   */
  @WorkflowMethod
  HttpWorkflowOutput run(HttpWorkflowInput input);
}
