# Testing Temporal Workflows in Java

This guide covers testing patterns and best practices for Temporal workflows in Java.

## Table of Contents

- [Testing Overview](#testing-overview)
- [Unit Testing Activities](#unit-testing-activities)
- [Integration Testing Workflows](#integration-testing-workflows)
- [Test Configuration](#test-configuration)
- [Time-Skipping Tests](#time-skipping-tests)
- [Mocking Patterns](#mocking-patterns)
- [Coverage Requirements](#coverage-requirements)
- [Running Tests](#running-tests)

## Testing Overview

This template uses a layered testing approach:

1. **Unit Tests** - Test activities in isolation with mocked dependencies
2. **Integration Tests** - Test workflows end-to-end with mocked activities
3. **Coverage** - Minimum 80% code coverage enforced by JaCoCo

### Test Framework Stack

- **JUnit 5** - Test framework
- **Mockito** - Mocking framework for dependencies
- **Temporal TestWorkflowEnvironment** - In-memory workflow testing
- **JaCoCo** - Code coverage reporting

## Unit Testing Activities

Activities should be tested in isolation with mocked dependencies.

### Example: Testing HTTP Activity

```java
@ExtendWith(MockitoExtension.class)
class HttpActivitiesTest {

    @Mock
    private RestTemplate restTemplate;

    private HttpActivitiesImpl activities;

    @BeforeEach
    void setUp() {
        activities = new HttpActivitiesImpl(restTemplate);
    }

    @Test
    void testHttpGet_Success() {
        // Arrange
        String url = "https://example.com";
        String responseBody = "<html>Content</html>";
        ResponseEntity<String> responseEntity =
            new ResponseEntity<>(responseBody, HttpStatus.OK);

        when(restTemplate.getForEntity(url, String.class))
            .thenReturn(responseEntity);

        // Act
        HttpGetActivityInput input = new HttpGetActivityInput(url);
        HttpGetActivityOutput output = activities.httpGet(input);

        // Assert
        assertNotNull(output);
        assertEquals(responseBody, output.responseText());
        assertEquals(200, output.statusCode());
    }

    @Test
    void testHttpGet_NetworkError() {
        // Arrange
        String url = "https://invalid.com";
        when(restTemplate.getForEntity(url, String.class))
            .thenThrow(new RestClientException("Connection refused"));

        // Act & Assert
        HttpGetActivityInput input = new HttpGetActivityInput(url);
        assertThrows(RestClientException.class,
            () -> activities.httpGet(input));
    }
}
```

### Key Patterns for Activity Tests

1. **Use @ExtendWith(MockitoExtension.class)** for Mockito support
2. **Mock external dependencies** (HTTP clients, databases, etc.)
3. **Test success paths** and error conditions
4. **Verify behavior** with assertions
5. **Test edge cases** (null responses, empty data, etc.)

## Integration Testing Workflows

Workflows should be tested end-to-end using `TestWorkflowEnvironment`.

### Example: Testing HTTP Workflow

```java
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
        // Arrange: Mock activities
        HttpActivities mockActivities = mock(HttpActivities.class);
        worker.registerActivitiesImplementations(mockActivities);

        String testUrl = "https://example.com";
        HttpGetActivityOutput activityOutput =
            new HttpGetActivityOutput("Response", 200);
        when(mockActivities.httpGet(any(HttpGetActivityInput.class)))
            .thenReturn(activityOutput);

        testEnv.start();

        // Act: Execute workflow
        HttpWorkflow workflow = client.newWorkflowStub(
            HttpWorkflow.class,
            WorkflowOptions.newBuilder()
                .setTaskQueue(HttpWorker.TASK_QUEUE)
                .build());

        HttpWorkflowInput input = new HttpWorkflowInput(testUrl);
        HttpWorkflowOutput output = workflow.run(input);

        // Assert
        assertNotNull(output);
        assertEquals("Response", output.responseText());
        assertEquals(testUrl, output.url());
        assertEquals(200, output.statusCode());

        // Verify activity was called
        verify(mockActivities).httpGet(any(HttpGetActivityInput.class));
    }
}
```

### Key Patterns for Workflow Tests

1. **Create TestWorkflowEnvironment** in @BeforeEach
2. **Register workflow implementations** to the worker
3. **Mock activities** to isolate workflow logic
4. **Start test environment** before execution
5. **Close environment** in @AfterEach
6. **Verify activity interactions** with Mockito

## Test Configuration

### TestConfig.java

Create a test configuration for reusable test beans:

```java
@TestConfiguration
public class TestConfig {

    @Bean
    public TestWorkflowEnvironment testWorkflowEnvironment() {
        return TestWorkflowEnvironment.newInstance();
    }

    @Bean
    public WorkflowClient testWorkflowClient(TestWorkflowEnvironment testEnv) {
        return testEnv.getWorkflowClient();
    }
}
```

### Using Spring Test Context

For tests that need Spring context:

```java
@SpringBootTest
@Import(TestConfig.class)
class MyIntegrationTest {

    @Autowired
    private TestWorkflowEnvironment testEnv;

    @Autowired
    private WorkflowClient client;

    // Tests...
}
```

## Time-Skipping Tests

TestWorkflowEnvironment supports time-skipping for testing workflows with timers.

### Example: Testing Workflow with Sleep

```java
@Test
void testWorkflowWithDelay() {
    // Register workflow and activities
    worker.registerWorkflowImplementationTypes(MyWorkflowImpl.class);
    worker.registerActivitiesImplementations(mockActivities);

    testEnv.start();

    // Execute workflow asynchronously
    WorkflowClient.start(workflow::run, input);

    // Skip forward in time (no actual waiting)
    testEnv.sleep(Duration.ofHours(1));

    // Verify workflow continued after sleep
    String result = workflow.run(input);
    assertEquals("expected", result);
}
```

### Testing Retry Behavior

Time-skipping also works with retry intervals:

```java
@Test
void testActivityRetry() {
    // Configure activity to fail then succeed
    when(mockActivities.unstableOperation())
        .thenThrow(new RuntimeException("Temporary failure"))
        .thenReturn("Success");

    testEnv.start();

    // Execute workflow - will retry activity
    String result = workflow.run();

    // Verify retry happened
    verify(mockActivities, times(2)).unstableOperation();
    assertEquals("Success", result);
}
```

## Mocking Patterns

### Mocking Activities with Different Behaviors

```java
@Test
void testCrawlerWorkflow_MultiplePages() {
    CrawlerActivities mockActivities = mock(CrawlerActivities.class);

    // First call returns 2 links
    when(mockActivities.parseLinksFromUrl(
            new ParseLinksInput("https://page1.com")))
        .thenReturn(new ParseLinksOutput(
            List.of("https://page2.com", "https://page3.com")));

    // Subsequent calls return empty
    when(mockActivities.parseLinksFromUrl(
            new ParseLinksInput("https://page2.com")))
        .thenReturn(new ParseLinksOutput(List.of()));

    when(mockActivities.parseLinksFromUrl(
            new ParseLinksInput("https://page3.com")))
        .thenReturn(new ParseLinksOutput(List.of()));

    worker.registerActivitiesImplementations(mockActivities);
    testEnv.start();

    // Test workflow behavior with multiple pages
    CrawlerWorkflowOutput output = workflow.run(input);
    assertEquals(3, output.totalLinksCrawled());
}
```

### Using ArgumentMatchers

```java
// Match any input
when(mockActivities.process(any(Input.class)))
    .thenReturn(new Output("result"));

// Match specific values
when(mockActivities.process(argThat(input ->
    input.url().contains("example.com"))))
    .thenReturn(new Output("example result"));

// Verify with matchers
verify(mockActivities).process(argThat(input ->
    input.retries() > 0));
```

### Capturing Arguments

```java
@Test
void testActivityArguments() {
    ArgumentCaptor<ParseLinksInput> captor =
        ArgumentCaptor.forClass(ParseLinksInput.class);

    workflow.run(input);

    verify(mockActivities, atLeastOnce())
        .parseLinksFromUrl(captor.capture());

    List<ParseLinksInput> allInputs = captor.getAllValues();
    assertTrue(allInputs.stream()
        .anyMatch(i -> i.url().contains("example.com")));
}
```

## Coverage Requirements

### JaCoCo Configuration

This template enforces 80% minimum coverage:

```groovy
jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = 0.80
            }
        }
    }
}
```

### Viewing Coverage Reports

```bash
# Generate coverage report
./gradlew test jacocoTestReport

# View HTML report
open build/reports/jacoco/test/html/index.html
```

### Coverage by Package

The report shows coverage by:
- **Instructions** - Bytecode instructions executed
- **Branches** - Decision points (if/else, switch, etc.)
- **Lines** - Source code lines
- **Methods** - Method coverage
- **Classes** - Class coverage

### Improving Coverage

Focus on:
1. **Test all public methods** in activities and workflows
2. **Test error paths** and exception handling
3. **Test edge cases** (empty inputs, null values, etc.)
4. **Test conditional logic** (all branches)
5. **Test different input combinations**

## Running Tests

### Command Line

```bash
# Run all tests
./gradlew test

# Run tests for specific package
./gradlew test --tests "com.example.temporal.workflows.http.*"

# Run specific test class
./gradlew test --tests HttpWorkflowTest

# Run specific test method
./gradlew test --tests HttpWorkflowTest.testHttpWorkflow_Success

# Run with coverage
./gradlew test jacocoTestReport

# Continuous testing (watch mode)
./gradlew test --continuous
```

### IDE Integration

**IntelliJ IDEA:**
- Right-click test class/method → Run
- Run with coverage → Run with Coverage
- View coverage in editor gutter

**VS Code:**
- Install "Test Runner for Java" extension
- Click run/debug above test methods
- View coverage with "Coverage Gutters" extension

## Test Organization

### File Structure

```
src/test/java/com/example/temporal/
├── TestConfig.java                      # Shared test configuration
└── workflows/
    ├── http/
    │   ├── HttpActivitiesTest.java     # Activity unit tests
    │   └── HttpWorkflowTest.java       # Workflow integration tests
    └── crawler/
        ├── CrawlerActivitiesTest.java  # Activity unit tests
        └── CrawlerWorkflowTest.java    # Workflow integration tests
```

### Naming Conventions

- **Test Classes**: `<ClassName>Test.java`
- **Test Methods**: `test<Method>_<Scenario>()` (underscores allowed)
- **Mocks**: `mock<Type>` (e.g., `mockActivities`)

## Best Practices

### DO ✅

- Test both success and failure scenarios
- Mock external dependencies (HTTP, DB, etc.)
- Use TestWorkflowEnvironment for workflow tests
- Verify activity interactions with `verify()`
- Clean up resources in @AfterEach
- Use meaningful test method names
- Test edge cases and boundary conditions
- Maintain 80%+ code coverage

### DON'T ❌

- Test with real external services
- Skip cleanup in @AfterEach
- Test implementation details
- Write tests that depend on execution order
- Ignore test failures in CI
- Mock everything (test real workflow logic)
- Write tests without assertions

## Debugging Tests

### Enable Logging

```yaml
# src/test/resources/application-test.yml
logging:
  level:
    io.temporal: DEBUG
    com.example.temporal: DEBUG
```

### Print Workflow History

```java
@Test
void debugWorkflow() {
    workflow.run(input);

    // Print workflow history for debugging
    System.out.println(
        testEnv.getWorkflowClient()
            .fetchHistory("workflow-id")
            .getHistory());
}
```

### Step Through Tests

Use IDE debugger to:
1. Set breakpoints in test methods
2. Step through workflow execution
3. Inspect workflow state
4. Examine activity calls

## Additional Resources

- [Temporal Java Testing Documentation](https://docs.temporal.io/dev-guide/java/testing)
- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [Mockito Documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
- [JaCoCo Documentation](https://www.jacoco.org/jacoco/trunk/doc/)
