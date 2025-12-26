package com.example.temporal.workflows.http;

import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;
import org.slf4j.Logger;

/**
 * Implementation of the HTTP workflow.
 *
 * <p>This workflow demonstrates a simple pattern of calling an activity to perform an HTTP GET
 * request.
 */
public class HttpWorkflowImpl implements HttpWorkflow {

  private static final Logger logger = Workflow.getLogger(HttpWorkflowImpl.class);

  /** Activity options with a 3-second timeout (matching Python implementation). */
  private final ActivityOptions activityOptions =
      ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(3)).build();

  /** Activity stub for executing HTTP activities. */
  private final HttpActivities activities =
      Workflow.newActivityStub(HttpActivities.class, activityOptions);

  @Override
  public HttpWorkflowOutput run(HttpWorkflowInput input) {
    logger.info("Starting HTTP workflow for URL: {}", input.url());

    // Execute the HTTP GET activity
    HttpGetActivityInput activityInput = new HttpGetActivityInput(input.url());
    HttpGetActivityOutput activityOutput = activities.httpGet(activityInput);

    logger.info(
        "HTTP workflow completed. Status code: {}, Response length: {}",
        activityOutput.statusCode(),
        activityOutput.responseText().length());

    // Return the workflow output
    return new HttpWorkflowOutput(
        activityOutput.responseText(), input.url(), activityOutput.statusCode());
  }
}
