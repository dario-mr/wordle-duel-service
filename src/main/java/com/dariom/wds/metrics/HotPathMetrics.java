package com.dariom.wds.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HotPathMetrics {

  private static final String METRIC_NAME = "wordle.hotpath.duration";
  private static final String OUTCOME_SUCCESS = "success";
  private static final String OUTCOME_ERROR = "error";

  private final MeterRegistry meterRegistry;

  public void record(String operation, String step, ThrowingRunnable runnable) {
    record(operation, step, () -> {
      runnable.run();
      return null;
    });
  }

  public <T> T record(String operation, String step, ThrowingSupplier<T> supplier) {
    var sample = Timer.start(meterRegistry);
    try {
      var result = supplier.get();
      stop(sample, operation, step, OUTCOME_SUCCESS);
      return result;
    } catch (RuntimeException e) {
      stop(sample, operation, step, OUTCOME_ERROR);
      throw e;
    }
  }

  private void stop(Timer.Sample sample, String operation, String step, String outcome) {
    sample.stop(Timer.builder(METRIC_NAME)
        .tag("operation", operation)
        .tag("step", step)
        .tag("outcome", outcome)
        .register(meterRegistry));
  }

  @FunctionalInterface
  public interface ThrowingRunnable {

    void run();
  }

  @FunctionalInterface
  public interface ThrowingSupplier<T> {

    T get();
  }
}
