package com.example.temporal.workflows.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for HttpWorkflow.
 *
 * <p>These tests use TestWorkflowEnvironment to test the workflow with mocked activities.
 */
class HttpWorkflowTest {

  private TestWorkflowEnvironment testEnv;
  private Worker worker;
  private WorkflowClient client;

  @BeforeEach
  void setUp() {
    testEnv = TestWorkflowEnvironment.newInstance();
    worker = testEnv.newWorker(HttpWorker.TASK_QUEUE);
    worker.registerWorkflowImplementationTypes(HttpWorkflowImpl.class);
    client = testEnv.getWorkflowClient();
  }

  @AfterEach
  void tearDown() {
    testEnv.close();
  }

  @Test
  void testHttpWorkflow_Success() {
    // Arrange
    HttpActivities mockActivities = mock(HttpActivities.class);
    worker.registerActivitiesImplementations(mockActivities);

    String testUrl = "https://example.com";
    String responseText = "<html><body>Test Content</body></html>";
    int statusCode = 200;

    HttpGetActivityOutput activityOutput = new HttpGetActivityOutput(responseText, statusCode);
    when(mockActivities.httpGet(any(HttpGetActivityInput.class))).thenReturn(activityOutput);

    testEnv.start();

    // Act
    HttpWorkflow workflow =
        client.newWorkflowStub(
            HttpWorkflow.class,
            WorkflowOptions.newBuilder().setTaskQueue(HttpWorker.TASK_QUEUE).build());

    HttpWorkflowInput input = new HttpWorkflowInput(testUrl);
    HttpWorkflowOutput output = workflow.run(input);

    // Assert
    assertNotNull(output);
    assertEquals(responseText, output.responseText());
    assertEquals(testUrl, output.url());
    assertEquals(statusCode, output.statusCode());

    // Verify activity was called
    verify(mockActivities).httpGet(any(HttpGetActivityInput.class));
  }

  @Test
  void testHttpWorkflow_ActivityInvocation() {
    // Arrange
    HttpActivities mockActivities = mock(HttpActivities.class);
    worker.registerActivitiesImplementations(mockActivities);

    String testUrl = "https://test.com/api";
    HttpGetActivityOutput activityOutput = new HttpGetActivityOutput("Response", 201);
    when(mockActivities.httpGet(any(HttpGetActivityInput.class))).thenReturn(activityOutput);

    testEnv.start();

    // Act
    HttpWorkflow workflow =
        client.newWorkflowStub(
            HttpWorkflow.class,
            WorkflowOptions.newBuilder().setTaskQueue(HttpWorker.TASK_QUEUE).build());

    HttpWorkflowInput input = new HttpWorkflowInput(testUrl);
    workflow.run(input);

    // Assert - Verify the activity was called with correct input
    verify(mockActivities).httpGet(new HttpGetActivityInput(testUrl));
  }
}
