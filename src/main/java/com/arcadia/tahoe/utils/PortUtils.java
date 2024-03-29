package com.arcadia.tahoe.utils;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ThreadLocalRandom;

public class PortUtils {
  public static int nextFreePort(int from, int to) {
    int port = randPort(from, to);
    while (true) {
      if (isLocalPortFree(port)) {
        return port;
      } else {
        port = ThreadLocalRandom.current().nextInt(from, to);
      }
    }
  }

  private static int randPort(int from, int to) {
    return ThreadLocalRandom.current().nextInt(from, to);
  }

  private static boolean isLocalPortFree(int port) {
    try {
      new ServerSocket(port).close();
      return true;
    } catch (IOException e) {
      return false;
    }
  }
}
