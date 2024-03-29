package com.arcadia.tahoe.service;

import com.arcadia.tahoe.service.dto.WorkflowSubmissionDTO;
import com.arcadia.tahoe.service.dto.WorkflowSubmissionResultDTO;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Random;

public class WorkflowService {
  private static final String SUBMIT = "/api/v1/workflows/%s/submit";
  private static final String URL = "https://localhost:%s";
  final ObjectMapper mapper;
  private final String region;
  private final KubeProxyService proxyService;

  public WorkflowService(KubeProxyService proxyService, String region) {
    this.region = region;
    this.mapper = new ObjectMapper();
    this.proxyService = proxyService;
  }

  private int getPort() {
    Random rnd = new Random();
    return rnd.nextInt(9090 - 8080) + 8080;
  }

  private HttpClient createHttpClientBypassingSSLVerification() {
    try {
      TrustManager[] trustAllCerts = new TrustManager[]{
        new X509TrustManager() {
          public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
          }

          public void checkClientTrusted(X509Certificate[] certs, String authType) {
          }

          public void checkServerTrusted(X509Certificate[] certs, String authType) {
          }
        }
      };

      SSLContext sslContext = SSLContext.getInstance("SSL");
      sslContext.init(null, trustAllCerts, new SecureRandom());
      return HttpClient.newBuilder()
                       .sslContext(sslContext)
                       .sslParameters(sslContext.getDefaultSSLParameters())
                       .build();
    } catch (Exception e) {
      throw new RuntimeException("Failed to create a trust-all HttpClient", e);
    }
  }

  public WorkflowSubmissionResultDTO submitWorkflow(WorkflowSubmissionDTO workflowSubmissionDTO) {
    try {
      var port = getPort();
      proxyService.proxyToArgoService(workflowSubmissionDTO.getNamespace(), port);

      var client = createHttpClientBypassingSSLVerification();
      var submitUrl = URL.formatted(port) + SUBMIT.formatted(workflowSubmissionDTO.getNamespace());
      HttpRequest request = HttpRequest.newBuilder()
                                       .uri(new URI(submitUrl))
                                       .header("Content-Type", "application/json")
                                       .POST(BodyPublishers.ofString(workflowSubmissionDTO.toJson()))
                                       .build();

      HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
      proxyService.closeProxyToArgoService();
      return mapper.readValue(response.body(), WorkflowSubmissionResultDTO.class);
    } catch (Exception e) {
      throw new RuntimeException("Failed to submit workflow\n %s".formatted(workflowSubmissionDTO.toJson()), e);
    }
  }
}
