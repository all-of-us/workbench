package org.pmiops.workbench.db.dao;

import com.google.common.collect.ImmutableMap;
import java.sql.Timestamp;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.persistence.SqlResultSetMapping;
import org.pmiops.workbench.db.model.DbBillingProjectBufferEntry;
import org.pmiops.workbench.db.model.DbBillingProjectBufferEntry.BufferEntryStatus;
import org.pmiops.workbench.db.model.DbStorageEnums;
import org.pmiops.workbench.db.model.DbWorkspace.BillingMigrationStatus;
import org.pmiops.workbench.model.WorkspaceActiveStatus;
import org.pmiops.workbench.utils.DaoUtils;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BillingProjectBufferEntryDao
    extends CrudRepository<DbBillingProjectBufferEntry, Long> {

  String ASSIGNING_LOCK = "ASSIGNING_LOCK";

  DbBillingProjectBufferEntry findByFireCloudProjectName(String fireCloudProjectName);

  @Query("SELECT COUNT(*) FROM DbBillingProjectBufferEntry WHERE status IN (0, 2)")
  Long getCurrentBufferSize();

  List<DbBillingProjectBufferEntry> findAllByStatusAndLastStatusChangedTimeLessThan(
      short status, Timestamp timestamp);

  DbBillingProjectBufferEntry findFirstByStatusOrderByLastSyncRequestTimeAsc(short status);

  List<DbBillingProjectBufferEntry> findTop5ByStatusOrderByLastSyncRequestTimeAsc(short status);

  DbBillingProjectBufferEntry findFirstByStatusOrderByCreationTimeAsc(short status);

  Long countByStatus(short status);

  default Map<BufferEntryStatus, Long> getCountByStatusMap() {
    return DaoUtils.getAttributeToCountMap(findAll(), DbBillingProjectBufferEntry::getStatusEnum);
  }

  @Query(value = "select status, count(billing_project_buffer_entry.billing_project_buffer_entry_id) as num_projects\n"
      + "    from billing_project_buffer_entry\n"
      + "group by status\n"
      + "order by status;", nativeQuery = true)
//  List<BillingProjectBufferEntryStatusToCountResult> computeProjectCountByStatus();
   BillingProjectBufferEntryStatusToCountResult[] computeProjectCountByStatus();

  @Query(value = "select status, count(billing_project_buffer_entry.billing_project_buffer_entry_id) as num_projects\n"
      + "    from billing_project_buffer_entry\n"
      + "group by status\n"
      + "order by status;", nativeQuery = true)
  Object[] computeProjectCountByStatus2();

  default Map<BufferEntryStatus, Long> getStatusToProjectCountMap() {
//    List<BillingProjectBufferEntryStatusToCountResult> rows = computeProjectCountByStatus();
    BillingProjectBufferEntryStatusToCountResult[] rows = computeProjectCountByStatus();

    return Arrays.stream(rows)
        .collect(ImmutableMap.toImmutableMap(
            BillingProjectBufferEntryStatusToCountResult::getStatusEnum,
            BillingProjectBufferEntryStatusToCountResult::getNumProjects));
  }

  @Query(value = "SELECT GET_LOCK('" + ASSIGNING_LOCK + "', 1)", nativeQuery = true)
  int acquireAssigningLock();

  @Query(value = "SELECT RELEASE_LOCK('" + ASSIGNING_LOCK + "')", nativeQuery = true)
  int releaseAssigningLock();

  // get Billing Projects which are ASSIGNED to Workspaces which have been DELETED and have NEW
  // Billing Migration Status.  These are ready to be garbage-collected
  default List<String> findBillingProjectsForGarbageCollection() {
    return findByStatusAndActiveStatusAndBillingMigrationStatus(
        DbStorageEnums.billingProjectBufferEntryStatusToStorage(BufferEntryStatus.ASSIGNED),
        DbStorageEnums.workspaceActiveStatusToStorage(WorkspaceActiveStatus.DELETED),
        DbStorageEnums.billingMigrationStatusToStorage(BillingMigrationStatus.NEW));
  }

  @Query(
      "SELECT p.fireCloudProjectName "
          + "FROM DbBillingProjectBufferEntry p "
          + "JOIN DbWorkspace w "
          + "ON w.workspaceNamespace = p.fireCloudProjectName "
          + "AND p.status = :billingStatus "
          + "AND w.activeStatus = :workspaceStatus "
          + "AND w.billingMigrationStatus = :migrationStatus")
  List<String> findByStatusAndActiveStatusAndBillingMigrationStatus(
      @Param("billingStatus") short billingStatus,
      @Param("workspaceStatus") short workspaceStatus,
      @Param("migrationStatus") short migrationStatus);

  /**
   * POJO result class for counting entries by status.
   *
   *  TODO(jaycarlton): see if an Immutable will work here.
   */
//  todo:  does @SqlResultSetMapping()  go here? or should  this be an  entity (again)?
  class BillingProjectBufferEntryStatusToCountResult {

    private short billingProjectStatusStorage;
    private long numProjects;

    public BillingProjectBufferEntryStatusToCountResult() {
    }

    public BillingProjectBufferEntryStatusToCountResult(short status, long numProjects) {
      this.billingProjectStatusStorage = status;
      this.numProjects = numProjects;
    }

    public void setStatus(short status) {
      this.billingProjectStatusStorage = status;
    }

    public Short getStatus() {
      return billingProjectStatusStorage;
    }

    public void setNumProjects(long numProjects) {
      this.numProjects  = numProjects;
    }

    public long getNumProjects() {
      return numProjects;
    }

    public BufferEntryStatus getStatusEnum() {
      return DbStorageEnums.billingProjectBufferEntryStatusFromStorage(billingProjectStatusStorage);
    }
  }
}
