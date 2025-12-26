package com.example.temporal;

import io.temporal.client.WorkflowClient;
import io.temporal.testing.TestWorkflowEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Test configuration for Temporal workflow tests.
 *
 * <p>This configuration provides a TestWorkflowEnvironment for integration testing with
 * time-skipping capabilities.
 */
@TestConfiguration
public class TestConfig {

  /**
   * Creates a TestWorkflowEnvironment bean for testing.
   *
   * <p>TestWorkflowEnvironment allows for time-skipping and deterministic testing of workflows.
   *
   * @return A new TestWorkflowEnvironment instance
   */
  @Bean
  public TestWorkflowEnvironment testWorkflowEnvironment() {
    return TestWorkflowEnvironment.newInstance();
  }

  /**
   * Creates a WorkflowClient bean from the test environment.
   *
   * @param testEnv The test workflow environment
   * @return A WorkflowClient for test execution
   */
  @Bean
  public WorkflowClient testWorkflowClient(TestWorkflowEnvironment testEnv) {
    return testEnv.getWorkflowClient();
  }
}
