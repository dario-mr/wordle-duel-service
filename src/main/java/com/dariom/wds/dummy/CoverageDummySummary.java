package com.dariom.wds.dummy;

public record CoverageDummySummary(int total, int errors) {

  public double errorRate() {
    if (total <= 0) {
      return 0.0;
    }

    return (double) errors / total;
  }

  public boolean isHealthy() {
    return errors <= 0 || errorRate() < 0.05;
  }
}
