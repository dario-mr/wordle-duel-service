package com.dariom.wds;

import static org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO;

import com.dariom.wds.config.nativeimage.CaffeineRuntimeHints;
import com.dariom.wds.config.nativeimage.LiquibaseRuntimeHints;
import com.dariom.wds.config.nativeimage.PersistenceRuntimeHints;
import com.dariom.wds.config.nativeimage.SecurityRuntimeHints;
import com.dariom.wds.config.nativeimage.SystemMetricsRuntimeHints;
import com.dariom.wds.config.nativeimage.ValidationRuntimeHints;
import com.dariom.wds.config.nativeimage.WebSocketRuntimeHints;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@ConfigurationPropertiesScan
@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)
@ImportRuntimeHints({
    LiquibaseRuntimeHints.class,
    CaffeineRuntimeHints.class,
    PersistenceRuntimeHints.class,
    SecurityRuntimeHints.class,
    SystemMetricsRuntimeHints.class,
    ValidationRuntimeHints.class,
    WebSocketRuntimeHints.class
})
public class Application {

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }

}
