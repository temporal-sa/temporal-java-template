package com.example.temporal.workflows.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Unit tests for HttpActivitiesImpl.
 *
 * <p>These tests use Mockito to mock RestTemplate and verify activity behavior.
 */
@ExtendWith(MockitoExtension.class)
class HttpActivitiesTest {

  @Mock private RestTemplate restTemplate;

  private HttpActivitiesImpl activities;

  @BeforeEach
  void setUp() {
    activities = new HttpActivitiesImpl(restTemplate);
  }

  @Test
  void testHttpGet_Success() {
    // Arrange
    String url = "https://example.com";
    String responseBody = "<html><body>Hello World</body></html>";
    ResponseEntity<String> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.OK);

    when(restTemplate.getForEntity(url, String.class)).thenReturn(responseEntity);

    // Act
    HttpGetActivityInput input = new HttpGetActivityInput(url);
    HttpGetActivityOutput output = activities.httpGet(input);

    // Assert
    assertNotNull(output);
    assertEquals(responseBody, output.responseText());
    assertEquals(200, output.statusCode());
  }

  @Test
  void testHttpGet_EmptyResponse() {
    // Arrange
    String url = "https://example.com";
    ResponseEntity<String> responseEntity = new ResponseEntity<>(null, HttpStatus.OK);

    when(restTemplate.getForEntity(url, String.class)).thenReturn(responseEntity);

    // Act
    HttpGetActivityInput input = new HttpGetActivityInput(url);
    HttpGetActivityOutput output = activities.httpGet(input);

    // Assert
    assertNotNull(output);
    assertEquals("", output.responseText());
    assertEquals(200, output.statusCode());
  }

  @Test
  void testHttpGet_NonOkStatus() {
    // Arrange
    String url = "https://example.com";
    String responseBody = "Not Found";
    ResponseEntity<String> responseEntity =
        new ResponseEntity<>(responseBody, HttpStatus.NOT_FOUND);

    when(restTemplate.getForEntity(url, String.class)).thenReturn(responseEntity);

    // Act
    HttpGetActivityInput input = new HttpGetActivityInput(url);
    HttpGetActivityOutput output = activities.httpGet(input);

    // Assert
    assertNotNull(output);
    assertEquals(responseBody, output.responseText());
    assertEquals(404, output.statusCode());
  }

  @Test
  void testHttpGet_NetworkError() {
    // Arrange
    String url = "https://invalid-url.com";
    when(restTemplate.getForEntity(url, String.class))
        .thenThrow(new RestClientException("Connection refused"));

    // Act & Assert
    HttpGetActivityInput input = new HttpGetActivityInput(url);
    assertThrows(RestClientException.class, () -> activities.httpGet(input));
  }
}
