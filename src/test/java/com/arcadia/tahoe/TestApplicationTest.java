package com.arcadia.tahoe;

import io.cucumber.junit.CucumberOptions;
import io.cucumber.junit.platform.engine.Cucumber;
import io.cucumber.spring.CucumberContextConfiguration;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
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
@CucumberOptions(
  glue= {"stepDefinitions"},
  monochrome = true,

  plugin = {
    "pretty","summary",
    "json:target/cucumber-reports/Cucumber.json",
    "junit:target/cucumber-reports/Cucumber.xml",
    "html:target/cucumber-reports",
//    "com.aventstack.extentreports.cucumber.adapter.ExtentCucumberAdapter",
  }
//  monochrome = true

//  plugin = {
//    "pretty",
//    "junit:target/cucumber-reports/Cucumber.xml",
//    "json:target/cucumber-reports/Cucumber.json",
//    "html:target/cucumber-reports/Cucumber.html",
//
//
//  },
)
class TestApplicationTest {

  @Autowired
  private KubernetesClient kubernetesClient;
  @Autowired
  private CacheManager cacheManager;

  @Test
  void testAutoConfiguration() {
    Assertions.assertThat(this.cacheManager).isInstanceOf(ConcurrentMapCacheManager.class);
    Assertions.assertThat(this.cacheManager.getCacheNames()).contains("cucumber");
    Assertions.assertThat(this.kubernetesClient).isInstanceOf(DefaultKubernetesClient.class);
  }
}
