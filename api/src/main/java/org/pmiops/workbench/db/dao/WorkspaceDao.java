package org.pmiops.workbench.db.dao;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.pmiops.workbench.db.model.DbAccessTier;
import org.pmiops.workbench.db.model.DbStorageEnums;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.model.BillingStatus;
import org.pmiops.workbench.model.WorkspaceActiveStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

/**
 * Declaration of automatic query methods for Workspaces. The methods declared here are
 * automatically interpreted by Spring Data (see README).
 *
 * <p>Use of @Query is discouraged; if desired, define aliases in WorkspaceService.
 */
public interface WorkspaceDao extends CrudRepository<DbWorkspace, Long>, WorkspaceDaoCustom {

  Logger log = Logger.getLogger(WorkspaceDao.class.getName());

  default DbWorkspace get(String ns, String firecloudName) {
    return findByWorkspaceNamespaceAndFirecloudNameAndActiveStatus(
        ns,
        firecloudName,
        DbStorageEnums.workspaceActiveStatusToStorage(WorkspaceActiveStatus.ACTIVE));
  }

  default DbWorkspace getRequired(String ns, String firecloudName) {
    DbWorkspace workspace = get(ns, firecloudName);
    if (workspace == null) {
      throw new NotFoundException(String.format("DbWorkspace %s/%s not found.", ns, firecloudName));
    }
    return workspace;
  }

  default DbWorkspace getRequiredWithCohorts(String ns, String firecloudName) {
    DbWorkspace workspace =
        findByFirecloudNameAndActiveStatusWithEagerCohorts(
            ns,
            firecloudName,
            DbStorageEnums.workspaceActiveStatusToStorage(WorkspaceActiveStatus.ACTIVE));
    if (workspace == null) {
      throw new NotFoundException(String.format("DbWorkspace %s/%s not found.", ns, firecloudName));
    }
    return workspace;
  }

  default Optional<DbWorkspace> getByNamespace(String workspaceNamespace) {
    return findFirstByWorkspaceNamespaceAndActiveStatusOrderByLastModifiedTimeDesc(
        workspaceNamespace,
        DbStorageEnums.workspaceActiveStatusToStorage(WorkspaceActiveStatus.ACTIVE));
  }

  default Optional<DbWorkspace> getByGoogleProject(String googleProject) {
    return findFirstByGoogleProjectAndActiveStatusOrderByLastModifiedTimeDesc(
        googleProject, DbStorageEnums.workspaceActiveStatusToStorage(WorkspaceActiveStatus.ACTIVE));
  }

  DbWorkspace findByWorkspaceNamespaceAndFirecloudNameAndActiveStatus(
      String workspaceNamespace, String firecloudName, short activeStatus);

  @Query("SELECT distinct w.workspaceNamespace, w from DbWorkspace w")
  Set<String> findAllWorkspaceNamespaces();

  @Query(
      "SELECT w FROM DbWorkspace w LEFT JOIN FETCH w.cohorts c LEFT JOIN FETCH c.cohortReviews"
          + " WHERE w.workspaceNamespace = (:ns) AND w.firecloudName = (:fcName)"
          + " AND w.activeStatus = (:status)")
  DbWorkspace findByFirecloudNameAndActiveStatusWithEagerCohorts(
      @Param("ns") String workspaceNamespace,
      @Param("fcName") String fcName,
      @Param("status") short status);

  List<DbWorkspace> findAllByFirecloudUuidIn(Collection<String> firecloudUuids);

  default List<DbWorkspace> findActiveByFirecloudUuidIn(Collection<String> firecloudUuids) {
    return findAllByFirecloudUuidIn(firecloudUuids).stream()
        .filter(DbWorkspace::isActive)
        .collect(Collectors.toList());
  }

  List<DbWorkspace> findAllByWorkspaceIdIn(Collection<Long> dbIds);

  List<DbWorkspace> findAllByGoogleProjectIn(Collection<String> googleProjects);

  default Optional<DbWorkspace> findActiveByWorkspaceId(long workspaceId) {
    DbWorkspace workspace = findById(workspaceId).orElse(null);
    if (workspace == null || !workspace.isActive()) {
      return Optional.empty();
    }
    return Optional.of(workspace);
  }

  List<DbWorkspace> findAllByWorkspaceNamespace(String workspaceNamespace);

  Optional<DbWorkspace> findFirstByWorkspaceNamespaceOrderByFirecloudNameAsc(
      String workspaceNamespace);

  Optional<DbWorkspace> findFirstByWorkspaceNamespaceAndActiveStatusOrderByLastModifiedTimeDesc(
      String workspaceNamespace, short activeStatus);

  Optional<DbWorkspace> findFirstByGoogleProjectAndActiveStatusOrderByLastModifiedTimeDesc(
      String googleProject, short activeStatus);

  DbWorkspace findDbWorkspaceByWorkspaceId(long workspaceId);

  Set<DbWorkspace> findAllByCreator(DbUser user);

  default void updateBillingStatus(long workspaceId, BillingStatus status) {
    DbWorkspace toUpdate =
        findById(workspaceId)
            .orElseThrow(
                () ->
                    new NotFoundException(
                        String.format("DbWorkspace %s does not exist", workspaceId)));
    toUpdate.setBillingStatus(status);
    save(toUpdate);
  }

  @Query(
      "SELECT w.creator FROM DbWorkspace w "
          + "WHERE w.billingStatus = (:status) AND w.billingAccountName in (:billingAccountNames) AND w.creator in (:creators)")
  Set<DbUser> findCreatorsByBillingStatusAndBillingAccountNameIn(
      @Param("status") BillingStatus status,
      @Param("billingAccountNames") List<String> billingAccountNames,
      @Param("creators") Set<DbUser> creators);

  @Query(
      "SELECT COUNT(workspace.workspaceId) AS workspaceCount, workspace.activeStatus AS activeStatus, tier AS tier "
          + "FROM DbWorkspace workspace "
          + "JOIN DbCdrVersion version ON workspace.cdrVersion.cdrVersionId = version.cdrVersionId "
          + "JOIN DbAccessTier tier ON version.accessTier.accessTierId = tier.accessTierId "
          + "GROUP BY workspace.activeStatus, tier "
          + "ORDER BY workspace.activeStatus, tier")
  List<WorkspaceCountByActiveStatusAndTier> getWorkspaceCountGaugeData();

  interface WorkspaceCountByActiveStatusAndTier {
    Long getWorkspaceCount();

    Short getActiveStatus();

    DbAccessTier getTier();

    default WorkspaceActiveStatus getActiveStatusEnum() {
      return DbStorageEnums.workspaceActiveStatusFromStorage(getActiveStatus());
    }
  }

  interface WorkspaceCostView {
    Long getWorkspaceId();

    String getGoogleProject();

    Long getCreatorId();

    Double getFreeTierCost();

    Timestamp getFreeTierLastUpdated();

    Timestamp getWorkspaceLastUpdated();

    Short getActiveStatus();
  }

  @Query(
      "SELECT w.workspaceId AS workspaceId, "
          + "w.googleProject AS googleProject, "
          + "w.creator.id AS creatorId, "
          + "f.cost AS freeTierCost, "
          + "f.lastUpdateTime AS freeTierLastUpdated, "
          + "w.lastModifiedTime AS workspaceLastUpdated, "
          + "w.activeStatus AS activeStatus "
          + "FROM DbWorkspace w "
          + "LEFT JOIN DbWorkspaceFreeTierUsage f ON w.workspaceId = f.workspace.id "
          + "WHERE w.creator IS NOT NULL "
          + "AND w.creator in (:creators)")
  List<WorkspaceCostView> getWorkspaceCostViews(@Param("creators") Set<DbUser> creators);

  @Query(
      "SELECT w.googleProject AS googleProject "
          + "FROM DbWorkspace w "
          + "WHERE w.creator.userId = (:creatorid)")
  List<String> getGoogleProjectForUser(@Param("creatorid") long creatorId);
}
