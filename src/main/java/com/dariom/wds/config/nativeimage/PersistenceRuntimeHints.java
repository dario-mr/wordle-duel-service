package com.dariom.wds.config.nativeimage;

import static org.springframework.aot.hint.MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS;
import static org.springframework.aot.hint.MemberCategory.INVOKE_PUBLIC_METHODS;

import com.dariom.wds.persistence.entity.RoomPlayerIdEmbeddable;
import java.util.List;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;

public class PersistenceRuntimeHints implements RuntimeHintsRegistrar {

  private static final List<TypeReference> HIBERNATE_LOGGER_IMPLEMENTATIONS = List.of(
      TypeReference.of("org.hibernate.boot.jaxb.JaxbLogger_$logger"),
      TypeReference.of("org.hibernate.bytecode.enhance.spi.interceptor.BytecodeInterceptorLogging_$logger"),
      TypeReference.of("org.hibernate.cache.spi.SecondLevelCacheLogger_$logger"),
      TypeReference.of("org.hibernate.dialect.DialectLogging_$logger"),
      TypeReference.of("org.hibernate.engine.jdbc.JdbcLogging_$logger"),
      TypeReference.of("org.hibernate.engine.jdbc.batch.JdbcBatchLogging_$logger"),
      TypeReference.of("org.hibernate.engine.jdbc.env.internal.LobCreationLogging_$logger"),
      TypeReference.of("org.hibernate.internal.CoreMessageLogger_$logger"),
      TypeReference.of("org.hibernate.internal.EntityManagerMessageLogger_$logger"),
      TypeReference.of("org.hibernate.internal.log.ConnectionAccessLogger_$logger"),
      TypeReference.of("org.hibernate.internal.log.ConnectionInfoLogger_$logger"),
      TypeReference.of("org.hibernate.internal.log.DeprecationLogger_$logger"),
      TypeReference.of("org.hibernate.internal.log.IncubationLogger_$logger"),
      TypeReference.of("org.hibernate.internal.log.UrlMessageBundle_$logger"),
      TypeReference.of("org.hibernate.metamodel.mapping.MappingModelCreationLogging_$logger"),
      TypeReference.of("org.hibernate.query.QueryLogging_$logger"),
      TypeReference.of("org.hibernate.query.hql.HqlLogging_$logger"),
      TypeReference.of("org.hibernate.resource.beans.internal.BeansMessageLogger_$logger"),
      TypeReference.of("org.hibernate.sql.ast.tree.SqlAstTreeLogger_$logger"),
      TypeReference.of("org.hibernate.sql.exec.SqlExecLogger_$logger"),
      TypeReference.of("org.hibernate.sql.results.LoadingLogger_$logger"),
      TypeReference.of("org.hibernate.sql.results.ResultsLogger_$logger"),
      TypeReference.of("org.hibernate.sql.results.graph.embeddable.EmbeddableLoadingLogger_$logger")
  );

  @Override
  public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
    hints.reflection().registerType(RoomPlayerIdEmbeddable.class,
        INVOKE_PUBLIC_CONSTRUCTORS,
        INVOKE_PUBLIC_METHODS);

    for (var type : HIBERNATE_LOGGER_IMPLEMENTATIONS) {
      hints.reflection().registerType(type, INVOKE_PUBLIC_CONSTRUCTORS);
    }
  }

}
