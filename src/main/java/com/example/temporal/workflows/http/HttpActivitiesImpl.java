package com.example.temporal.workflows.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Implementation of HTTP activities using Spring's RestTemplate.
 *
 * <p>This class can be used as a Spring bean or instantiated directly in workers.
 */
@Component
public class HttpActivitiesImpl implements HttpActivities {

  private static final Logger logger = LoggerFactory.getLogger(HttpActivitiesImpl.class);

  private final RestTemplate restTemplate;

  /**
   * Constructor with RestTemplate dependency injection.
   *
   * @param restTemplate The RestTemplate to use for HTTP requests
   */
  public HttpActivitiesImpl(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  /**
   * Default constructor that creates a RestTemplate instance. Useful for standalone workers that
   * don't use Spring dependency injection.
   */
  public HttpActivitiesImpl() {
    this.restTemplate = new RestTemplate();
  }

  @Override
  public HttpGetActivityOutput httpGet(HttpGetActivityInput input) {
    logger.info("Performing HTTP GET request to URL: {}", input.url());

    try {
      ResponseEntity<String> response = restTemplate.getForEntity(input.url(), String.class);

      String responseText = response.getBody() != null ? response.getBody() : "";
      int statusCode = response.getStatusCode().value();

      logger.info("HTTP GET request completed with status code: {}", statusCode);

      return new HttpGetActivityOutput(responseText, statusCode);
    } catch (Exception e) {
      logger.error("Error performing HTTP GET request to {}: {}", input.url(), e.getMessage());
      throw e;
    }
  }
}
