package com.example.temporal.workflows.crawler;

import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.worker.WorkerOptions;

/**
 * Standalone worker for the crawler workflow.
 *
 * <p>This worker polls the "crawler-task-queue" and executes crawler workflows and activities. Run
 * this class directly to start the worker.
 *
 * <p>Usage: java com.example.temporal.workflows.crawler.CrawlerWorker
 */
public class CrawlerWorker {

  public static final String TASK_QUEUE = "crawler-task-queue";
  private static final String TEMPORAL_SERVICE_ADDRESS = "localhost:7233";
  private static final String NAMESPACE = "default";
  private static final int MAX_CONCURRENT_ACTIVITIES = 16;

  public static void main(String[] args) {
    // Create connection to Temporal service
    WorkflowServiceStubsOptions options =
        WorkflowServiceStubsOptions.newBuilder().setTarget(TEMPORAL_SERVICE_ADDRESS).build();

    WorkflowServiceStubs service = WorkflowServiceStubs.newServiceStubs(options);
    WorkflowClient client = WorkflowClient.newInstance(service);

    // Create worker factory
    WorkerFactory factory = WorkerFactory.newInstance(client);

    // Create worker for the crawler task queue with 16 concurrent activity workers
    WorkerOptions workerOptions =
        WorkerOptions.newBuilder()
            .setMaxConcurrentActivityExecutionSize(MAX_CONCURRENT_ACTIVITIES)
            .build();

    Worker worker = factory.newWorker(TASK_QUEUE, workerOptions);

    // Register workflow implementation
    worker.registerWorkflowImplementationTypes(CrawlerWorkflowImpl.class);

    // Register activity implementation
    worker.registerActivitiesImplementations(new CrawlerActivitiesImpl());

    // Start the worker
    factory.start();

    System.out.println("Crawler Worker started. Polling task queue: " + TASK_QUEUE);
    System.out.println("Max concurrent activities: " + MAX_CONCURRENT_ACTIVITIES);
    System.out.println("Press Ctrl+C to stop the worker.");
  }
}
