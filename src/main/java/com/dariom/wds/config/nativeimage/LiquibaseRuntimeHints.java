package com.dariom.wds.config.nativeimage;

import static org.springframework.aot.hint.MemberCategory.DECLARED_FIELDS;
import static org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_CONSTRUCTORS;
import static org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_METHODS;
import static org.springframework.aot.hint.MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS;
import static org.springframework.aot.hint.MemberCategory.INVOKE_PUBLIC_METHODS;

import liquibase.change.ChangeFactory;
import liquibase.change.ColumnConfig;
import liquibase.change.ConstraintsConfig;
import liquibase.change.core.AddColumnChange;
import liquibase.change.core.AddForeignKeyConstraintChange;
import liquibase.change.core.AddPrimaryKeyChange;
import liquibase.change.core.AddUniqueConstraintChange;
import liquibase.change.core.CreateIndexChange;
import liquibase.change.core.CreateTableChange;
import liquibase.change.core.DropColumnChange;
import liquibase.change.core.DropForeignKeyConstraintChange;
import liquibase.change.core.DropIndexChange;
import liquibase.change.core.DropTableChange;
import liquibase.change.core.InsertDataChange;
import liquibase.change.core.RawSQLChange;
import liquibase.change.visitor.ChangeVisitorFactory;
import liquibase.changelog.ChangeLogHistoryServiceFactory;
import liquibase.changelog.FastCheckService;
import liquibase.changelog.StandardChangeLogHistoryService;
import liquibase.changelog.visitor.ValidatingVisitorGeneratorFactory;
import liquibase.changeset.ChangeSetServiceFactory;
import liquibase.command.CommandFactory;
import liquibase.configuration.ConfiguredValueModifierFactory;
import liquibase.database.ConnectionServiceFactory;
import liquibase.database.DatabaseFactory;
import liquibase.database.LiquibaseTableNamesFactory;
import liquibase.database.PreparedStatementFactory;
import liquibase.datatype.DataTypeFactory;
import liquibase.diff.DiffGeneratorFactory;
import liquibase.diff.compare.DatabaseObjectComparatorFactory;
import liquibase.diff.output.changelog.ChangeGeneratorFactory;
import liquibase.executor.ExecutorService;
import liquibase.io.OutputFileHandlerFactory;
import liquibase.license.LicenseServiceFactory;
import liquibase.lockservice.LockServiceFactory;
import liquibase.logging.core.JavaLogService;
import liquibase.logging.core.LogServiceFactory;
import liquibase.logging.mdc.MdcManagerFactory;
import liquibase.parser.ChangeLogParserFactory;
import liquibase.parser.NamespaceDetailsFactory;
import liquibase.parser.SnapshotParserFactory;
import liquibase.parser.SqlParserFactory;
import liquibase.plugin.PluginFactory;
import liquibase.precondition.PreconditionFactory;
import liquibase.report.ShowSummaryGeneratorFactory;
import liquibase.resource.PathHandlerFactory;
import liquibase.serializer.ChangeLogSerializerFactory;
import liquibase.serializer.SnapshotSerializerFactory;
import liquibase.servicelocator.StandardServiceLocator;
import liquibase.snapshot.SnapshotGeneratorFactory;
import liquibase.sql.visitor.SqlVisitorFactory;
import liquibase.sqlgenerator.SqlGeneratorFactory;
import liquibase.structure.core.DatabaseObjectFactory;
import liquibase.ui.LoggerUIService;
import liquibase.ui.UIServiceFactory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

public class LiquibaseRuntimeHints implements RuntimeHintsRegistrar {

  private static final Class<?>[] LIQUIBASE_SERVICE_TYPES = {
      LoggerUIService.class,
      FastCheckService.class,
      LiquibaseTableNamesFactory.class,
      StandardChangeLogHistoryService.class,
      SnapshotGeneratorFactory.class,
      ChangeLogParserFactory.class,
      SnapshotParserFactory.class,
      SqlParserFactory.class,
      NamespaceDetailsFactory.class,
      SqlGeneratorFactory.class,
      DatabaseFactory.class,
      DatabaseObjectFactory.class,
      ChangeLogHistoryServiceFactory.class,
      ValidatingVisitorGeneratorFactory.class,
      ChangeFactory.class,
      ChangeVisitorFactory.class,
      ChangeSetServiceFactory.class,
      CommandFactory.class,
      ConfiguredValueModifierFactory.class,
      ConnectionServiceFactory.class,
      PreparedStatementFactory.class,
      DataTypeFactory.class,
      DiffGeneratorFactory.class,
      DatabaseObjectComparatorFactory.class,
      ChangeGeneratorFactory.class,
      OutputFileHandlerFactory.class,
      LockServiceFactory.class,
      UIServiceFactory.class,
      LogServiceFactory.class,
      MdcManagerFactory.class,
      JavaLogService.class,
      StandardServiceLocator.class,
      LicenseServiceFactory.class,
      ExecutorService.class,
      PreconditionFactory.class,
      ShowSummaryGeneratorFactory.class,
      ChangeLogSerializerFactory.class,
      SnapshotSerializerFactory.class,
      PathHandlerFactory.class,
      PluginFactory.class,
      SqlVisitorFactory.class,
  };

  private static final Class<?>[] LIQUIBASE_CHECKSUM_TYPES = {
      CreateTableChange.class,
      AddColumnChange.class,
      DropColumnChange.class,
      DropForeignKeyConstraintChange.class,
      DropIndexChange.class,
      DropTableChange.class,
      InsertDataChange.class,
      AddForeignKeyConstraintChange.class,
      AddPrimaryKeyChange.class,
      AddUniqueConstraintChange.class,
      CreateIndexChange.class,
      RawSQLChange.class,
      ColumnConfig.class,
      ConstraintsConfig.class,
  };

  @Override
  public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
    for (var type : LIQUIBASE_SERVICE_TYPES) {
      hints.reflection().registerType(type,
          INVOKE_PUBLIC_CONSTRUCTORS, INVOKE_DECLARED_CONSTRUCTORS);
    }

    for (var type : LIQUIBASE_CHECKSUM_TYPES) {
      hints.reflection().registerType(type,
          INVOKE_PUBLIC_CONSTRUCTORS,
          INVOKE_DECLARED_CONSTRUCTORS,
          INVOKE_PUBLIC_METHODS,
          INVOKE_DECLARED_METHODS,
          DECLARED_FIELDS);
    }
  }

}
