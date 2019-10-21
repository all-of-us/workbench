package org.pmiops.workbench.db.dao;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.pmiops.workbench.db.model.StorageEnums;
import org.pmiops.workbench.db.model.Workspace;
import org.pmiops.workbench.db.model.Workspace.BillingMigrationStatus;
import org.pmiops.workbench.model.BillingStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

/**
 * Declaration of automatic query methods for Workspaces. The methods declared here are
 * automatically interpreted by Spring Data (see README).
 *
 * <p>Use of @Query is discouraged; if desired, define aliases in WorkspaceService.
 */
public interface WorkspaceDao extends CrudRepository<Workspace, Long> {

  Workspace findByWorkspaceNamespaceAndFirecloudNameAndActiveStatus(
      String workspaceNamespace, String firecloudName, short activeStatus);

  Workspace findByWorkspaceNamespaceAndNameAndActiveStatus(
      String workspaceNamespace, String name, short activeStatus);

  @Query("SELECT distinct w.workspaceNamespace, w from Workspace w")
  Set<String> findAllWorkspaceNamespaces();

  @Query(
      "SELECT w FROM Workspace w LEFT JOIN FETCH w.cohorts c LEFT JOIN FETCH c.cohortReviews"
          + " WHERE w.workspaceNamespace = (:ns) AND w.firecloudName = (:fcName)"
          + " AND w.activeStatus = (:status)")
  Workspace findByFirecloudNameAndActiveStatusWithEagerCohorts(
      @Param("ns") String workspaceNamespace,
      @Param("fcName") String fcName,
      @Param("status") short status);

  List<Workspace> findByApprovedIsNullAndReviewRequestedTrueOrderByTimeRequested();

  List<Workspace> findAllByFirecloudUuidIn(Collection<String> firecloudUuids);

  List<Workspace> findAllByWorkspaceIdIn(Collection<Long> dbIds);

  List<Workspace> findAllByBillingMigrationStatus(Short billingMigrationStatus);

  default List<Workspace> findAllByBillingMigrationStatus(BillingMigrationStatus status) {
    return findAllByBillingMigrationStatus(StorageEnums.billingMigrationStatusToStorage(status));
  }

  default void updateBillingStatus(long workspaceId, BillingStatus status) {
    Workspace toUpdate = findOne(workspaceId);
    toUpdate.setBillingStatus(status);
    save(toUpdate);
  }
}
