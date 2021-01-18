package org.pmiops.workbench.db.jdbc;

import java.util.Iterator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.pmiops.workbench.model.ReportingCohort;
import org.pmiops.workbench.model.ReportingDataset;
import org.pmiops.workbench.model.ReportingDatasetCohort;
import org.pmiops.workbench.model.ReportingDatasetConceptSet;
import org.pmiops.workbench.model.ReportingDatasetDomainIdValue;
import org.pmiops.workbench.model.ReportingInstitution;
import org.pmiops.workbench.model.ReportingUser;
import org.pmiops.workbench.model.ReportingWorkspace;
import org.pmiops.workbench.model.ReportingWorkspaceFreeTierUsage;

/** Expose handy, performant queries that don't require Dao, Entity, or Projection classes. */
public interface ReportingQueryService {
  long getQueryBatchSize();

  List<ReportingCohort> getCohorts();

  List<ReportingDataset> getDatasets();

  List<ReportingDatasetCohort> getDatasetCohorts();

  List<ReportingDatasetConceptSet> getDatasetConceptSets();

  List<ReportingDatasetDomainIdValue> getDatasetDomainIdValues();

  List<ReportingInstitution> getInstitutions();

  List<ReportingUser> getUsers();

  List<ReportingWorkspaceFreeTierUsage> getWorkspaceFreeTierUsage();

  List<ReportingWorkspace> getWorkspaces(long limit, long offset);

  default List<ReportingWorkspace> getWorkspaces() {
    return getAll(this::getWorkspaces);
  }

  default Stream<List<ReportingWorkspace>> getWorkspacesStream() {
    return getStream(this::getWorkspaces);
  }

  default <T> List<T> getBatchByIndex(BiFunction<Long, Long, List<T>> getter, long batchIndex) {
    final long offset = getQueryBatchSize() * batchIndex;
    return getter.apply(getQueryBatchSize(), offset);
  }

  /**
   * Get an iterator to batches of rows
   *
   * @param getter - method to retrieve a batch, typically a method reference against this interface
   * @param <T> - DTO type
   * @return
   */
  default <T> Iterator<List<T>> getBatchIterator(BiFunction<Long, Long, List<T>> getter) {
    return new Iterator<List<T>>() {
      private long batchIndex = 0;
      private long lastResultSetSize = -1; // first call to hasNext() should return true

      @Override
      public void remove() {
        throw new IllegalStateException();
      }

      @Override
      public void forEachRemaining(Consumer<? super List<T>> action) {
        while (hasNext()) {
          action.accept(next());
        }
      }

      /** @return */
      @Override
      public boolean hasNext() {
        if (lastResultSetSize < 0) {
          // query for one row. lastResultSetSize will be updated if there's at least one row and we
          // call next().
          final List<T> upToOneRow = getter.apply(1L, 0L);
          return !upToOneRow.isEmpty();
        } else {
          return lastResultSetSize == getQueryBatchSize();
        }
      }

      @Override
      public List<T> next() {
        final List<T> result = getBatchByIndex(getter, batchIndex++);
        lastResultSetSize = result.size();
        return result;
      }
    };
  }

  default Iterator<List<ReportingWorkspace>> getWorkspaceBatchIterator() {
    return getBatchIterator(this::getWorkspaces);
  }

  /** Use the maximum batch to get in single batch */
  default <T> List<T> getAll(BiFunction<Long, Long, List<T>> getter) {
    return getter.apply(Long.MAX_VALUE, 0L);
  }

  /**
   * Construct a Stream of batches from one of the query methods that takes a limit and offset
   *
   * @param getter - limit & offset version of query method (currently just getWorkspaces())
   * @param <T> - DTO type
   * @return
   */
  default <T> Stream<List<T>> getStream(BiFunction<Long, Long, List<T>> getter) {
    final Iterator<List<T>> batchIterator = getBatchIterator(getter);
    final Iterable<List<T>> iterable = () -> batchIterator;
    return StreamSupport.stream(iterable.spliterator(), false);
  }
}
