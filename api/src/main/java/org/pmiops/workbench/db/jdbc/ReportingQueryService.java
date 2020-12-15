package org.pmiops.workbench.db.jdbc;

import java.util.Iterator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
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
    return getWorkspaces(Long.MAX_VALUE, 0);
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
}
