package com.example.temporal.workflows.http;

import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;

/**
 * Standalone worker for the HTTP workflow.
 *
 * <p>This worker polls the "http-task-queue" and executes HTTP workflows and activities. Run this
 * class directly to start the worker.
 *
 * <p>Usage: java com.example.temporal.workflows.http.HttpWorker
 */
public class HttpWorker {

  public static final String TASK_QUEUE = "http-task-queue";
  private static final String TEMPORAL_SERVICE_ADDRESS =
      System.getenv().getOrDefault("TEMPORAL_ADDRESS", "localhost:7233");

  public static void main(String[] args) {
    // Create connection to Temporal service
    WorkflowServiceStubsOptions options =
        WorkflowServiceStubsOptions.newBuilder().setTarget(TEMPORAL_SERVICE_ADDRESS).build();

    WorkflowServiceStubs service = WorkflowServiceStubs.newServiceStubs(options);
    WorkflowClient client = WorkflowClient.newInstance(service);

    // Create worker factory
    WorkerFactory factory = WorkerFactory.newInstance(client);

    // Create worker for the HTTP task queue
    Worker worker = factory.newWorker(TASK_QUEUE);

    // Register workflow implementation
    worker.registerWorkflowImplementationTypes(HttpWorkflowImpl.class);

    // Register activity implementation
    worker.registerActivitiesImplementations(new HttpActivitiesImpl());

    // Start the worker
    factory.start();

    System.out.println("HTTP Worker started");
    System.out.println("Temporal Service: " + TEMPORAL_SERVICE_ADDRESS);
    System.out.println("Task Queue: " + TASK_QUEUE);
    System.out.println("Press Ctrl+C to stop the worker.");
  }
}
