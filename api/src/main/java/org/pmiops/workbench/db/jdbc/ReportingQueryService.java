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

  default <T> Iterator<List<T>> getBatchIterator(BiFunction<Long, Long, List<T>> getter) {
    return new Iterator<List<T>>() {
      private long batchIndex = 0;

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

      @Override
      public boolean hasNext() {
        return !getBatchByIndex(getter, batchIndex).isEmpty();
      }

      @Override
      public List<T> next() {
        return getBatchByIndex(getter, batchIndex++);
      }
    };
  }

  default Iterator<List<ReportingWorkspace>> getWorkspaceBatchIterator() {
    return getBatchIterator(this::getWorkspaces);
  }

  // Use the maximum batch to get in single batch
  default <T> List<T> getAll(BiFunction<Long, Long, List<T>> getter) {
    return getter.apply(Long.MAX_VALUE, 0L);
  }

  default <T> Stream<List<T>> getStream(BiFunction<Long, Long, List<T>> getter) {
    final Iterator<List<T>> batchIterator = getBatchIterator(getter);
    final Iterable<List<T>> iterable = () -> batchIterator;
    return StreamSupport.stream(iterable.spliterator(), false);
  }
}
