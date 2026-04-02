package com.dariom.wds.config.nativeimage;

import static org.springframework.aot.hint.ExecutableMode.INVOKE;

import com.sun.management.OperatingSystemMXBean;
import java.lang.NoSuchMethodException;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

public class SystemMetricsRuntimeHints implements RuntimeHintsRegistrar {

  @Override
  public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
    registerMethod(hints, "getCpuLoad");
    registerMethod(hints, "getSystemCpuLoad");
    registerMethod(hints, "getProcessCpuLoad");
    registerMethod(hints, "getProcessCpuTime");
  }

  private void registerMethod(RuntimeHints hints, String methodName) {
    try {
      var method = OperatingSystemMXBean.class.getMethod(methodName);
      hints.reflection().registerMethod(method, INVOKE);
    } catch (NoSuchMethodException e) {
      throw new IllegalStateException("Missing OperatingSystemMXBean method: " + methodName, e);
    }
  }

}
