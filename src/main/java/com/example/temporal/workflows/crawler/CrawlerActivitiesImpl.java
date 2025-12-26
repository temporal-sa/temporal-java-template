package com.example.temporal.workflows.crawler;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Implementation of crawler activities for fetching URLs and parsing links.
 *
 * <p>This class handles HTTP requests, HTML parsing, and link extraction.
 */
@Component
public class CrawlerActivitiesImpl implements CrawlerActivities {

  private static final Logger logger = LoggerFactory.getLogger(CrawlerActivitiesImpl.class);
  private static final String USER_AGENT = "Mozilla/5.0 (compatible; TemporalCrawler/1.0)";
  private static final Pattern LINK_PATTERN =
      Pattern.compile("<a\\s+(?:[^>]*?\\s+)?href=\"([^\"]*)\"", Pattern.CASE_INSENSITIVE);

  @Override
  public ParseLinksOutput parseLinksFromUrl(ParseLinksInput input) {
    logger.info("Parsing links from URL: {}", input.url());

    // Fetch the URL content
    UrlContent urlContent = fetchUrl(input.url());

    if (!urlContent.success()) {
      logger.warn("Failed to fetch URL: {}", input.url());
      return new ParseLinksOutput(List.of());
    }

    // Parse links from the HTML content
    List<String> links = parseLinks(urlContent.htmlContent(), input.url());

    logger.info("Found {} links on {}", links.size(), input.url());
    return new ParseLinksOutput(links);
  }

  /**
   * Fetches content from a URL.
   *
   * @param urlString The URL to fetch
   * @return UrlContent containing the HTML content and success status
   */
  private UrlContent fetchUrl(String urlString) {
    try {
      URI uri = new URI(urlString);
      HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();

      // Set User-Agent header
      connection.setRequestMethod("GET");
      connection.setRequestProperty("User-Agent", USER_AGENT);
      connection.setConnectTimeout(5000);
      connection.setReadTimeout(5000);

      int responseCode = connection.getResponseCode();

      if (responseCode >= 200 && responseCode < 300) {
        // Read response with UTF-8 encoding
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader =
            new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
          String line;
          while ((line = reader.readLine()) != null) {
            content.append(line).append("\n");
          }
        }

        logger.debug("Successfully fetched URL: {} (status: {})", urlString, responseCode);
        return new UrlContent(urlString, content.toString(), true);
      } else {
        logger.warn("Failed to fetch URL: {} (status: {})", urlString, responseCode);
        return new UrlContent(urlString, "", false);
      }

    } catch (Exception e) {
      logger.error("Error fetching URL {}: {}", urlString, e.getMessage());
      return new UrlContent(urlString, "", false);
    }
  }

  /**
   * Parses links from HTML content.
   *
   * @param htmlContent The HTML content to parse
   * @param baseUrl The base URL for resolving relative links
   * @return List of absolute URLs found in the HTML
   */
  private List<String> parseLinks(String htmlContent, String baseUrl) {
    Set<String> uniqueLinks = new HashSet<>();

    Matcher matcher = LINK_PATTERN.matcher(htmlContent);
    while (matcher.find()) {
      String href = matcher.group(1);

      // Convert relative URLs to absolute
      String absoluteUrl = resolveUrl(href, baseUrl);

      // Filter valid HTTP/HTTPS URLs
      if (absoluteUrl != null
          && (absoluteUrl.startsWith("http://") || absoluteUrl.startsWith("https://"))) {
        uniqueLinks.add(absoluteUrl);
      }
    }

    return new ArrayList<>(uniqueLinks);
  }

  /**
   * Resolves a URL (possibly relative) against a base URL.
   *
   * @param href The URL to resolve
   * @param baseUrl The base URL
   * @return The absolute URL, or null if resolution fails
   */
  private String resolveUrl(String href, String baseUrl) {
    try {
      URI baseUri = new URI(baseUrl);
      URI resolvedUri = baseUri.resolve(href);
      return resolvedUri.toString();
    } catch (Exception e) {
      logger.debug("Failed to resolve URL: {} against base: {}", href, baseUrl);
      return null;
    }
  }
}
