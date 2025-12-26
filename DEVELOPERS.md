# Development Guide

This template comes with opinionated tooling for Temporal development using its Java SDK.

## Table of Contents

- [Build System](#build-system)
  - [Common `./gradlew` Commands](#common-gradlew-commands)
- [Testing](#testing)
  - [Key Features](#key-features)
  - [Common Test Commands](#common-test-commands)
- [Data Serialization](#data-serialization)
  - [Temporal Integration](#temporal-integration)
- [Code Quality](#code-quality)
  - [Common `spotless` Commands](#common-spotless-commands)
  - [Common `checkstyle` Commands](#common-checkstyle-commands)
- [Task Management](#task-management)
- [Continuous Integration](#continuous-integration)
- [Dependency Automation](#dependency-automation)

## Build System

We use **[Gradle](https://gradle.org/)** with the Gradle Wrapper for build management because it provides powerful dependency management, incremental builds for faster compilation, flexible task configuration, and excellent IDE integration. The wrapper ensures consistent Gradle versions across all developers and CI environments.

### Common `./gradlew` Commands

```bash
# Build the project
./gradlew build

# Run the application
./gradlew run

# Install dependencies and compile
./gradlew compileJava

# Clean build artifacts
./gradlew clean

# Add a new dependency (edit build.gradle manually, then sync)
./gradlew --refresh-dependencies

# Show dependency tree
./gradlew dependencies
```

For Maven users:
```bash
# Build the project
./mvnw clean install

# Run tests
./mvnw test

# Run the application
./mvnw exec:java
```

See [Gradle User Guide](https://docs.gradle.org/current/userguide/userguide.html) for the full list of Gradle commands.

## Testing

We use **[JUnit 5](https://junit.org/junit5/)** for testing because it provides excellent support for modern Java features, powerful extension mechanisms (essential for Temporal test environments), and comprehensive assertion libraries. The Temporal Java SDK provides `TestWorkflowExtension` for easy workflow testing.

### Key Features

- **Temporal Testing Support**: `TestWorkflowExtension` enables testing of workflows and activities in a local test environment
- **Parameterized Tests**: `@ParameterizedTest` for testing multiple scenarios
- **Test Coverage**: JaCoCo plugin ensures minimum test coverage requirements
- **Timeout Protection**: `@Timeout` annotation prevents hanging tests
- **Flexible Discovery**: Automatically finds test classes ending with `Test.java`

### Common Test Commands

```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests "workflows.http.HttpWorkflowTest"

# Run tests with verbose output
./gradlew test --info

# Run tests and stop on first failure
./gradlew test --fail-fast

# Run tests matching a pattern
./gradlew test --tests "*Http*"

# Generate coverage report (HTML)
./gradlew test jacocoTestReport
# Report available at: build/reports/jacoco/test/html/index.html

# Check coverage meets minimum threshold
./gradlew test jacocoTestCoverageVerification
```

For Maven users:
```bash
# Run all tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=HttpWorkflowTest

# Generate coverage report
./mvnw jacoco:report
```

See [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/) for the full list of testing features.

## Data Serialization

We use **POJOs (Plain Old Java Objects)** with Jackson for data validation and serialization because it provides flexibility in defining data structures, automatic JSON serialization/deserialization, and seamless integration with Temporal's data converter. This is important for Temporal Workflows because it is strongly recommended to pass objects as input and output to Workflows and Activities.

**Alternative**: For more robust validation, consider using **[Bean Validation (JSR 380)](https://beanvalidation.org/)** with implementations like Hibernate Validator.

### Temporal Integration

Temporal's Java SDK uses Jackson by default for serialization. You can customize the data converter if needed:

```java
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.temporal.client.WorkflowClient;
import io.temporal.common.converter.DefaultDataConverter;
import io.temporal.common.converter.JacksonJsonPayloadConverter;

// Create custom ObjectMapper
ObjectMapper objectMapper = new ObjectMapper()
    .registerModule(new JavaTimeModule())
    .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

// Create custom data converter
DefaultDataConverter dataConverter = DefaultDataConverter.newDefaultInstance()
    .withPayloadConverterOverrides(
        new JacksonJsonPayloadConverter(objectMapper)
    );

// Use custom data converter in client
WorkflowClient client = WorkflowClient.newInstance(
    service,
    WorkflowClientOptions.newBuilder()
        .setDataConverter(dataConverter)
        .build()
);
```

This enables automatic serialization/deserialization of Java objects in Workflow inputs, outputs, and Activity parameters.

**Example POJO with validation:**

```java
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

public class WorkflowInput {
    @NotNull
    @Pattern(regexp = "https?://.*")
    @JsonProperty("url")
    private String url;

    // Default constructor for deserialization
    public WorkflowInput() {}

    public WorkflowInput(String url) {
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

## Code Quality

We use **[Spotless](https://github.com/diffplug/spotless)** for code formatting because it's fast, supports multiple formatters (Google Java Format, Prettier, etc.), and integrates seamlessly with Gradle. It enforces consistent code style across the project and can automatically fix formatting issues.

### Common `spotless` Commands

```bash
# Check formatting without fixing
./gradlew spotlessCheck

# Auto-format all code
./gradlew spotlessApply

# Format specific source set
./gradlew spotlessJavaApply
```

**Configuration example (`build.gradle`):**

```groovy
plugins {
    id 'com.diffplug.spotless' version '6.23.3'
}

spotless {
    java {
        googleJavaFormat('1.17.0')
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
        
        // Exclude generated files
        targetExclude('build/**')
    }
}
```

See [Spotless Documentation](https://github.com/diffplug/spotless) for full configuration options.

### Common `checkstyle` Commands

We use **[Checkstyle](https://checkstyle.sourceforge.io/)** for static code analysis because it enforces coding standards, catches potential bugs early, and ensures consistent code quality across the team.

```bash
# Run checkstyle analysis
./gradlew checkstyleMain checkstyleTest

# View checkstyle report
# Report available at: build/reports/checkstyle/main.html
```

**Configuration example (`build.gradle`):**

```groovy
plugins {
    id 'checkstyle'
}

checkstyle {
    toolVersion = '10.12.5'
    configFile = file("${rootDir}/config/checkstyle/checkstyle.xml")
    ignoreFailures = false
    maxWarnings = 0
}
```

**Git Hooks with Husky-style setup:**

While Java doesn't have a direct equivalent to Python's pre-commit, you can set up Git hooks manually:

```bash
# Create pre-commit hook
cat > .git/hooks/pre-commit << 'EOF'
#!/bin/bash
./gradlew spotlessCheck checkstyleMain
if [ $? -ne 0 ]; then
    echo "Code quality checks failed. Run './gradlew spotlessApply' to fix formatting."
    exit 1
fi
EOF

chmod +x .git/hooks/pre-commit
```

## Task Management

Gradle serves as the primary task runner for Java projects. All common tasks are defined in `build.gradle` and can be executed via `./gradlew`.

**Common custom tasks:**

```groovy
// In build.gradle
tasks.register('format') {
    group = 'formatting'
    description = 'Format code and run checks'
    dependsOn 'spotlessApply'
}

tasks.register('verify') {
    group = 'verification'
    description = 'Run all verification tasks'
    dependsOn 'test', 'checkstyleMain', 'spotlessCheck'
}

tasks.register('setupHooks') {
    group = 'setup'
    description = 'Install Git hooks'
    doLast {
        def preCommitHook = file('.git/hooks/pre-commit')
        preCommitHook.text = '''#!/bin/bash
./gradlew spotlessCheck checkstyleMain
if [ $? -ne 0 ]; then
    echo "Code quality checks failed."
    exit 1
fi
'''
        preCommitHook.setExecutable(true)
        println 'Git hooks installed successfully'
    }
}
```

**Usage:**

```bash
# Format code
./gradlew format

# Run all verifications
./gradlew verify

# Install Git hooks
./gradlew setupHooks

# List all available tasks
./gradlew tasks --all
```

## Continuous Integration

We use **[GitHub Actions](https://docs.github.com/en/actions)** for continuous integration because it provides native integration with GitHub repositories, supports matrix builds across multiple Java versions and operating systems, and offers excellent caching capabilities for faster builds. GitHub Actions is free for public repositories and provides generous limits for private repositories.

**Example GitHub Actions workflow (`.github/workflows/ci.yml`):**

```yaml
name: CI

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main, develop ]

jobs:
  build:
    runs-on: ${{ matrix.os }}
    strategy:
      matrix:
        os: [ubuntu-latest, macos-latest, windows-latest]
        java: [17, 21]
    
    steps:
    - uses: actions/checkout@v4
    
    - name: Set up JDK ${{ matrix.java }}
      uses: actions/setup-java@v4
      with:
        java-version: ${{ matrix.java }}
        distribution: 'temurin'
        cache: 'gradle'
    
    - name: Grant execute permission for gradlew
      run: chmod +x gradlew
      if: runner.os != 'Windows'
    
    - name: Build with Gradle
      run: ./gradlew build
    
    - name: Run tests
      run: ./gradlew test
    
    - name: Check code quality
      run: ./gradlew spotlessCheck checkstyleMain
    
    - name: Generate coverage report
      run: ./gradlew jacocoTestReport
    
    - name: Upload coverage to Codecov
      uses: codecov/codecov-action@v3
      with:
        files: build/reports/jacoco/test/jacocoTestReport.xml
```

To disable the CI pipeline, delete the `.github/workflows/ci.yml` file or rename it with a `.disabled` extension.

## Dependency Automation

We use **[Dependabot](https://docs.github.com/en/code-security/dependabot)** for automated dependency management because it provides proactive security updates, keeps dependencies current with minimal manual effort, and integrates seamlessly with GitHub's security advisory database.

Dependabot automatically creates pull requests for dependency updates, making it easy to review and merge changes while maintaining project security.

**Example Dependabot configuration (`.github/dependabot.yml`):**

```yaml
version: 2
updates:
  # Gradle dependencies
  - package-ecosystem: "gradle"
    directory: "/"
    schedule:
      interval: "weekly"
    open-pull-requests-limit: 10
    reviewers:
      - "your-team"
    labels:
      - "dependencies"
      - "java"
    
  # GitHub Actions
  - package-ecosystem: "github-actions"
    directory: "/"
    schedule:
      interval: "weekly"
    open-pull-requests-limit: 5
```

**Alternative: Renovate Bot**

For more advanced dependency management, consider **[Renovate](https://docs.renovatebot.com/)**, which offers:
- More granular configuration options
- Support for monorepos
- Automatic dependency grouping
- Custom update schedules per dependency

For full documentation on Dependabot configuration, see [Dependabot options reference](https://docs.github.com/en/code-security/dependabot/working-with-dependabot/dependabot-options-reference).

## Additional Tools

### Static Analysis

**SpotBugs** - Find bugs in Java programs:
```bash
./gradlew spotbugsMain
# Report: build/reports/spotbugs/main.html
```

**PMD** - Source code analyzer:
```bash
./gradlew pmdMain
# Report: build/reports/pmd/main.html
```

### Documentation

**Javadoc** - Generate API documentation:
```bash
./gradlew javadoc
# Output: build/docs/javadoc/index.html
```

### Dependency Analysis

**Gradle Versions Plugin** - Check for dependency updates:
```bash
./gradlew dependencyUpdates
```

### IDE Integration

Most Java IDEs (IntelliJ IDEA, Eclipse, VS Code with Java extensions) automatically detect and import Gradle projects. Simply open the project directory and the IDE will configure itself based on `build.gradle`.

**IntelliJ IDEA**: File → Open → Select project directory
**VS Code**: Install "Extension Pack for Java" and open the project folder
**Eclipse**: File → Import → Existing Gradle Project
