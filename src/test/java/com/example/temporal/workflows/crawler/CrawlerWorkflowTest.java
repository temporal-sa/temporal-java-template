package com.example.temporal.workflows.crawler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for CrawlerWorkflow.
 *
 * <p>These tests use TestWorkflowEnvironment to test the workflow with mocked activities.
 */
class CrawlerWorkflowTest {

  private TestWorkflowEnvironment testEnv;
  private Worker worker;
  private WorkflowClient client;

  @BeforeEach
  void setUp() {
    testEnv = TestWorkflowEnvironment.newInstance();
    worker = testEnv.newWorker(CrawlerWorker.TASK_QUEUE);
    worker.registerWorkflowImplementationTypes(CrawlerWorkflowImpl.class);
    client = testEnv.getWorkflowClient();
  }

  @AfterEach
  void tearDown() {
    testEnv.close();
  }

  @Test
  void testCrawlerWorkflow_SinglePage() {
    // Arrange
    CrawlerActivities mockActivities = mock(CrawlerActivities.class);
    worker.registerActivitiesImplementations(mockActivities);

    String startUrl = "https://example.com";
    ParseLinksOutput emptyOutput = new ParseLinksOutput(List.of());
    when(mockActivities.parseLinksFromUrl(any(ParseLinksInput.class))).thenReturn(emptyOutput);

    testEnv.start();

    // Act
    CrawlerWorkflow workflow =
        client.newWorkflowStub(
            CrawlerWorkflow.class,
            WorkflowOptions.newBuilder().setTaskQueue(CrawlerWorker.TASK_QUEUE).build());

    CrawlerWorkflowInput input = new CrawlerWorkflowInput(startUrl, 5);
    CrawlerWorkflowOutput output = workflow.run(input);

    // Assert
    assertNotNull(output);
    assertEquals(1, output.totalLinksCrawled());
    assertTrue(output.linksDiscovered().contains(startUrl));
    assertEquals(1, output.domainsDiscovered().size());
    assertTrue(output.domainsDiscovered().contains("example.com"));
  }

  @Test
  void testCrawlerWorkflow_MultiplePages() {
    // Arrange
    CrawlerActivities mockActivities = mock(CrawlerActivities.class);
    worker.registerActivitiesImplementations(mockActivities);

    String startUrl = "https://example.com/page1";
    String link2 = "https://example.com/page2";
    String link3 = "https://example.com/page3";

    // First call returns 2 links
    when(mockActivities.parseLinksFromUrl(new ParseLinksInput(startUrl)))
        .thenReturn(new ParseLinksOutput(List.of(link2, link3)));

    // Subsequent calls return empty
    when(mockActivities.parseLinksFromUrl(new ParseLinksInput(link2)))
        .thenReturn(new ParseLinksOutput(List.of()));
    when(mockActivities.parseLinksFromUrl(new ParseLinksInput(link3)))
        .thenReturn(new ParseLinksOutput(List.of()));

    testEnv.start();

    // Act
    CrawlerWorkflow workflow =
        client.newWorkflowStub(
            CrawlerWorkflow.class,
            WorkflowOptions.newBuilder().setTaskQueue(CrawlerWorker.TASK_QUEUE).build());

    CrawlerWorkflowInput input = new CrawlerWorkflowInput(startUrl, 10);
    CrawlerWorkflowOutput output = workflow.run(input);

    // Assert
    assertNotNull(output);
    assertEquals(3, output.totalLinksCrawled());
    assertEquals(3, output.linksDiscovered().size());
    assertTrue(output.linksDiscovered().contains(startUrl));
    assertTrue(output.linksDiscovered().contains(link2));
    assertTrue(output.linksDiscovered().contains(link3));
  }

  @Test
  void testCrawlerWorkflow_RespectsMaxLinks() {
    // Arrange
    CrawlerActivities mockActivities = mock(CrawlerActivities.class);
    worker.registerActivitiesImplementations(mockActivities);

    String startUrl = "https://example.com/page1";
    List<String> manyLinks =
        List.of(
            "https://example.com/page2",
            "https://example.com/page3",
            "https://example.com/page4",
            "https://example.com/page5",
            "https://example.com/page6");

    when(mockActivities.parseLinksFromUrl(any(ParseLinksInput.class)))
        .thenReturn(new ParseLinksOutput(manyLinks));

    testEnv.start();

    // Act
    CrawlerWorkflow workflow =
        client.newWorkflowStub(
            CrawlerWorkflow.class,
            WorkflowOptions.newBuilder().setTaskQueue(CrawlerWorker.TASK_QUEUE).build());

    // Set maxLinks to 3
    CrawlerWorkflowInput input = new CrawlerWorkflowInput(startUrl, 3);
    CrawlerWorkflowOutput output = workflow.run(input);

    // Assert
    assertNotNull(output);
    assertEquals(3, output.totalLinksCrawled(), "Should respect maxLinks limit");
    assertTrue(output.linksDiscovered().size() >= 3, "Should discover at least maxLinks");
  }

  @Test
  void testCrawlerWorkflow_EliminatesDuplicates() {
    // Arrange
    CrawlerActivities mockActivities = mock(CrawlerActivities.class);
    worker.registerActivitiesImplementations(mockActivities);

    String startUrl = "https://example.com/page1";
    String link2 = "https://example.com/page2";

    // First page links to page2
    when(mockActivities.parseLinksFromUrl(new ParseLinksInput(startUrl)))
        .thenReturn(new ParseLinksOutput(List.of(link2)));

    // Page2 links back to page1 (circular reference)
    when(mockActivities.parseLinksFromUrl(new ParseLinksInput(link2)))
        .thenReturn(new ParseLinksOutput(List.of(startUrl)));

    testEnv.start();

    // Act
    CrawlerWorkflow workflow =
        client.newWorkflowStub(
            CrawlerWorkflow.class,
            WorkflowOptions.newBuilder().setTaskQueue(CrawlerWorker.TASK_QUEUE).build());

    CrawlerWorkflowInput input = new CrawlerWorkflowInput(startUrl, 10);
    CrawlerWorkflowOutput output = workflow.run(input);

    // Assert
    assertNotNull(output);
    assertEquals(2, output.totalLinksCrawled(), "Should only crawl each URL once");
    assertEquals(2, output.linksDiscovered().size());
    assertTrue(output.linksDiscovered().contains(startUrl));
    assertTrue(output.linksDiscovered().contains(link2));
  }

  @Test
  void testCrawlerWorkflow_CrossDomain() {
    // Arrange
    CrawlerActivities mockActivities = mock(CrawlerActivities.class);
    worker.registerActivitiesImplementations(mockActivities);

    String startUrl = "https://example.com/page1";
    String externalLink = "https://another-site.com/page";

    when(mockActivities.parseLinksFromUrl(new ParseLinksInput(startUrl)))
        .thenReturn(new ParseLinksOutput(List.of(externalLink)));

    when(mockActivities.parseLinksFromUrl(new ParseLinksInput(externalLink)))
        .thenReturn(new ParseLinksOutput(List.of()));

    testEnv.start();

    // Act
    CrawlerWorkflow workflow =
        client.newWorkflowStub(
            CrawlerWorkflow.class,
            WorkflowOptions.newBuilder().setTaskQueue(CrawlerWorker.TASK_QUEUE).build());

    CrawlerWorkflowInput input = new CrawlerWorkflowInput(startUrl, 10);
    CrawlerWorkflowOutput output = workflow.run(input);

    // Assert
    assertNotNull(output);
    assertEquals(2, output.domainsDiscovered().size(), "Should discover 2 domains");
    assertTrue(output.domainsDiscovered().contains("example.com"));
    assertTrue(output.domainsDiscovered().contains("another-site.com"));
  }

  @Test
  void testCrawlerWorkflow_EmptyWebsite() {
    // Arrange
    CrawlerActivities mockActivities = mock(CrawlerActivities.class);
    worker.registerActivitiesImplementations(mockActivities);

    String startUrl = "https://example.com";
    when(mockActivities.parseLinksFromUrl(any(ParseLinksInput.class)))
        .thenReturn(new ParseLinksOutput(List.of()));

    testEnv.start();

    // Act
    CrawlerWorkflow workflow =
        client.newWorkflowStub(
            CrawlerWorkflow.class,
            WorkflowOptions.newBuilder().setTaskQueue(CrawlerWorker.TASK_QUEUE).build());

    CrawlerWorkflowInput input = new CrawlerWorkflowInput(startUrl, 10);
    CrawlerWorkflowOutput output = workflow.run(input);

    // Assert
    assertNotNull(output);
    assertEquals(1, output.totalLinksCrawled());
    assertEquals(1, output.linksDiscovered().size());
    assertTrue(output.linksDiscovered().contains(startUrl));
  }

  @Test
  void testCrawlerWorkflow_DefaultMaxLinks() {
    // Arrange
    CrawlerActivities mockActivities = mock(CrawlerActivities.class);
    worker.registerActivitiesImplementations(mockActivities);

    String startUrl = "https://example.com";
    when(mockActivities.parseLinksFromUrl(any(ParseLinksInput.class)))
        .thenReturn(new ParseLinksOutput(List.of()));

    testEnv.start();

    // Act
    CrawlerWorkflow workflow =
        client.newWorkflowStub(
            CrawlerWorkflow.class,
            WorkflowOptions.newBuilder().setTaskQueue(CrawlerWorker.TASK_QUEUE).build());

    // Use constructor with default maxLinks
    CrawlerWorkflowInput input = new CrawlerWorkflowInput(startUrl);
    CrawlerWorkflowOutput output = workflow.run(input);

    // Assert
    assertNotNull(output);
    assertTrue(output.totalLinksCrawled() <= 10, "Default maxLinks should be 10");
  }
}
