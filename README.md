# Temporal Java Template

![GitHub CI](https://github.com/temporal-sa/temporal-java-template/actions/workflows/ci.yml/badge.svg)
[![Code Coverage](https://img.shields.io/codecov/c/github/temporal-sa/temporal-java-template.svg?maxAge=86400)](https://codecov.io/github/temporal-sa/temporal-java-template?branch=main)
[![GitHub License](https://img.shields.io/github/license/temporal-sa/temporal-java-template)](https://github.com/temporal-sa/temporal-java-template/blob/main/LICENSE)

## Introduction

A modern, production-ready template for building Temporal applications using [Temporal Java SDK](https://docs.temporal.io/dev-guide/java). This template provides a solid foundation for developing Workflow-based applications with comprehensive testing, code quality tools, and modern Java tooling.

### What's Included

- Complete testing setup (JUnit 5, Mockito) with `TestWorkflowEnvironment`
- Pre-configured development tooling (Checkstyle, SpotBugs, Spotless) and CI (GitHub Actions)
- Comprehensive documentation and guides
- [AGENTS.md](AGENTS.md) to provide context and instructions to help AI coding agents work on your project

## Getting Started

### Prerequisites

- Java 21 or higher
- Gradle 8.x (wrapper included)
- [Temporal CLI](https://docs.temporal.io/cli#install)

### Quick Start

1. **Clone and setup the project:**

   ```bash
   git clone https://github.com/temporal-sa/temporal-java-template.git
   cd temporal-java-template
   ./gradlew build
   ```

2. **Install development hooks:**

   ```bash
   pre-commit install
   ```

3. **Run tests:**

   ```bash
   ./gradlew test
   ```

4. **Start Temporal Server**:

   ```bash
   temporal server start-dev
   ```

5. **Run the example workflow** (in a separate terminal):

   ```bash
   # Start the HTTP worker
   ./gradlew runHttpWorker

   # In another terminal, execute a workflow
   temporal workflow start \
     --task-queue http-task-queue \
     --type HttpWorkflow \
     --input '{"url":"https://example.com"}' \
     --workflow-id http-example-1
   ```

### Next Steps

- Check out some example prompts to generate Temporal Workflows using your favorite AI tool.
- After you have built your first Temporal Workflow, read [DEVELOPERS-java.md](./DEVELOPERS-java.md) to learn about development tips & tricks using this template.
- See [`docs/temporal-patterns.md`](./docs/temporal-patterns.md) for advanced Temporal patterns
- Check [`docs/testing.md`](./docs/testing.md) for Temporal testing best practices

## Workflows

### HTTP Workflow

Demonstrates a simple workflow that executes an HTTP GET request through an activity.

**Key Features:**
- 3-second activity timeout
- RestTemplate-based HTTP client
- Error handling and logging
- Spring Boot integration support

**Task Queue:** `http-task-queue`

### Crawler Workflow

Demonstrates a complex workflow with parallel execution and stateful logic.

**Key Features:**
- Breadth-first crawling algorithm
- Parallel URL fetching (up to 10 concurrent)
- Configurable max links limit (default: 10)
- Domain tracking across crawled content
- Duplicate elimination
- 10-second activity timeout
- 16 concurrent activity workers

**Task Queue:** `crawler-task-queue`

## Development

### Running Tests

```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests HttpWorkflowTest

# Run specific test method
./gradlew test --tests CrawlerActivitiesTest.testLinkExtraction_StandardLinks

# Generate coverage report
./gradlew test jacocoTestReport
# View at: build/reports/jacoco/test/html/index.html
```

### Code Quality

```bash
# Format code
./gradlew spotlessApply

# Check code style
./gradlew checkstyleMain checkstyleTest

# Run static analysis
./gradlew spotbugsMain

# Run all quality checks
./gradlew qualityCheck
```

### Build

```bash
# Full build with all checks
./gradlew build

# Clean build
./gradlew clean build

# Build without tests
./gradlew build -x test
```

## Configuration

### Temporal Connection

Edit `src/main/resources/application.yml`:

```yaml
temporal:
  service-address: localhost:7233  # Temporal server address
  namespace: default               # Temporal namespace
```

### Temporal Cloud (mTLS)

For production deployments using Temporal Cloud, the application automatically detects and configures mTLS when certificate paths are provided via environment variables.

**Set environment variables:**

```bash
export TEMPORAL_SERVICE_ADDRESS=<namespace>.<account-id>.tmprl.cloud:7233
export TEMPORAL_NAMESPACE=<your-namespace>
export TEMPORAL_CERT_PATH=/path/to/client.pem
export TEMPORAL_KEY_PATH=/path/to/client.key
```

**Run workers with cloud configuration:**

```bash
./gradlew runHttpWorker
./gradlew runCrawlerWorker
```

The application will automatically:
- Detect the presence of `TEMPORAL_CERT_PATH` and `TEMPORAL_KEY_PATH`
- Configure mTLS for Temporal Cloud connection
- Fall back to local connection if certificates are not provided

**Note**: For local development, no configuration is needed. The application defaults to `localhost:7233` with namespace `default`.

### Worker Configuration

Workers are configured in their respective `*Worker.java` files:

- **HTTP Worker**: Standard worker configuration
- **Crawler Worker**: 16 concurrent activity execution threads for parallel crawling

## Testing

This template includes comprehensive tests demonstrating:

- **Unit Testing**: Activities with mocked dependencies (Mockito)
- **Integration Testing**: Workflows with `TestWorkflowEnvironment`
- **Time-Skipping**: Testing long-running workflows without waiting
- **Activity Mocking**: Isolating workflow logic from activity implementations

See [docs/testing.md](docs/testing.md) for detailed testing patterns.

## Temporal Patterns

This template demonstrates several Temporal best practices:

- **Deterministic Workflows**: Using `Workflow.getLogger()` and avoiding non-deterministic operations
- **Activity Timeouts**: Configurable timeouts for fault tolerance
- **Parallel Execution**: Using `Async.function()` for concurrent activity execution
- **Stateful Workflows**: Managing collections and state across workflow execution
- **Data Models**: Immutable records for type-safe workflow inputs/outputs

See [docs/temporal-patterns.md](docs/temporal-patterns.md) for detailed patterns.

## Tech Stack

- **Java 21**: LTS version with modern language features
- **Spring Boot 3.3.6**: Application framework and dependency injection
- **Temporal SDK 1.25.2**: Workflow orchestration
- **Gradle 8.11.1**: Build automation
- **JUnit 5**: Testing framework
- **Mockito**: Mocking framework
- **RestTemplate**: HTTP client

## Code Quality Tools

- **Checkstyle**: Google Java Style (customized)
- **SpotBugs**: Static analysis for bug detection
- **Spotless**: Code formatting (Google Java Format)
- **JaCoCo**: Code coverage reporting (80% minimum target)

## License

[MIT License](LICENSE).
