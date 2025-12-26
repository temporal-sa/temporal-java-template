# Write a New Workflow

## Table of Contents

- [File naming convention](#file-naming-convention)
- [Best practices when writing a new Workflow](#best-practices-when-writing-a-new-workflow)

1. Create a new package under `src/main/java/workflows/`
1. Create a workflow interface with `@WorkflowInterface` and implementation class
1. Define input/output POJOs (Plain Old Java Objects)
1. Use the following Temporal primitives to build a Workflow:
   - Activity
   - Signal
   - Query
   - Update
   - Timer
1. Create an activities interface and implementation class
1. Add happy path tests for both workflows and activities
1. Create a Worker class for running the workflow

Follow existing naming conventions (see examples below).

## File naming convention

- `src/main/java/workflows/http/` - uses consistent naming:
  - `HttpActivities.java` - Activity interface
  - `HttpActivitiesImpl.java` - Activity implementation
  - `HttpWorkflow.java` - Workflow interface
  - `HttpWorkflowImpl.java` - Workflow implementation
  - `HttpWorker.java` - Worker class
- Test files always end with `Test.java` and live in `src/test/java/`
- Package names should match the subdirectory structure (e.g., `workflows.http`)

## Best practices when writing a new Workflow

- Always stub Activities using logger statements to simulate actions
- Within a Workflow, use a reasonable value for `setStartToCloseTimeout()` for each Activity invocation
- Within a Workflow, do not explicitly configure retries. Do not use `RetryOptions` unless necessary.
- When Workflows, Activities, Updates, Signals, and Queries require input parameters or output values, use POJOs (Plain Old Java Objects) to define input and output.
- Never use Child Workflows. Use Activities instead of Child Workflows.
- In the Worker class, always include a `main` method that will instantiate a Temporal Client and start the Worker.

```java
package workflows.http;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;

public class HttpWorker {

    public static void main(String[] args) {
        // Create a Workflow service stub
        WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();

        // Create a Workflow client
        WorkflowClient client = WorkflowClient.newInstance(service);

        // Create a Worker factory
        WorkerFactory factory = WorkerFactory.newInstance(client);

        // Create a Worker that listens on a task queue
        Worker worker = factory.newWorker("http-task-queue");

        // Register Workflow implementation
        worker.registerWorkflowImplementationTypes(HttpWorkflowImpl.class);

        // Register Activity implementation
        worker.registerActivitiesImplementations(new HttpActivitiesImpl());

        // Start the Worker
        factory.start();

        // Execute the workflow (optional - for testing)
        HttpWorkflow workflow = client.newWorkflowStub(
            HttpWorkflow.class,
            WorkflowOptions.newBuilder()
                .setWorkflowId("http-workflow-id")
                .setTaskQueue("http-task-queue")
                .build()
        );

        HttpWorkflowInput input = new HttpWorkflowInput(
            "https://httpbin.org/anything/http-workflow"
        );
        
        HttpWorkflowOutput result = workflow.run(input);
        System.out.println("Successful Workflow Result: " + result);
    }
}
```

### Example Workflow Interface

```java
package workflows.http;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface HttpWorkflow {
    @WorkflowMethod
    HttpWorkflowOutput run(HttpWorkflowInput input);
}
```

### Example Workflow Implementation

```java
package workflows.http;

import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;

public class HttpWorkflowImpl implements HttpWorkflow {
    
    private final HttpActivities activities = 
        Workflow.newActivityStub(
            HttpActivities.class,
            ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(30))
                .build()
        );

    @Override
    public HttpWorkflowOutput run(HttpWorkflowInput input) {
        // Execute activity
        String response = activities.makeHttpRequest(input.getUrl());
        
        // Return workflow output
        return new HttpWorkflowOutput(response);
    }
}
```

### Example Activity Interface

```java
package workflows.http;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface HttpActivities {
    @ActivityMethod
    String makeHttpRequest(String url);
}
```

### Example Activity Implementation

```java
package workflows.http;

import io.temporal.activity.Activity;
import org.slf4j.Logger;

public class HttpActivitiesImpl implements HttpActivities {
    
    private static final Logger logger = 
        org.slf4j.LoggerFactory.getLogger(HttpActivitiesImpl.class);

    @Override
    public String makeHttpRequest(String url) {
        logger.info("Making HTTP request to: {}", url);
        
        // Stub: simulate HTTP request
        logger.info("Simulating HTTP GET request");
        
        return "Success: Response from " + url;
    }
}
```

### Example Input POJO

```java
package workflows.http;

public class HttpWorkflowInput {
    private String url;

    public HttpWorkflowInput() {
        // Default constructor for serialization
    }

    public HttpWorkflowInput(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
```

### Example Output POJO

```java
package workflows.http;

public class HttpWorkflowOutput {
    private String response;

    public HttpWorkflowOutput() {
        // Default constructor for serialization
    }

    public HttpWorkflowOutput(String response) {
        this.response = response;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    @Override
    public String toString() {
        return "HttpWorkflowOutput{response='" + response + "'}";
    }
}
```

### Example Test

```java
package workflows.http;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.testing.TestWorkflowExtension;
import io.temporal.worker.Worker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpWorkflowTest {

    @RegisterExtension
    public static final TestWorkflowExtension testWorkflow =
        TestWorkflowExtension.newBuilder()
            .setWorkflowTypes(HttpWorkflowImpl.class)
            .setActivityImplementations(new HttpActivitiesImpl())
            .build();

    @Test
    void testHttpWorkflow(
        TestWorkflowEnvironment testEnv,
        Worker worker,
        WorkflowClient client
    ) {
        // Create workflow stub
        HttpWorkflow workflow = client.newWorkflowStub(
            HttpWorkflow.class,
            WorkflowOptions.newBuilder()
                .setTaskQueue(worker.getTaskQueue())
                .build()
        );

        // Execute workflow
        HttpWorkflowInput input = new HttpWorkflowInput(
            "https://httpbin.org/anything/test"
        );
        HttpWorkflowOutput result = workflow.run(input);

        // Assert results
        assertNotNull(result);
        assertNotNull(result.getResponse());
        assertTrue(result.getResponse().contains("Success"));
    }
}
```
