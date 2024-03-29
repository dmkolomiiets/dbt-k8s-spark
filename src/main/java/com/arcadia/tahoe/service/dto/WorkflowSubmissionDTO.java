package com.arcadia.tahoe.service.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WorkflowSubmissionDTO implements ToJson {
  private String namespace;
  private String resourceKind;
  private String resourceName;
  private SubmitOptions submitOptions;

  // Constructor, getters, and setters

  public WorkflowSubmissionDTO(String namespace, String resourceKind, String resourceName, SubmitOptions submitOptions) {
    this.namespace = namespace;
    this.resourceKind = resourceKind;
    this.resourceName = resourceName;
    this.submitOptions = submitOptions;
  }

  public WorkflowSubmissionDTO() {
  }

  @Data
  public static class SubmitOptions {
    private String entryPoint;
    private String[] parameters;

    public SubmitOptions() {
    }

    public SubmitOptions(String entryPoint, String[] parameters) {
      this.entryPoint = entryPoint;
      this.parameters = parameters;
    }
  }
}
