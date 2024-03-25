package com.arcadia.tahoe.features;

import com.arcadia.tahoe.argo.ArgoWorkflow;
import com.arcadia.tahoe.argo.ArgoWorkflowTemplate;
import com.arcadia.tahoe.dsl.Maybe;
import com.arcadia.tahoe.service.KubeProxyService;
import com.arcadia.tahoe.service.WorkflowService;
import com.arcadia.tahoe.service.dto.WorkflowSubmissionDTO;
import com.arcadia.tahoe.service.dto.WorkflowSubmissionResultDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Converter;
import com.google.common.base.Enums;
import io.cucumber.java.DefaultDataTableCellTransformer;
import io.cucumber.java.DefaultDataTableEntryTransformer;
import io.cucumber.java.DefaultParameterTransformer;
import io.cucumber.java.ParameterType;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.api.model.NamedContext;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import lombok.SneakyThrows;
import lombok.extern.java.Log;
import org.assertj.core.api.Assertions;
import org.junit.platform.commons.function.Try;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CachePut;

import javax.annotation.Nonnull;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Log
@CacheConfig(cacheNames = "cucumber")
public class KubernetesFeature {

  private final KubernetesClient kubernetesClient;
  private final ObjectMapper objectMapper;
  private final Converter<String, String> dslNameConverter;
  private final Cache cache;
  private final WorkflowService workflowService;
  private Integer argoPort;
  private KubeProxyService proxyService;

  public KubernetesFeature(
    @Autowired final Converter<String, String> dslNameConverter,
    @Autowired final ObjectMapper objectMapper,
    @Autowired final KubernetesClient kubernetesClient,
    @Value("#{cacheManager.getCache('cucumber')}") final Cache cache,
    @Autowired WorkflowService workflowService,
    @Autowired Integer argoPort,
    KubeProxyService proxyService) {
    this.kubernetesClient = kubernetesClient;
    this.objectMapper = objectMapper;
    this.dslNameConverter = dslNameConverter;
    this.cache = cache;
    this.workflowService = workflowService;
    this.argoPort = argoPort;
    this.proxyService = proxyService;
  }

  @DefaultDataTableEntryTransformer
  @DefaultDataTableCellTransformer
  @DefaultParameterTransformer
  public Object defaultTransformer(final Object fromValue, final Type toValueType) {
    return this.objectMapper.convertValue(fromValue, this.objectMapper.constructType(toValueType));
  }

  /**
   * @param value One of "is" | "is not" | "are" | "are not" | "has" | "has not" | "should" |
   *              "should not" . Case insensitive
   * @return {@link Maybe}
   */
  @ParameterType("(is|is not|has|has not|have|have not|should|should not|are|are not|contains)")
  public Maybe maybe(final String value) {
    return Enums.stringConverter(Maybe.class)
                .compose(this.dslNameConverter::convert)
                .apply(value);
  }

  @ParameterType(
    "(pods|services|ingresses|deployments|replica sets|daemon sets|stateful sets|secrets|config maps|workflow template)")
  public Supplier<MixedOperation<?, ? extends KubernetesResourceList<?>, ?>> kubernetesResource(
    @Nonnull final String value) {

    return switch (value) {
      case "services" -> this.kubernetesClient::services;
      case "ingresses" -> this.kubernetesClient.network()::ingress;
      case "config maps" -> this.kubernetesClient::configMaps;
      case "secrets" -> this.kubernetesClient::secrets;
      case "deployments" -> this.kubernetesClient.apps()::deployments;
      case "daemon sets" -> this.kubernetesClient.apps()::daemonSets;
      case "replica sets" -> this.kubernetesClient.apps()::replicaSets;
      case "workflow template" -> () -> kubernetesClient.resources(ArgoWorkflowTemplate.class);
      case "workflow" -> () -> kubernetesClient.resources(ArgoWorkflow.class);
      default -> this.kubernetesClient::pods;
    };
  }

  @Given("{maybe} resource with {string} {maybe} equal to {string}")
  @CachePut(key = "'template'")
  public String resourceWithPathMaybeExist(
    @Nonnull final Maybe maybeHas,
    @Nonnull final String path,
    @Nonnull final Maybe maybeEqualTo,
    @Nonnull final String value) {
    final var resources = this.cache.get("resources", ArrayList<HasMetadata>::new);
    Assertions.assertThat(resources).extracting(path).anyMatch(it -> {
      return it.toString().contains(value);
    });
    return value;
  }

  @Given("namespace is {string}")
  @CachePut(key = "#root.methodName")
  public Namespace namespace(final String expected) {
    final var namespace = this.kubernetesClient.namespaces().withName(expected).get();
    Assertions.assertThat(namespace).isNotNull();
    return namespace;
  }

  @When("I get {kubernetesResource}")
  @CachePut(key = "#root.methodName")
  public <T extends HasMetadata> List<T> resources(
    @Nonnull final Supplier<MixedOperation<T, KubernetesResourceList<T>, ?>>
      kubernetesResourceSupplier) {
    final var namespace = this.cache.get("namespace", Namespace.class);
    final var operation = kubernetesResourceSupplier.get();
    return (namespace == null)
      ? operation.inAnyNamespace().list().getItems()
      : operation.inNamespace(namespace.getMetadata().getNamespace()).list().getItems();
  }

  @Then("list size {maybe} greater then {int}")
  public void listSize(final Maybe maybe, final Integer size) {
    Assertions.assertThat(this.cache.get("resources", List.class))
              .hasSizeGreaterThanOrEqualTo(size);
  }

  @When("I submit workflow with parameters")
  @SneakyThrows
  @CachePut(key = "'workflowSubmission'")
  public WorkflowSubmissionResultDTO iSubmitWorkflowWithParams(String workflowParams) {
    var options = objectMapper.readValue(workflowParams, WorkflowSubmissionDTO.SubmitOptions.class);
    final var namespace = this.cache.get("namespace", Namespace.class);
    final var workflowTemplate = this.cache.get("template", String.class);
    var workflow = WorkflowSubmissionDTO.builder()
                                        .namespace(namespace.getMetadata().getName())
                                        .resourceKind("WorkflowTemplate")
                                        .resourceName(workflowTemplate)
                                        .submitOptions(options);
    var submitResult = this.workflowService.submitWorkflow(workflow.build());
    log.info(() -> "Submitted workflow: " + submitResult.name());
    return submitResult;
  }

  @Then("It {maybe} completed {string}")
  public void itCompletedSuccessfully(Maybe maybe, String status) throws InterruptedException {
    final var namespace = this.cache.get("namespace", Namespace.class).getMetadata().getName();
    final var workflowName = this.cache.get("workflowSubmission", WorkflowSubmissionResultDTO.class).name();
    Resource<ArgoWorkflow> workflow = (Resource<ArgoWorkflow>) kubernetesResource("workflow").get()
                                                                                             .inNamespace(namespace)
                                                                                             .withName(workflowName);
    while (true) {
      var workflowStatus = workflow.get().getStatus().get("phase");
      log.info(() -> "Workflow: '%s' status: '%s'".formatted(workflowName, workflowStatus));

      if (!workflowStatus.equals("Running")) {
        break;
      } else {
        Thread.sleep(30_000);
      }
    }

    Assertions.assertThat(workflow.get().getStatus().get("phase").equals(status))
              .isEqualTo(maybe.yes());
  }

  @And("proxy to argo-service enabled")
  public void proxyToArgoServiceEnabled() {
    final var namespace = this.cache.get("namespace", Namespace.class).getMetadata().getName();
    proxyService.proxyToArgoServiceEnabled(namespace, argoPort);
  }
}
