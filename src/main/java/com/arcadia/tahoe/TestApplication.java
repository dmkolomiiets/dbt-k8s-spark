package com.arcadia.tahoe;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication(scanBasePackageClasses = TestApplication.class)
public class TestApplication {

  public static void main(final String[] args) {
    new SpringApplicationBuilder().sources(TestApplication.class).run(args);
  }
}
