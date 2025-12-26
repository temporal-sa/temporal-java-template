package com.example.temporal.config;

import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import java.io.FileInputStream;
import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for Temporal workflow client beans.
 *
 * <p>This configuration automatically detects whether to connect to a local Temporal server or
 * Temporal Cloud based on the presence of mTLS certificate configuration. If cert-path and key-path
 * are provided, it enables mTLS for Temporal Cloud. Otherwise, it connects to a local Temporal
 * server.
 */
@Configuration
public class TemporalConfig {

  private static final Logger logger = LoggerFactory.getLogger(TemporalConfig.class);

  @Value("${temporal.service-address}")
  private String serviceAddress;

  @Value("${temporal.namespace}")
  private String namespace;

  @Value("${temporal.cert-path:}")
  private String certPath;

  @Value("${temporal.key-path:}")
  private String keyPath;

  /**
   * Creates a WorkflowServiceStubs bean for connecting to the Temporal server.
   *
   * <p>Automatically configures mTLS if cert-path and key-path are provided, otherwise uses
   * standard connection for local development.
   *
   * @return WorkflowServiceStubs configured with the service address and optional mTLS
   * @throws Exception if mTLS configuration fails
   */
  @Bean
  public WorkflowServiceStubs workflowServiceStubs() throws Exception {
    WorkflowServiceStubsOptions.Builder optionsBuilder =
        WorkflowServiceStubsOptions.newBuilder().setTarget(serviceAddress);

    // Enable mTLS if cert paths are provided (Temporal Cloud)
    if (certPath != null && !certPath.isEmpty() && keyPath != null && !keyPath.isEmpty()) {
      logger.info("Configuring Temporal Cloud connection with mTLS to: {}", serviceAddress);

      try (InputStream clientCert = new FileInputStream(certPath);
          InputStream clientKey = new FileInputStream(keyPath)) {

        SslContext sslContext =
            GrpcSslContexts.forClient().keyManager(clientCert, clientKey).build();

        optionsBuilder.setSslContext(sslContext);
      }

      logger.info("mTLS configuration successful");
    } else {
      logger.info("Configuring local Temporal server connection to: {}", serviceAddress);
    }

    return WorkflowServiceStubs.newServiceStubs(optionsBuilder.build());
  }

  /**
   * Creates a WorkflowClient bean for executing workflows.
   *
   * @param workflowServiceStubs the service stubs to use for communication
   * @return WorkflowClient configured with the specified namespace
   */
  @Bean
  public WorkflowClient workflowClient(WorkflowServiceStubs workflowServiceStubs) {
    WorkflowClientOptions options =
        WorkflowClientOptions.newBuilder().setNamespace(namespace).build();
    return WorkflowClient.newInstance(workflowServiceStubs, options);
  }
}
