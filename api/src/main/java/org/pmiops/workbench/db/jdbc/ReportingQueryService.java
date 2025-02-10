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
import org.pmiops.workbench.model.ReportingLeonardoAppUsage;
import org.pmiops.workbench.model.ReportingNewUserSatisfactionSurvey;
import org.pmiops.workbench.model.ReportingUser;
import org.pmiops.workbench.model.ReportingUserGeneralDiscoverySource;
import org.pmiops.workbench.model.ReportingUserPartnerDiscoverySource;
import org.pmiops.workbench.model.ReportingWorkspace;
import org.pmiops.workbench.model.ReportingWorkspaceFreeTierUsage;

/** Expose handy, performant queries that don't require Dao, Entity, or Projection classes. */
public interface ReportingQueryService {
  long getQueryBatchSize();

  List<ReportingDataset> getDatasetBatch(long limit, long offset);

  List<ReportingDatasetCohort> getDatasetCohortBatch(long limit, long offset);

  List<ReportingDatasetConceptSet> getDatasetConceptSetBatch(long limit, long offset);

  List<ReportingDatasetDomainIdValue> getDatasetDomainIdValueBatch(long limit, long offset);

  List<ReportingInstitution> getInstitutionBatch(long limit, long offset);

  List<ReportingWorkspaceFreeTierUsage> getWorkspaceFreeTierUsageBatch(long limit, long offset);

  List<ReportingWorkspace> getWorkspaceBatch(long limit, long offset);

  List<ReportingUser> getUserBatch(long limit, long offset);

  List<ReportingCohort> getCohortBatch(long limit, long offset);

  List<ReportingNewUserSatisfactionSurvey> getNewUserSatisfactionSurveyBatch(
      long limit, long offset);

  List<ReportingUserGeneralDiscoverySource> getUserGeneralDiscoverySourceBatch(
      long limit, long offset);

  List<ReportingUserPartnerDiscoverySource> getUserPartnerDiscoverySourceBatch(
      long limit, long offset);

  List<ReportingLeonardoAppUsage> getLeonardoAppUsageBatch(long limit, long offset);

  default <T> List<T> getBatchByIndex(BiFunction<Long, Long, List<T>> getter, long batchIndex) {
    final long offset = getQueryBatchSize() * batchIndex;
    return getter.apply(getQueryBatchSize(), offset);
  }

  /**
   * Get an iterator to batches of rows
   *
   * @param getter - method to retrieve a batch, typically a method reference against this interface
   * @param <T> - DTO type
   */
  default <T> Iterator<List<T>> getBatchIterator(BiFunction<Long, Long, List<T>> getter) {
    return new Iterator<>() {
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

  /**
   * Construct a Stream of batches from one of the query methods that takes a limit and offset
   *
   * @param getter - limit & offset version of query method (currently just getWorkspaces())
   * @param <T> - DTO type
   */
  default <T> Stream<List<T>> getBatchedStream(BiFunction<Long, Long, List<T>> getter) {
    final Iterator<List<T>> batchIterator = getBatchIterator(getter);
    final Iterable<List<T>> iterable = () -> batchIterator;
    return StreamSupport.stream(iterable.spliterator(), false);
  }

  int getTableRowCount(String rwbTableName);

  int getAppUsageRowCount();

  int getActiveWorkspaceCount();
}
