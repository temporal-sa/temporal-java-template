# Temporal Primitives

## Table of Contents

- [Workflow](#workflow)
- [Activity](#activity)
- [Timer (fixed time)](#timer-fixed-time)
- [Timer (event-driven)](#timer-event-driven)
- [Query](#query)
- [Signal](#signal)
- [Update](#update)
- [Retry Policy](#retry-policy)
- [Search Attributes](#search-attributes)

## Workflow

Workflows orchestrate business logic and coordinate activities. They must be deterministic and replay-safe.

```java
@WorkflowInterface
public interface MyWorkflow {
    @WorkflowMethod
    void run();
}

public class MyWorkflowImpl implements MyWorkflow {
    private final MyActivities activities = 
        Workflow.newActivityStub(
            MyActivities.class,
            ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(10))
                .build()
        );

    @Override
    public void run() {
        activities.doDatabaseThing();
    }
}
```

**Key characteristics:**

- Use `@WorkflowInterface` and `@WorkflowMethod` annotations
- Entry point marked with `@WorkflowMethod`
- Execute activities via `Workflow.newActivityStub()`
- Must be deterministic (no direct I/O, random numbers, system calls)

## Activity

Activities handle non-deterministic operations like database operations, HTTP calls, and file I/O. They can be retried independently and must be idempotent.

```java
@ActivityInterface
public interface MyActivities {
    @ActivityMethod
    void doDatabaseThing();
}

public class MyActivitiesImpl implements MyActivities {
    private final MyDatabaseClient dbClient;

    public MyActivitiesImpl(MyDatabaseClient dbClient) {
        this.dbClient = dbClient;
    }

    @Override
    public void doDatabaseThing() {
        dbClient.runDatabaseUpdate();
    }
}
```

**Key characteristics:**

- Use `@ActivityInterface` and `@ActivityMethod` annotations
- Can maintain state through class instances
- Handle all non-deterministic operations
- Automatically retryable on failure

## Timer (fixed time)

Timers provide deterministic delays in Workflows using `Workflow.sleep()`. Timers are replay-safe and respect Temporal's execution guarantees.

```java
@WorkflowInterface
public interface TimerWorkflow {
    @WorkflowMethod
    String run(String name);
}

public class TimerWorkflowImpl implements TimerWorkflow {
    @Override
    public String run(String name) {
        String greeting = "Hello, " + name + "!";
        // Deterministic 2-second delay
        Workflow.sleep(Duration.ofSeconds(2));
        return "Goodbye, " + name + "!";
    }
}
```

**Key characteristics:**

- Use `Workflow.sleep(Duration)` for deterministic delays
- Timers are replay-safe and persistent across workflow restarts
- Never use `Thread.sleep()` in workflows

## Timer (event-driven)

```java
@WorkflowInterface
public interface TimerWorkflow {
    @WorkflowMethod
    List<String> run();
    
    @SignalMethod
    void addItem(String item);
    
    @SignalMethod
    void exit();
}

public class TimerWorkflowImpl implements TimerWorkflow {
    private final List<String> queue = new ArrayList<>();
    private boolean exit = false;

    @Override
    public List<String> run() {
        List<String> results = new ArrayList<>();
        while (true) {
            // Wait for condition or timeout
            Workflow.await(() -> !queue.isEmpty() || exit);
            
            // Process queue items
            results.addAll(queue);
            queue.clear();
            
            if (exit) {
                return results;
            }
        }
    }

    @Override
    public void addItem(String item) {
        queue.add(item);
    }

    @Override
    public void exit() {
        this.exit = true;
    }
}
```

**Key characteristics:**

- Use `Workflow.await(() -> condition)` for event-based waiting
- Timers are replay-safe and persistent across workflow restarts

## Query

Queries allow external clients to read workflow state without affecting execution. They're synchronous, read-only operations that work even after workflow completion.

```java
@WorkflowInterface
public interface GreetingWorkflow {
    @WorkflowMethod
    void run(String name);
    
    @QueryMethod
    String getGreeting();
}

public class GreetingWorkflowImpl implements GreetingWorkflow {
    private String greeting = "";

    @Override
    public void run(String name) {
        this.greeting = "Hello, " + name + "!";
        Workflow.sleep(Duration.ofSeconds(2));
        this.greeting = "Goodbye, " + name + "!";
    }

    @Override
    public String getGreeting() {
        return greeting;
    }
}

// Client usage
WorkflowClient client = WorkflowClient.newInstance(service);
GreetingWorkflow workflow = client.newWorkflowStub(
    GreetingWorkflow.class,
    WorkflowOptions.newBuilder()
        .setWorkflowId("greeting-workflow")
        .setTaskQueue("greeting-queue")
        .build()
);

WorkflowClient.start(workflow::run, "World");
String result = workflow.getGreeting();
```

**Key characteristics:**

- Use `@QueryMethod` annotation for query methods
- Read-only operations that don't modify workflow state
- Work during execution and after workflow completion
- Synchronous and deterministic

## Signal

Signals allow external clients to send asynchronous messages to running workflows, enabling dynamic interaction and state changes during execution.

```java
@WorkflowInterface
public interface GreetingWorkflow {
    @WorkflowMethod
    List<String> run();
    
    @SignalMethod
    void submitGreeting(String name);
    
    @SignalMethod
    void exit();
}

public class GreetingWorkflowImpl implements GreetingWorkflow {
    private final List<String> pendingGreetings = new ArrayList<>();
    private boolean exit = false;

    @Override
    public List<String> run() {
        List<String> greetings = new ArrayList<>();
        while (true) {
            Workflow.await(() -> !pendingGreetings.isEmpty() || exit);
            
            for (String name : pendingGreetings) {
                greetings.add("Hello, " + name);
            }
            pendingGreetings.clear();
            
            if (exit) {
                return greetings;
            }
        }
    }

    @Override
    public void submitGreeting(String name) {
        pendingGreetings.add(name);
    }

    @Override
    public void exit() {
        this.exit = true;
    }
}

// Client usage
WorkflowClient client = WorkflowClient.newInstance(service);
GreetingWorkflow workflow = client.newWorkflowStub(
    GreetingWorkflow.class,
    WorkflowOptions.newBuilder()
        .setWorkflowId("greeting-workflow")
        .setTaskQueue("greeting-queue")
        .build()
);

WorkflowClient.start(workflow::run);
workflow.submitGreeting("user1");
workflow.exit();
```

**Key characteristics:**

- Use `@SignalMethod` annotation for signal methods
- Asynchronous, fire-and-forget operations
- Can modify workflow state and trigger workflow logic
- Often combined with `Workflow.await()` for event-driven workflows

## Update

Updates allow external clients to send synchronous messages to workflows and receive responses. Unlike signals, updates can return values and provide stronger consistency guarantees.

```java
public enum Language {
    ENGLISH, CHINESE, SPANISH
}

@WorkflowInterface
public interface GreetingWorkflow {
    @WorkflowMethod
    void run();
    
    @UpdateMethod
    Language setLanguage(Language language);
    
    @UpdateValidatorMethod(updateName = "setLanguage")
    void validateLanguage(Language language);
    
    @UpdateMethod
    Language setLanguageUsingActivity(Language language);
}

public class GreetingWorkflowImpl implements GreetingWorkflow {
    private Language language = Language.ENGLISH;
    private final Map<Language, String> greetings = new HashMap<>();
    
    private final GreetingActivities activities = 
        Workflow.newActivityStub(
            GreetingActivities.class,
            ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(10))
                .build()
        );

    public GreetingWorkflowImpl() {
        greetings.put(Language.ENGLISH, "Hello, world");
    }

    @Override
    public void run() {
        Workflow.await(() -> false); // Run indefinitely
    }

    @Override
    public Language setLanguage(Language language) {
        // Synchronous update - mutates state and returns value
        Language previousLanguage = this.language;
        this.language = language;
        return previousLanguage;
    }

    @Override
    public void validateLanguage(Language language) {
        if (!greetings.containsKey(language)) {
            throw new IllegalArgumentException(
                language.name() + " is not supported"
            );
        }
    }

    @Override
    public Language setLanguageUsingActivity(Language language) {
        // Update with activity - can execute activities
        String greeting = activities.callGreetingService(language);
        if (greeting == null) {
            throw ApplicationFailure.newFailure(
                "Service doesn't support " + language.name(),
                "LanguageNotSupported"
            );
        }

        greetings.put(language, greeting);
        Language previousLanguage = this.language;
        this.language = language;
        return previousLanguage;
    }
}

// Client usage
WorkflowClient client = WorkflowClient.newInstance(service);
GreetingWorkflow workflow = client.newWorkflowStub(
    GreetingWorkflow.class,
    WorkflowOptions.newBuilder()
        .setWorkflowId("greeting-workflow")
        .setTaskQueue("greeting-queue")
        .build()
);

WorkflowClient.start(workflow::run);
Language result = workflow.setLanguage(Language.CHINESE);
```

**Key characteristics:**

- Use `@UpdateMethod` annotation for update methods
- Can return values to clients (unlike signals)
- Support validators with `@UpdateValidatorMethod`
- Can execute activities within updates
- Use `ApplicationFailure` for client-visible failures
- Updates are automatically retried and provide strong consistency

## Retry Policy

Retry policies define how activities and child workflows should be retried when they fail. Temporal provides automatic retry capabilities with configurable backoff strategies, maximum attempts, and timeout settings.

```java
@ActivityInterface
public interface GreetingActivities {
    @ActivityMethod
    String composeGreeting(ComposeGreetingInput input);
}

public class GreetingActivitiesImpl implements GreetingActivities {
    @Override
    public String composeGreeting(ComposeGreetingInput input) {
        int attempt = Activity.getExecutionContext().getInfo().getAttempt();
        System.out.println("Invoking activity, attempt number " + attempt);
        
        // Fail the first 3 attempts, succeed the 4th
        if (attempt < 4) {
            throw new RuntimeException("Intentional failure");
        }
        return input.getGreeting() + ", " + input.getName() + "!";
    }
}

@WorkflowInterface
public interface GreetingWorkflow {
    @WorkflowMethod
    String run(String name);
}

public class GreetingWorkflowImpl implements GreetingWorkflow {
    @Override
    public String run(String name) {
        GreetingActivities activities = Workflow.newActivityStub(
            GreetingActivities.class,
            ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(10))
                .setRetryOptions(RetryOptions.newBuilder()
                    .setMaximumInterval(Duration.ofSeconds(2))
                    .build())
                .build()
        );
        
        return activities.composeGreeting(
            new ComposeGreetingInput("Hello", name)
        );
    }
}
```

**Default retry behavior:**

- Activities retry automatically with exponential backoff
- Initial interval: 1 second
- Backoff coefficient: 2.0 (doubles each retry)
- Maximum interval: 100 × initial interval
- Unlimited attempts and duration by default
- It's very common for Temporal Activity to use the default Retry Policy

**Custom retry policy configuration:**

```java
// Custom retry policy with specific limits
RetryOptions customRetry = RetryOptions.newBuilder()
    .setInitialInterval(Duration.ofSeconds(1))      // First retry after 1s
    .setBackoffCoefficient(2.0)                     // Double interval each retry
    .setMaximumInterval(Duration.ofSeconds(30))     // Cap at 30s between retries
    .setMaximumAttempts(5)                          // Stop after 5 attempts
    .setDoNotRetry("IllegalArgumentException")      // Don't retry these errors
    .build();

GreetingActivities activities = Workflow.newActivityStub(
    GreetingActivities.class,
    ActivityOptions.newBuilder()
        .setStartToCloseTimeout(Duration.ofMinutes(5))
        .setRetryOptions(customRetry)
        .build()
);

activities.myActivity(inputData);
```

**Activity retry information:**

```java
@ActivityInterface
public interface MyActivities {
    @ActivityMethod
    String myActivity();
}

public class MyActivitiesImpl implements MyActivities {
    @Override
    public String myActivity() {
        // Access retry attempt information
        int attempt = Activity.getExecutionContext().getInfo().getAttempt();
        Activity.getExecutionContext().getLogger().info("Attempt #" + attempt);

        // Conditional logic based on attempt
        if (attempt < 3) {
            throw new RuntimeException("Failing attempt " + attempt);
        }

        return "Success!";
    }
}
```

**Key characteristics:**

- Use `RetryOptions` class to configure retry behavior
- Access current attempt via `Activity.getExecutionContext().getInfo().getAttempt()`
- Exponential backoff prevents overwhelming downstream services
- `setDoNotRetry()` for permanent failures
- Applies to both activities and child workflows
- Retry state persists across worker restarts

## Search Attributes

Search Attributes are key-value pairs that enable filtering and searching workflows in Temporal. They're indexed metadata that can be set at workflow start and updated during execution, making workflows discoverable through the Temporal Web UI and programmatic queries.

```java
@WorkflowInterface
public interface GreetingWorkflow {
    @WorkflowMethod
    void run();
}

public class GreetingWorkflowImpl implements GreetingWorkflow {
    @Override
    public void run() {
        // Wait a couple seconds, then alter the search attributes
        Workflow.sleep(Duration.ofSeconds(2));
        Workflow.upsertTypedSearchAttributes(
            SearchAttributeUpdate.valueSet(
                SearchAttributeKey.forKeyword("CustomerId"),
                "customer_2"
            )
        );
    }
}

// Client usage - starting workflow with typed search attributes
WorkflowClient client = WorkflowClient.newInstance(service);
GreetingWorkflow workflow = client.newWorkflowStub(
    GreetingWorkflow.class,
    WorkflowOptions.newBuilder()
        .setWorkflowId("search-attributes-workflow-id")
        .setTaskQueue("search-attributes-task-queue")
        .setTypedSearchAttributes(
            SearchAttributeUpdate.valueSet(
                SearchAttributeKey.forKeyword("CustomerId"),
                "customer_1"
            ),
            SearchAttributeUpdate.valueSet(
                SearchAttributeKey.forText("MiscData"),
                "customer_1_data"
            )
        )
        .build()
);

WorkflowClient.start(workflow::run);

// Reading search attributes from workflow handle
WorkflowExecution execution = WorkflowStub.fromTyped(workflow).getExecution();
WorkflowStub untypedStub = client.newUntypedWorkflowStub(
    execution.getWorkflowId()
);
String customerId = untypedStub.describe()
    .getTypedSearchAttributes()
    .get(SearchAttributeKey.forKeyword("CustomerId"));
System.out.println("Search attribute value: " + customerId);
```

**Creating typed search attribute keys:**

```java
import io.temporal.common.SearchAttributeKey;

// Different types of search attribute keys
SearchAttributeKey<String> customerIdKey = 
    SearchAttributeKey.forKeyword("CustomerId");
SearchAttributeKey<Double> orderValueKey = 
    SearchAttributeKey.forDouble("OrderValue");
SearchAttributeKey<Long> itemCountKey = 
    SearchAttributeKey.forLong("ItemCount");
SearchAttributeKey<Boolean> isPriorityKey = 
    SearchAttributeKey.forBoolean("IsPriority");
SearchAttributeKey<OffsetDateTime> createdDateKey = 
    SearchAttributeKey.forOffsetDateTime("CreatedDate");
SearchAttributeKey<List<String>> tagsKey = 
    SearchAttributeKey.forKeywordList("Tags");
SearchAttributeKey<String> descriptionKey = 
    SearchAttributeKey.forText("Description");
```

**Updating search attributes during workflow execution:**

```java
@WorkflowInterface
public interface OrderWorkflow {
    @WorkflowMethod
    String run(String orderId);
}

public class OrderWorkflowImpl implements OrderWorkflow {
    private final PaymentActivities paymentActivities = 
        Workflow.newActivityStub(
            PaymentActivities.class,
            ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofMinutes(5))
                .build()
        );

    @Override
    public String run(String orderId) {
        // Set initial search attributes using typed API
        Workflow.upsertTypedSearchAttributes(
            SearchAttributeUpdate.valueSet(
                SearchAttributeKey.forKeyword("CustomerId"),
                "customer_1"
            ),
            SearchAttributeUpdate.valueSet(
                SearchAttributeKey.forDouble("OrderValue"),
                100.50
            ),
            SearchAttributeUpdate.valueSet(
                SearchAttributeKey.forLong("ItemCount"),
                3L
            ),
            SearchAttributeUpdate.valueSet(
                SearchAttributeKey.forBoolean("IsPriority"),
                true
            ),
            SearchAttributeUpdate.valueSet(
                SearchAttributeKey.forOffsetDateTime("CreatedDate"),
                OffsetDateTime.now()
            ),
            SearchAttributeUpdate.valueSet(
                SearchAttributeKey.forKeywordList("Tags"),
                Arrays.asList("electronics", "urgent")
            )
        );

        // Process order...
        paymentActivities.processPayment();

        // Update search attributes after processing
        Workflow.upsertTypedSearchAttributes(
            SearchAttributeUpdate.valueSet(
                SearchAttributeKey.forKeyword("CustomerId"),
                "customer_1_processed"
            )
        );

        return "Order completed";
    }
}
```

**Removing search attributes:**

```java
@WorkflowInterface
public interface CleanupWorkflow {
    @WorkflowMethod
    String run();
}

public class CleanupWorkflowImpl implements CleanupWorkflow {
    @Override
    public String run() {
        // Set some initial search attributes
        Workflow.upsertTypedSearchAttributes(
            SearchAttributeUpdate.valueSet(
                SearchAttributeKey.forKeyword("CustomerId"),
                "customer_1"
            ),
            SearchAttributeUpdate.valueSet(
                SearchAttributeKey.forKeywordList("Tags"),
                Arrays.asList("electronics", "urgent")
            ),
            SearchAttributeUpdate.valueSet(
                SearchAttributeKey.forBoolean("IsPriority"),
                true
            )
        );

        // Do some work...
        Workflow.sleep(Duration.ofSeconds(1));

        // Remove specific search attributes by unsetting them
        Workflow.upsertTypedSearchAttributes(
            SearchAttributeUpdate.valueUnset(
                SearchAttributeKey.forKeywordList("Tags")
            ),
            SearchAttributeUpdate.valueUnset(
                SearchAttributeKey.forBoolean("IsPriority")
            )
        );

        // CustomerId remains set, only Tags and IsPriority are removed
        return "Cleanup completed";
    }
}
```

**Querying workflows by search attributes:**

```java
// List workflows with specific search attributes using string queries
WorkflowClient client = WorkflowClient.newInstance(service);

client.listExecutions(
    "WorkflowType='GreetingWorkflow'"
).forEach(workflow -> {
    System.out.println("Workflow: " + workflow.getExecution().getWorkflowId());
});

// Advanced queries with multiple conditions
String query = """
    CustomerId='customer_1' AND
    OrderValue > 50.0 AND
    ItemCount >= 1 AND
    StartTime > '2024-01-01T00:00:00Z'
    """;
    
client.listExecutions(query).forEach(workflow -> {
    System.out.println("Found workflow: " + workflow.getExecution().getWorkflowId());
});

// Query with keyword lists
String keywordQuery = "Tags IN ('electronics', 'urgent')";
client.listExecutions(keywordQuery).forEach(workflow -> {
    System.out.println("Found workflow: " + workflow.getExecution().getWorkflowId());
});
```

**Key characteristics:**

- Use typed search attributes with `SearchAttributeKey` for type safety
- Create search attributes with `SearchAttributeUpdate.valueSet()`
- Use `Workflow.upsertTypedSearchAttributes()` to set/update attributes during execution
- Set initial attributes via `setTypedSearchAttributes()` parameter when starting workflows
- Search attributes are indexed and queryable through Temporal Web UI and CLI
- Support multiple data types: Boolean, Double, Long, Keyword, KeywordList, Text, OffsetDateTime
- Remove attributes using `SearchAttributeUpdate.valueUnset()`
- Enable powerful workflow discovery and monitoring capabilities
- Persist across workflow restarts and are available after completion
