# Example Prompts for Agents to Develop Workflows

This document provides example prompts that can be used to guide AI agents in developing Temporal workflows using the Java SDK.

## Table of Contents

- [Text-based Prompt](#text-based-prompt)
- [Multi-modal Prompt](#multi-modal-prompt)
- [Usage Guidelines](#usage-guidelines)

## Text-based Prompt

```text
Implement an order Workflow with the following steps:

1. check fraud
1. prepare shipment
1. charge customer
1. ship order

Use the Temporal Java SDK following these requirements:
- Create a workflow interface annotated with @WorkflowInterface
- Create a workflow implementation class
- Create an activities interface annotated with @ActivityInterface
- Create an activities implementation class
- Define POJOs for input/output data
- Use proper ActivityOptions with setStartToCloseTimeout
- Follow Java naming conventions (OrderWorkflow, OrderWorkflowImpl, OrderActivities, OrderActivitiesImpl)
```

[![Order Workflow with Cursor](https://img.youtube.com/vi/ePbdiPNsgv4/maxresdefault.jpg)](https://youtu.be/ePbdiPNsgv4)
*Video 1: generate order Workflow using Cursor (demonstrates workflow development concepts in Python, but applicable to Java)*

### Expected Structure

```
workflows/order/
├── OrderWorkflow.java           # Workflow interface
├── OrderWorkflowImpl.java       # Workflow implementation
├── OrderActivities.java         # Activities interface
├── OrderActivitiesImpl.java     # Activities implementation
├── OrderWorkflowInput.java      # Input POJO
├── OrderWorkflowOutput.java     # Output POJO
├── OrderWorker.java             # Worker class
└── OrderWorkflowTest.java       # Test class (in src/test/java)
```

## Multi-modal Prompt

```text
# attach-your-workflow-diagram-as-context
Analyze the provided diagram and convert it into a Temporal Workflow Definition using the Java SDK.

Requirements:
- Create workflow interface with @WorkflowInterface annotation
- Implement workflow class with deterministic logic
- Create activities interface with @ActivityInterface annotation
- Implement activities with proper logging
- Define POJOs for all inputs and outputs with proper getters/setters
- Use Workflow.newActivityStub() with appropriate ActivityOptions
- Set reasonable timeout values (e.g., Duration.ofSeconds(30))
- Include a Worker class with main() method for running the workflow
- Add JUnit 5 tests using TestWorkflowExtension

Follow the implementation standards defined in write-new-workflow.md and DEVELOPERS.md
```

[![Employee Anniversary Workflow with Warp Code](https://img.youtube.com/vi/pgRWSEM7bn4/maxresdefault.jpg)](https://youtu.be/pgRWSEM7bn4)
*Video 2: generate employee anniversary Workflow using Warp Code (demonstrates workflow development concepts in Python, but applicable to Java)*

## Usage Guidelines

When using these prompts:

1. **Start Simple**: Begin with simple workflows before iterating on more complex patterns
1. **Follow Project Patterns**: Use the existing project structure and conventions from `write-new-workflow.md`
1. **Use Java Conventions**: 
   - Interface/Implementation pattern for workflows and activities
   - POJOs with default constructors for serialization
   - Proper package structure (`workflows.<domain>`)
   - File naming: `<Name>Workflow.java`, `<Name>WorkflowImpl.java`, `<Name>Activities.java`, `<Name>ActivitiesImpl.java`
1. **Test Thoroughly**: Implement comprehensive tests using JUnit 5 and `TestWorkflowExtension`
1. **Code Quality**: Ensure code passes Spotless formatting and Checkstyle validation
1. **Document**: Add Javadoc comments for public interfaces and methods

## Additional Prompt Examples

### For Signal and Query Patterns

```text
Implement a document approval Workflow with the following features:

1. Submit document for review (via workflow start)
2. Allow approvers to approve/reject via Signal
3. Allow querying current approval status
4. Send reminder activities if no response within 24 hours
5. Complete workflow once all approvals received or any rejection

Requirements:
- Use @SignalMethod for approval actions
- Use @QueryMethod for status checks
- Use Workflow.await() for event-driven waiting
- Implement timeout logic with Workflow.sleep()
```

### For Update Pattern

```text
Implement a subscription management Workflow with the following features:

1. Create subscription with initial plan
2. Allow upgrading/downgrading plan via Update (return previous plan)
3. Validate plan transitions in Update validator
4. Execute billing activity on plan changes
5. Query current subscription details

Requirements:
- Use @UpdateMethod for plan changes
- Use @UpdateValidatorMethod for validation
- Use @QueryMethod for subscription details
- Return values from Update methods
- Use ApplicationFailure for validation errors
```

### For Long-Running Workflows

```text
Implement a customer onboarding Workflow that:

1. Sends welcome email
2. Waits up to 7 days for email verification (Signal)
3. If verified, create account and send confirmation
4. If not verified, send reminder emails every 2 days
5. Cancel onboarding after 7 days if not verified

Requirements:
- Use Workflow.await() with timeout for verification
- Use Workflow.sleep() for reminder timing
- Use Signal for verification event
- Implement graceful cancellation handling
- Track state with private workflow fields
```

## Common Pitfalls to Avoid

When prompting agents, remind them to avoid:

1. ❌ Using `Thread.sleep()` instead of `Workflow.sleep()`
1. ❌ Direct I/O operations in workflow code (use Activities instead)
1. ❌ Missing default constructors in POJOs
1. ❌ Not setting `setStartToCloseTimeout()` on activities
1. ❌ Using non-deterministic operations in workflows (random, system time, etc.)
1. ❌ Forgetting to register workflows and activities with the Worker
1. ❌ Not handling serialization/deserialization properly

## Verification Checklist

After generating workflow code, ensure:

- [ ] Workflow interface has `@WorkflowInterface` annotation
- [ ] Workflow method has `@WorkflowMethod` annotation
- [ ] Activities interface has `@ActivityInterface` annotation
- [ ] Activity methods have `@ActivityMethod` annotation
- [ ] All POJOs have default constructors
- [ ] ActivityOptions include `setStartToCloseTimeout()`
- [ ] Worker registers both workflow and activity implementations
- [ ] Tests use `TestWorkflowExtension`
- [ ] Code follows naming conventions
- [ ] No non-deterministic operations in workflow code
