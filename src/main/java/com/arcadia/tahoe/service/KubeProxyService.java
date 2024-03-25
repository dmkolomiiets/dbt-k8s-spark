package com.arcadia.tahoe.service;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import lombok.SneakyThrows;
import lombok.extern.java.Log;
import org.assertj.core.api.Assertions;
import org.junit.platform.commons.function.Try;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;

@Log
@Service
public class KubeProxyService {
  private static final String KUBE_PROXY_HOST = "localhost:%s";

  public KubeProxyService(SessionService sessionService) {
    this.sessionService = sessionService;
  }

  private final SessionService sessionService;

  @SneakyThrows
  public void proxyToArgoServiceEnabled(String namespace, int toPort) {
    var command = "kubectl --insecure-skip-tls-verify=true port-forward service/argo-server -n %s %s:2746";
    ProcessBuilder pb = new ProcessBuilder(command.formatted(namespace, toPort).split(" "));
    pb.redirectErrorStream(true);
    Process p = pb.start();
    BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
    String line;
    while ((line = r.readLine()) != null) {
      log.info(line);
      if (line.contains("Forwarding from")){
        break;
      }
    }
    new Thread(() -> {
      try {
        String restp;
        while ((restp = r.readLine()) != null) {
          log.info(restp);
        }
      } catch (Exception e) {
        log.severe(e.getMessage());
        throw new RuntimeException(e);
      }
    }).start();
  }

  @SneakyThrows
  public void configureKubeCluster(String region, String clusterName) {
    var command = "aws eks update-kubeconfig --name %s --region %s";
    ProcessBuilder pb = new ProcessBuilder(command.formatted(clusterName, region).split(" "));
    pb.redirectErrorStream(true);
    Process p = pb.start();
    BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
    String line;
    while ((line = r.readLine()) != null) {
      log.info(line);
    }

  }

  @SneakyThrows
  public void openSession(String region) {
    String clusterUrl = getClusterApiUrl();
    sessionService.openSession(region, clusterUrl);
    updateKubeConfig(clusterUrl);
  }

  private void updateKubeConfig(String clusterUrl) throws IOException {
    log.info("Modify kube config clusterUrl. Old: %s; New: %s.".formatted(clusterUrl, KUBE_PROXY_HOST.formatted(SessionService.PORT)));
    var path = Paths.get(Config.getKubeconfigFilename());
    var charset = StandardCharsets.UTF_8;
    var content = Files.readString(path, charset);
    content = content.replaceAll(new URL(clusterUrl).getHost(), KUBE_PROXY_HOST.formatted(SessionService.PORT));
    Files.writeString(path, content, charset);
  }

  public String getClusterProxyUrl() {
    return "https://"+ KUBE_PROXY_HOST.formatted(SessionService.PORT);
  }

  private String getClusterApiUrl() {
    return new ConfigBuilder().build().getMasterUrl();
  }
}
