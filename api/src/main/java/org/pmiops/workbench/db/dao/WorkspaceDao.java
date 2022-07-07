package org.pmiops.workbench.db.dao;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
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

  Set<DbWorkspace> findAllByActiveStatus(short activeStatus);

  List<DbWorkspace> findAllByNeedsResearchPurposeReviewPrompt(short researchPurposeReviewed);

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
          + "WHERE w.billingStatus = (:status) AND w.billingAccountName in (:billingAccountNames)")
  Set<DbUser> findAllCreatorsByBillingStatusAndBillingAccountNameIn(
      @Param("status") BillingStatus status,
      @Param("billingAccountNames") List<String> billingAccountNames);

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

  // a speculative view of fields I'll need when dumping the whole workspace table in
  // OrphanedProjects
  @Query(
      nativeQuery = true,
      value =
          "SELECT workspace.workspace_id AS dbId, workspace.name, workspace.workspace_namespace AS namespace, "
              + "workspace.google_project AS googleProject, workspace.active_status AS activeStatus, "
              + "workspace.creator_id AS creatorId, workspace.creation_time AS creationTime, "
              + "workspace.last_modified_time AS lastModifiedTime, "
              + "access_tier.short_name AS tierName "
              + "FROM workspace "
              + "JOIN cdr_version ON workspace.cdr_version_id = cdr_version.cdr_version_id "
              + "JOIN access_tier ON cdr_version.access_tier = access_tier.access_tier_id "
              + "WHERE active_status = 0 " // ACTIVE
              + "ORDER BY workspace.google_project")
  List<FieldsForOrphanChecking> getActiveWorkspacesForOrphanChecking();

  interface FieldsForOrphanChecking {
    long getDbId();

    String getName();

    String getNamespace();

    String getGoogleProject();

    Short getActiveStatus();

    String getTierName();

    long getCreatorId();

    Timestamp getCreationTime();

    Timestamp getLastModifiedTime();

    default WorkspaceActiveStatus getActiveStatusEnum() {
      return DbStorageEnums.workspaceActiveStatusFromStorage(getActiveStatus());
    }

    default String logString() {
      return String.format(
          "WS: db-id %d, name %s, namespace %s, googProj %s, tier %s, status %s",
          getDbId(),
          getName(),
          getNamespace(),
          getGoogleProject(),
          getTierName(),
          getActiveStatusEnum());
    }
  }
}
