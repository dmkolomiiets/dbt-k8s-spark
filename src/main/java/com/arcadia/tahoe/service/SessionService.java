package com.arcadia.tahoe.service;

import lombok.SneakyThrows;
import lombok.extern.java.Log;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.paginators.DescribeInstancesIterable;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

@Log
@Service
public class SessionService {
  public static final int PORT = 9091;
  private static final String COMMAND = """
    aws ssm start-session --region %s --target %s --document-name AWS-StartPortForwardingSessionToRemoteHost --parameters {"portNumber":["443"],"localPortNumber":["%s"],"host":["%s"]}""";

  @SneakyThrows
  public void openSession(String region, String apiUrl) {
    var instanceId = getInstanceId(region);
    var command = COMMAND.formatted(region, instanceId, PORT, new URL(apiUrl).getHost());

    try {
      ProcessBuilder pb = new ProcessBuilder(command.split(" "));
      pb.redirectErrorStream(true);
      Process p = pb.start();
      BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
      String line;
      while ((line = r.readLine()) != null) {
        log.info(line);
        if (line.contains("Starting session with SessionId"))
          break;
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

    } catch (Exception e) {
      log.severe(e.getMessage());
      throw new RuntimeException(e);
    }
  }

  private String getInstanceId(String region) {
    try (Ec2Client ec2 = Ec2Client.builder().region(Region.of(region)).build()) {
      DescribeInstancesRequest request = DescribeInstancesRequest.builder()
                                                                 .build();

      DescribeInstancesIterable instancesIterable = ec2.describeInstancesPaginator(request);

      return instancesIterable
        .stream()
        .flatMap(r -> r.reservations().stream())
        .flatMap(reservation -> reservation.instances().stream())
        .filter(instance -> instance.tags().stream().anyMatch(tag ->
          tag.key().equals("aws:eks:cluster-name")
            && tag.value().equals("preprd-data-processing")))
        .map(Instance::instanceId)
        .findAny().orElse(null);

    } catch (Exception e) {
      log.severe(e.getMessage());
      throw e;
    }
  }
}
