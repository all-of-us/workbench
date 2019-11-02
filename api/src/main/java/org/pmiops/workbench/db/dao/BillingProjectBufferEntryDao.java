package org.pmiops.workbench.db.dao;

import java.sql.Timestamp;
import java.util.List;
import org.pmiops.workbench.db.model.DbBillingProjectBufferEntry;
import org.pmiops.workbench.db.model.DbBillingProjectBufferEntry.BillingProjectBufferStatus;
import org.pmiops.workbench.db.model.StorageEnums;
import org.pmiops.workbench.db.model.DbWorkspace.BillingMigrationStatus;
import org.pmiops.workbench.model.WorkspaceActiveStatus;
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

  DbBillingProjectBufferEntry findFirstByStatusOrderByCreationTimeAsc(short status);

  Long countByStatus(short status);

  @Query(value = "SELECT GET_LOCK('" + ASSIGNING_LOCK + "', 1)", nativeQuery = true)
  int acquireAssigningLock();

  @Query(value = "SELECT RELEASE_LOCK('" + ASSIGNING_LOCK + "')", nativeQuery = true)
  int releaseAssigningLock();

  // get Billing Projects which are ASSIGNED to Workspaces which have been DELETED and have NEW
  // Billing Migration Status.  These are ready to be garbage-collected
  default List<String> findBillingProjectsForGarbageCollection() {
    return findByStatusAndActiveStatusAndBillingMigrationStatus(
        StorageEnums.billingProjectBufferStatusToStorage(BillingProjectBufferStatus.ASSIGNED),
        StorageEnums.workspaceActiveStatusToStorage(WorkspaceActiveStatus.DELETED),
        StorageEnums.billingMigrationStatusToStorage(BillingMigrationStatus.NEW));
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
}
