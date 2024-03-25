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

public class WorkflowService {
  private static final String SUBMIT = "/api/v1/workflows/%s/submit";
  private static final String STATUS = "/api/v1/workflows/%s/%s";
  private final String url;
  private HttpClient client;
  ObjectMapper mapper;

  public WorkflowService(String url) {
    this.url = url;
    this.mapper = new ObjectMapper();
    this.client = createHttpClientBypassingSSLVerification();
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


      HttpRequest request = HttpRequest.newBuilder()
                                       .uri(new URI(url + SUBMIT.formatted(workflowSubmissionDTO.getNamespace())))
                                       .header("Content-Type", "application/json")
                                       .POST(BodyPublishers.ofString(workflowSubmissionDTO.toJson()))
                                       .build();

      HttpResponse<String> response = client.send(request, BodyHandlers.ofString());

      return mapper.readValue(response.body(), WorkflowSubmissionResultDTO.class);
    } catch (Exception e) {
      throw new RuntimeException("Failed to submit workflow", e);
    }
  }
}
