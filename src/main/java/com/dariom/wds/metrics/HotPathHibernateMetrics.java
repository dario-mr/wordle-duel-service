package com.dariom.wds.metrics;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HotPathHibernateMetrics {

  private static final String METRIC_NAME = "wordle.hotpath.hibernate.delta";

  private final EntityManagerFactory entityManagerFactory;
  private final MeterRegistry meterRegistry;

  public <T> T record(String operation, String step, HotPathMetrics.ThrowingSupplier<T> supplier) {
    var statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    if (!statistics.isStatisticsEnabled()) {
      return supplier.get();
    }

    var snapshot = StatisticsSnapshot.capture(statistics);
    var result = supplier.get();
    StatisticsSnapshot.capture(statistics).recordDelta(snapshot, operation, step, meterRegistry);
    return result;
  }

  private record StatisticsSnapshot(
      long entityLoadCount,
      long entityFetchCount,
      long collectionLoadCount,
      long collectionFetchCount,
      long queryExecutionCount,
      long prepareStatementCount
  ) {

    private static final List<StatValue> STAT_VALUES = List.of(
        new StatValue("entity_load", StatisticsSnapshot::entityLoadCount),
        new StatValue("entity_fetch", StatisticsSnapshot::entityFetchCount),
        new StatValue("collection_load", StatisticsSnapshot::collectionLoadCount),
        new StatValue("collection_fetch", StatisticsSnapshot::collectionFetchCount),
        new StatValue("query_execution", StatisticsSnapshot::queryExecutionCount),
        new StatValue("prepare_statement", StatisticsSnapshot::prepareStatementCount)
    );

    private static StatisticsSnapshot capture(Statistics statistics) {
      return new StatisticsSnapshot(
          statistics.getEntityLoadCount(),
          statistics.getEntityFetchCount(),
          statistics.getCollectionLoadCount(),
          statistics.getCollectionFetchCount(),
          statistics.getQueryExecutionCount(),
          statistics.getPrepareStatementCount()
      );
    }

    private void recordDelta(
        StatisticsSnapshot before,
        String operation,
        String step,
        MeterRegistry meterRegistry
    ) {
      for (var statValue : STAT_VALUES) {
        DistributionSummary.builder(METRIC_NAME)
            .tag("operation", operation)
            .tag("step", step)
            .tag("stat", statValue.name())
            .baseUnit("operations")
            .register(meterRegistry)
            .record(Math.max(0, statValue.extract(this) - statValue.extract(before)));
      }
    }
  }

  private record StatValue(String name, StatExtractor extractor) {

    private long extract(StatisticsSnapshot snapshot) {
      return extractor.extract(snapshot);
    }
  }

  @FunctionalInterface
  private interface StatExtractor {

    long extract(StatisticsSnapshot snapshot);
  }
}
