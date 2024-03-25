package com.arcadia.tahoe.configurations;

import com.arcadia.tahoe.TestApplication;
import com.arcadia.tahoe.service.KubeProxyService;
import com.arcadia.tahoe.service.WorkflowService;
import com.google.common.base.Converter;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.SneakyThrows;
import lombok.extern.java.Log;
import org.assertj.core.api.Assertions;
import org.junit.platform.commons.function.Try;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import software.amazon.awssdk.regions.Region;

import javax.annotation.Nonnull;
import java.util.Random;

@Configuration
@ComponentScan(basePackageClasses = TestApplication.class)
@Log
public class ApplicationConfiguration {

  @Autowired
  @SneakyThrows
  public void init(KubeProxyService proxyService, Environment env) {

    Try.call(() -> Runtime.getRuntime().exec("kubectl")).ifFailure(e -> Assertions.fail(e.getMessage()));
    Try.call(() -> Runtime.getRuntime().exec("aws")).ifFailure(e -> Assertions.fail(e.getMessage()));
    Try.call(() -> Runtime.getRuntime().exec("session-manager-plugin")).ifFailure(e -> Assertions.fail(e.getMessage()));

    String region = env.getProperty("region", Region.US_EAST_1.toString());
    String clusterName = env.getProperty("cluster");

    proxyService.configureKubeCluster(region, clusterName);
    proxyService.openSession(region);
  }

  @Bean
  public Converter<String, String> dslNameConverter() {
    return new Converter<>() {
      @Override
      protected String doForward(final @Nonnull String s) {
        return s.toUpperCase().replace(' ', '_');
      }

      @Override
      protected String doBackward(final @Nonnull String s) {
        return s.toLowerCase().replace('_', ' ');
      }
    };
  }

  @Bean
  public CacheManager cacheManager() {
    return new ConcurrentMapCacheManager("cucumber");
  }

  @Bean
  public KubernetesClient kubernetesClient(KubeProxyService proxyService) {
    var conf = new ConfigBuilder()
      .withMasterUrl(proxyService.getClusterProxyUrl())
      .withTrustCerts(true)
      .build();
    return new DefaultKubernetesClient(conf);
  }

  @Bean
  public Integer argoPort() {
    Random rnd = new Random();
    int port = rnd.nextInt(9090 - 8080) + 8080;
    return port;
  }

  @Bean
  public WorkflowService workflowService(Integer argoPort) {
    return new WorkflowService("https://localhost:%s".formatted(argoPort));
  }
}
