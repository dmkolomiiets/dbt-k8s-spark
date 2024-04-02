package com.arcadia.tahoe.service;

import com.arcadia.tahoe.configurations.ConfigKeys;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import lombok.SneakyThrows;
import lombok.extern.java.Log;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

@Log
@Service
public class KubeProxyService {
  private static final String KUBE_PROXY_HOST = "localhost:%s";
  private static final String PROXY_PASS_COMMAND = "kubectl --insecure-skip-tls-verify=true port-forward service/argo-server -n %s %s:2746";
  private static final String UPDATE_KUBE_CONFIG_COMMAND = "aws eks update-kubeconfig --name %s --region %s --role-arn %s";
  private final SessionService sessionService;
  private final Environment environment;
  private final ThreadLocal<Process> proxyToArgoServiceProcess = new InheritableThreadLocal<>();

  public KubeProxyService(SessionService sessionService, Environment environment) {
    this.sessionService = sessionService;
    this.environment = environment;
  }

  @SneakyThrows
  public void proxyToArgoService(String namespace, int toPort) {
    ProcessBuilder pb = new ProcessBuilder(PROXY_PASS_COMMAND.formatted(namespace, toPort).split(" "));
    pb.redirectErrorStream(true);
    Process process = pb.start();
    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
    String line;
    while ((line = reader.readLine()) != null) {
      log.info(line);
      if (line.contains("Forwarding from")) {
        break;
      }
    }
    proxyToArgoServiceProcess.set(process);
  }


  public void closeProxyToArgoService() {
    proxyToArgoServiceProcess.get().destroy();
  }

  @SneakyThrows
  public void configureKubeCluster(String region, String clusterName) {
    var command = UPDATE_KUBE_CONFIG_COMMAND
      .formatted(clusterName, region, environment.getProperty(ConfigKeys.CLUSTER_ROLE));
    ProcessBuilder pb = new ProcessBuilder(command.split(" "));
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
    return "https://" + KUBE_PROXY_HOST.formatted(SessionService.PORT);
  }

  private String getClusterApiUrl() {
    return new ConfigBuilder().build().getMasterUrl();
  }
}
