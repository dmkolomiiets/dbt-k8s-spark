package com.arcadia.tahoe.features;

import com.arcadia.tahoe.configurations.ApplicationConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@SpringBootTest(classes = {ApplicationConfiguration.class, KubernetesFeature.class})
class KubernetesFeatureTest {

  @Autowired
  private KubernetesFeature sampleSteps;
}
