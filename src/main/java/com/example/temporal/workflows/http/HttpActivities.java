package com.example.temporal.workflows.http;

import io.temporal.activity.ActivityInterface;

/**
 * Activity interface for HTTP operations.
 *
 * <p>Activities are the unit of work that interact with external systems. All HTTP requests must be
 * made through activities, not directly in workflows.
 */
@ActivityInterface
public interface HttpActivities {

  /**
   * Performs an HTTP GET request to the specified URL.
   *
   * @param input The input containing the URL to fetch
   * @return The output containing the response text and status code
   */
  HttpGetActivityOutput httpGet(HttpGetActivityInput input);
}
