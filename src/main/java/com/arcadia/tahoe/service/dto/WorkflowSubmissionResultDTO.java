package com.arcadia.tahoe.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class WorkflowSubmissionResultDTO {
  private Metadata metadata;
  private Spec spec;
  private Status status;

  public String name() {
    return metadata.getName();
  }
}

@Data
class Metadata {
  private String name;
  @JsonProperty("generateName")
  private String generateName;
  private String namespace;
  private String uid;
  @JsonProperty("resourceVersion")
  private String resourceVersion;
  private int generation;
  @JsonProperty("creationTimestamp")
  private String creationTimestamp;
  private Map<String, String> labels;
  private List<ManagedField> managedFields;
}

@Data
class ManagedField {
  private String manager;
  private String operation;
  @JsonProperty("apiVersion")
  private String apiVersion;
  private String time;
  @JsonProperty("fieldsType")
  private String fieldsType;
  @JsonProperty("fieldsV1")
  private Map<String, Object> fieldsV1;
}

@Data
class Spec {
  private String entrypoint;
  private Arguments arguments;
  @JsonProperty("workflowTemplateRef")
  private WorkflowTemplateRef workflowTemplateRef;
}

@Data
class Arguments {
  private List<Parameter> parameters;
}

@Data
class Parameter {
  private String name;
  private String value;
}

@Data
class WorkflowTemplateRef {
  private String name;
}

@Data
class Status {
  @JsonProperty("startedAt")
  private String startedAt;
  @JsonProperty("finishedAt")
  private String finishedAt;
  @JsonProperty("storedTemplates")
  private Map<String, StoredTemplate> storedTemplates;
}

@Data
class StoredTemplate {
  private String name;
  private Inputs inputs;
  private Outputs outputs;
  private Metadata metadata;
  private Boolean daemon;
  private Container container;
  @JsonProperty("retryStrategy")
  private RetryStrategy retryStrategy;
  private Resource resource;
  private List<List<Step>> steps;
}

@Data
class Inputs {
  private List<Parameter> parameters;
}

@Data
class Outputs {
  private List<Parameter> parameters;
}

@Data
class Container {
  private String name;
  private String image;
  private List<String> command;
  private Map<String, String> resources;
}

@Data
class RetryStrategy {
  private int limit;
}

@Data
class Resource {
  private String action;
  private String manifest;
  @JsonProperty("successCondition")
  private String successCondition;
  @JsonProperty("failureCondition")
  private String failureCondition;
}

@Data
class Step {
  private String name;
  private String template;
  private Arguments arguments;
  private String when;
}
