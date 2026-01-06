package com.dariom.wds.service;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

public final class NoOpTransactionManager implements PlatformTransactionManager {

  @Override
  public TransactionStatus getTransaction(TransactionDefinition definition) {
    return new NoOpTransactionStatus();
  }

  @Override
  public void commit(TransactionStatus status) {
  }

  @Override
  public void rollback(TransactionStatus status) {
  }

  static final class NoOpTransactionStatus implements TransactionStatus {

    private boolean rollbackOnly;

    @Override
    public boolean isNewTransaction() {
      return true;
    }

    @Override
    public boolean hasSavepoint() {
      return false;
    }

    @Override
    public void setRollbackOnly() {
      rollbackOnly = true;
    }

    @Override
    public boolean isRollbackOnly() {
      return rollbackOnly;
    }

    @Override
    public void flush() {
    }

    @Override
    public boolean isCompleted() {
      return false;
    }

    @Override
    public Object createSavepoint() {
      throw new UnsupportedOperationException();
    }

    @Override
    public void rollbackToSavepoint(Object savepoint) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void releaseSavepoint(Object savepoint) {
      throw new UnsupportedOperationException();
    }
  }
}
