package com.example.temporal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Spring Boot application entry point for the Temporal Java Template.
 *
 * <p>Note: This application does NOT auto-start Temporal workers. Workers are run separately as
 * standalone applications (see HttpWorker.java and CrawlerWorker.java).
 *
 * <p>This class primarily serves to configure Spring beans (such as WorkflowClient) that can be
 * used by workers or for workflow execution.
 */
@SpringBootApplication
public class TemporalApplication {

  public static void main(String[] args) {
    SpringApplication.run(TemporalApplication.class, args);
  }
}
