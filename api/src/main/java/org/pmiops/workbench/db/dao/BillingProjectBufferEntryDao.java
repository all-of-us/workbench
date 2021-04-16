package org.pmiops.workbench.db.dao;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.pmiops.workbench.db.model.DbAccessTier;
import org.pmiops.workbench.db.model.DbBillingProjectBufferEntry;
import org.pmiops.workbench.db.model.DbBillingProjectBufferEntry.BufferEntryStatus;
import org.pmiops.workbench.db.model.DbStorageEnums;
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

  @Query(
      "SELECT COUNT(*) FROM DbBillingProjectBufferEntry WHERE status IN (0, 2) AND access_tier = (:tier)")
  Long getCurrentBufferSizeForAccessTier(@Param("tier") DbAccessTier accessTier);

  // TODO: decide if we care about potential "tier bias" here; it does not take tiers into account
  List<DbBillingProjectBufferEntry> findAllByStatusAndLastStatusChangedTimeLessThan(
      short status, Timestamp timestamp);

  // a friendlier method name than the above JPA magic name
  default List<DbBillingProjectBufferEntry> findOlderThanByStatus(
      Instant inst, BufferEntryStatus status) {
    return findAllByStatusAndLastStatusChangedTimeLessThan(
        DbStorageEnums.billingProjectBufferEntryStatusToStorage(status), Timestamp.from(inst));
  }

  // TODO: decide if we care about potential "tier bias" here; it does not take tiers into account
  @Query(
      value =
          "SELECT * FROM billing_project_buffer_entry "
              + "  WHERE status = 0 " // BufferEntryStatus.CREATING
              + "  ORDER BY last_sync_request_time ASC "
              + "  LIMIT ?1",
      nativeQuery = true)
  List<DbBillingProjectBufferEntry> getCreatingEntriesToSync(int limit);

  DbBillingProjectBufferEntry findFirstByStatusAndAccessTierOrderByCreationTimeAsc(
      short status, DbAccessTier accessTier);

  // a friendlier method name than the above JPA magic name
  default DbBillingProjectBufferEntry findOldestForStatus(
      BufferEntryStatus status, DbAccessTier accessTier) {
    return findFirstByStatusAndAccessTierOrderByCreationTimeAsc(
        DbStorageEnums.billingProjectBufferEntryStatusToStorage(status), accessTier);
  }

  Long countByStatus(short status);

  interface ProjectCountByStatusAndTier {
    long getNumProjects();

    short getStatus();

    DbAccessTier getAccessTier();

    default BufferEntryStatus getStatusEnum() {
      return DbStorageEnums.billingProjectBufferEntryStatusFromStorage(getStatus());
    }
  }

  @Query(
      value =
          "select count(entry.id) as numProjects, entry.status, entry.accessTier \n"
              + "from DbBillingProjectBufferEntry entry \n"
              + "group by entry.status, entry.accessTier\n"
              + "order by entry.status, entry.accessTier")
  List<ProjectCountByStatusAndTier> getBillingBufferGaugeData();

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
}
