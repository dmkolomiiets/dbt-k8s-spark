package com.arcadia.tahoe;

import io.cucumber.junit.platform.engine.Cucumber;
import io.cucumber.spring.CucumberContextConfiguration;
import io.fabric8.kubernetes.client.*;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

@EnableCaching
@SpringBootTest(
    classes = TestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE)
@CucumberContextConfiguration
@Cucumber
class TestApplicationTest {

  @Autowired private KubernetesClient kubernetesClient;
  @Autowired private CacheManager cacheManager;

  @Test
  void testAutoConfiguration() {
    Assertions.assertThat(this.cacheManager).isInstanceOf(ConcurrentMapCacheManager.class);
    Assertions.assertThat(this.cacheManager.getCacheNames()).contains("cucumber");
    Assertions.assertThat(this.kubernetesClient).isInstanceOf(DefaultKubernetesClient.class);
  }
}
