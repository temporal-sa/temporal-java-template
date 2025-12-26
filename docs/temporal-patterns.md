# Temporal Patterns in Java

This guide covers Java-specific Temporal patterns and best practices used in this template.

## Table of Contents

- [Workflow Determinism](#workflow-determinism)
- [Activity Options and Retry Policies](#activity-options-and-retry-policies)
- [Parallel Execution](#parallel-execution)
- [Data Serialization](#data-serialization)
- [Logging](#logging)
- [Error Handling](#error-handling)
- [Workflow Versioning](#workflow-versioning)
- [Signals and Queries](#signals-and-queries)

## Workflow Determinism

Workflows in Temporal must be deterministic to support replay. This means they must produce the same output given the same input and event history.

### ❌ Non-Deterministic Operations

**DO NOT** use these in workflow code:

```java
// ❌ BAD: Non-deterministic time
public class BadWorkflowImpl implements MyWorkflow {
    public String run() {
        long now = System.currentTimeMillis(); // Non-deterministic!
        return "Time: " + now;
    }
}

// ❌ BAD: Non-deterministic random
public class BadWorkflowImpl implements MyWorkflow {
    public String run() {
        double random = Math.random(); // Non-deterministic!
        return "Random: " + random;
    }
}

// ❌ BAD: Direct I/O operations
public class BadWorkflowImpl implements MyWorkflow {
    public String run() {
        String data = fetchFromDatabase(); // Non-deterministic!
        return data;
    }
}
```

### ✅ Deterministic Alternatives

**DO** use these instead:

```java
// ✅ GOOD: Use Workflow.currentTimeMillis()
public class GoodWorkflowImpl implements MyWorkflow {
    public String run() {
        long now = Workflow.currentTimeMillis(); // Deterministic!
        return "Time: " + now;
    }
}

// ✅ GOOD: Use Workflow.newRandom()
public class GoodWorkflowImpl implements MyWorkflow {
    public String run() {
        Random random = Workflow.newRandom();
        double value = random.nextDouble(); // Deterministic!
        return "Random: " + value;
    }
}

// ✅ GOOD: Use activities for I/O
public class GoodWorkflowImpl implements MyWorkflow {
    private final MyActivities activities =
        Workflow.newActivityStub(MyActivities.class, activityOptions);

    public String run() {
        String data = activities.fetchFromDatabase(); // Deterministic!
        return data;
    }
}
```

### Logging in Workflows

Always use `Workflow.getLogger()` instead of `LoggerFactory`:

```java
public class MyWorkflowImpl implements MyWorkflow {
    // ✅ GOOD: Workflow logger
    private static final Logger logger = Workflow.getLogger(MyWorkflowImpl.class);

    public String run(String input) {
        logger.info("Starting workflow with input: {}", input);
        // Workflow logic...
        return "result";
    }
}
```

## Activity Options and Retry Policies

### Basic Activity Options

Configure activity timeouts and retry behavior:

```java
public class MyWorkflowImpl implements MyWorkflow {

    private final ActivityOptions activityOptions =
        ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofSeconds(10))
            .build();

    private final MyActivities activities =
        Workflow.newActivityStub(MyActivities.class, activityOptions);
}
```

### Custom Retry Policy

Configure how activities retry on failure:

```java
ActivityOptions options = ActivityOptions.newBuilder()
    .setStartToCloseTimeout(Duration.ofSeconds(30))
    .setRetryOptions(
        RetryOptions.newBuilder()
            .setInitialInterval(Duration.ofSeconds(1))
            .setMaximumInterval(Duration.ofSeconds(60))
            .setBackoffCoefficient(2.0)
            .setMaximumAttempts(5)
            .build())
    .build();
```

### Activity Timeout Types

```java
ActivityOptions options = ActivityOptions.newBuilder()
    // Maximum time for single activity execution
    .setStartToCloseTimeout(Duration.ofSeconds(30))

    // Maximum time from schedule to completion (including retries)
    .setScheduleToCloseTimeout(Duration.ofMinutes(5))

    // Maximum time between heartbeats
    .setHeartbeatTimeout(Duration.ofSeconds(10))

    .build();
```

### Local Activities

For short, fast operations that don't need full durability:

```java
LocalActivityOptions localOptions = LocalActivityOptions.newBuilder()
    .setStartToCloseTimeout(Duration.ofSeconds(2))
    .build();

MyActivities localActivities =
    Workflow.newLocalActivityStub(MyActivities.class, localOptions);
```

## Parallel Execution

### Using Async.function() for Parallel Activities

Execute multiple activities concurrently:

```java
public CrawlerWorkflowOutput run(CrawlerWorkflowInput input) {
    List<Promise<ParseLinksOutput>> promises = new ArrayList<>();

    // Execute activities in parallel
    for (String url : urlsToProcess) {
        Promise<ParseLinksOutput> promise = Async.function(
            activities::parseLinksFromUrl,
            new ParseLinksInput(url)
        );
        promises.add(promise);
    }

    // Wait for all to complete
    for (Promise<ParseLinksOutput> promise : promises) {
        ParseLinksOutput result = promise.get();
        // Process result...
    }

    return output;
}
```

### Batch Processing

Process items in batches for controlled parallelism:

```java
int batchSize = 10;
for (int i = 0; i < items.size(); i += batchSize) {
    List<Promise<Result>> batch = new ArrayList<>();

    // Create batch
    for (int j = i; j < Math.min(i + batchSize, items.size()); j++) {
        batch.add(Async.function(activities::process, items.get(j)));
    }

    // Wait for batch to complete
    for (Promise<Result> promise : batch) {
        results.add(promise.get());
    }
}
```

### Async.procedure() for Side Effects

For activities that don't return values:

```java
List<Promise<Void>> promises = new ArrayList<>();

for (String notification : notifications) {
    promises.add(Async.procedure(activities::sendEmail, notification));
}

// Wait for all
Promise.allOf(promises).get();
```

## Data Serialization

### Using Records for Data Models

Java records provide immutable, serializable data models:

```java
// ✅ GOOD: Immutable record
public record WorkflowInput(String url, int maxRetries) {}

// ✅ GOOD: Records are automatically serializable
public record WorkflowOutput(
    String result,
    List<String> items,
    Map<String, Integer> stats
) {}
```

### Custom Objects

If using classes instead of records:

```java
// Must be serializable by Jackson
public class CustomInput {
    private final String field1;
    private final int field2;

    // Constructor
    public CustomInput(String field1, int field2) {
        this.field1 = field1;
        this.field2 = field2;
    }

    // Getters required for Jackson
    public String getField1() { return field1; }
    public int getField2() { return field2; }
}
```

### Collections

Use standard Java collections - they serialize automatically:

```java
public record MyOutput(
    List<String> items,           // ✅ Good
    Set<String> uniqueItems,      // ✅ Good
    Map<String, Integer> counts   // ✅ Good
) {}
```

## Logging

### Workflow Logging

```java
public class MyWorkflowImpl implements MyWorkflow {
    private static final Logger logger = Workflow.getLogger(MyWorkflowImpl.class);

    public String run(String input) {
        logger.info("Workflow started with input: {}", input);

        // Workflow logic
        String result = activities.process(input);

        logger.info("Workflow completed with result: {}", result);
        return result;
    }
}
```

### Activity Logging

Activities can use standard SLF4J logging:

```java
@Component
public class MyActivitiesImpl implements MyActivities {
    private static final Logger logger =
        LoggerFactory.getLogger(MyActivitiesImpl.class);

    public String process(String input) {
        logger.info("Processing: {}", input);
        // Activity logic
        return result;
    }
}
```

## Error Handling

### Activity Errors

Activities can throw exceptions that will be retried:

```java
@Component
public class MyActivitiesImpl implements MyActivities {

    public String fetchData(String url) {
        try {
            return httpClient.get(url);
        } catch (IOException e) {
            // This will trigger retry based on RetryOptions
            throw new RuntimeException("Failed to fetch: " + url, e);
        }
    }
}
```

### Workflow Error Handling

Handle activity failures in workflows:

```java
public String run(String input) {
    try {
        return activities.riskyOperation(input);
    } catch (ActivityFailure e) {
        logger.error("Activity failed: {}", e.getMessage());
        // Fallback logic or compensation
        return activities.fallbackOperation(input);
    }
}
```

### Non-Retryable Errors

Mark specific exceptions as non-retryable:

```java
ActivityOptions options = ActivityOptions.newBuilder()
    .setStartToCloseTimeout(Duration.ofSeconds(30))
    .setRetryOptions(
        RetryOptions.newBuilder()
            .setDoNotRetry(IllegalArgumentException.class.getName())
            .build())
    .build();
```

## Workflow Versioning

Handle workflow code changes over time:

```java
public class MyWorkflowImpl implements MyWorkflow {

    public String run(String input) {
        int version = Workflow.getVersion("my-change",
            Workflow.DEFAULT_VERSION, 1);

        if (version == Workflow.DEFAULT_VERSION) {
            // Old behavior for existing workflows
            return activities.oldMethod(input);
        } else {
            // New behavior for new workflows
            return activities.newMethod(input);
        }
    }
}
```

### Patching Workflows

For simple changes:

```java
public String run(String input) {
    String result = activities.step1(input);

    if (Workflow.getVersion("add-step2",
            Workflow.DEFAULT_VERSION, 1) == 1) {
        result = activities.step2(result);
    }

    return result;
}
```

## Signals and Queries

### Adding Signals

Workflows can receive external signals:

```java
@WorkflowInterface
public interface MyWorkflow {
    @WorkflowMethod
    String run(String input);

    @SignalMethod
    void updateConfig(Config newConfig);
}

public class MyWorkflowImpl implements MyWorkflow {
    private Config config;

    @Override
    public void updateConfig(Config newConfig) {
        this.config = newConfig;
    }

    @Override
    public String run(String input) {
        // Wait for signal if needed
        Workflow.await(() -> config != null);
        // Use config...
        return "result";
    }
}
```

### Adding Queries

Query workflow state without blocking:

```java
@WorkflowInterface
public interface MyWorkflow {
    @WorkflowMethod
    String run(String input);

    @QueryMethod
    String getStatus();
}

public class MyWorkflowImpl implements MyWorkflow {
    private String status = "RUNNING";

    @Override
    public String getStatus() {
        return status;
    }

    @Override
    public String run(String input) {
        status = "PROCESSING";
        // Workflow logic...
        status = "COMPLETED";
        return "result";
    }
}
```

### Querying from Client

```java
MyWorkflow workflow = client.newWorkflowStub(
    MyWorkflow.class,
    workflowId
);

String status = workflow.getStatus();
System.out.println("Workflow status: " + status);
```

## Best Practices Summary

1. ✅ **Keep workflows deterministic** - Use `Workflow.*` methods for time, random, etc.
2. ✅ **Use activities for I/O** - All external interactions must go through activities
3. ✅ **Use immutable data models** - Records are ideal for workflow inputs/outputs
4. ✅ **Configure timeouts** - Always set appropriate timeouts for activities
5. ✅ **Use parallel execution** - Leverage `Async.function()` for concurrent operations
6. ✅ **Log properly** - Use `Workflow.getLogger()` in workflows
7. ✅ **Handle errors gracefully** - Configure retry policies and handle failures
8. ✅ **Version workflows** - Use `Workflow.getVersion()` for code changes
9. ✅ **Test with TestWorkflowEnvironment** - Leverage time-skipping for testing
10. ✅ **Don't inject Spring beans into workflows** - Workflows are serialized and replayed

## Additional Resources

- [Temporal Java SDK Documentation](https://docs.temporal.io/dev-guide/java)
- [Temporal Best Practices](https://docs.temporal.io/kb)
- [Temporal Samples Java](https://github.com/temporalio/samples-java)
