package com.dariom.wds.it;

import static com.dariom.wds.it.config.SharedContainers.POSTGRES;
import static com.dariom.wds.it.config.SharedContainers.isPostgresContainerManaged;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

public abstract class AbstractPostgresTest {

  @DynamicPropertySource
  static void registerPostgresProperties(DynamicPropertyRegistry registry) {
    if (isPostgresContainerManaged()) {
      registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
      registry.add("spring.datasource.username", POSTGRES::getUsername);
      registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
  }
}
