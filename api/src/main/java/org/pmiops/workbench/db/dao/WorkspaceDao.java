package org.pmiops.workbench.db.dao;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import org.pmiops.workbench.db.model.DbStorageEnums;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.NotFoundException;
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

  DbWorkspace findByWorkspaceNamespaceAndFirecloudNameAndActiveStatus(
      String workspaceNamespace, String firecloudName, short activeStatus);

  default DbWorkspace getRequired(String ns, String firecloudName) {
    DbWorkspace workspace =
        findByWorkspaceNamespaceAndFirecloudNameAndActiveStatus(
            ns,
            firecloudName,
            DbStorageEnums.workspaceActiveStatusToStorage(WorkspaceActiveStatus.ACTIVE));
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

  List<DbWorkspace> getByWorkspaceNamespaceIn(Collection<String> workspaceNamespaces);

  default Optional<DbWorkspace> getByGoogleProject(String googleProject) {
    return findFirstByGoogleProjectAndActiveStatusOrderByLastModifiedTimeDesc(
        googleProject, DbStorageEnums.workspaceActiveStatusToStorage(WorkspaceActiveStatus.ACTIVE));
  }

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
    return findAllByFirecloudUuidIn(firecloudUuids).stream().filter(DbWorkspace::isActive).toList();
  }

  List<DbWorkspace> findAllByWorkspaceIdIn(Collection<Long> dbIds);

  List<DbWorkspace> findAllByGoogleProjectIn(Collection<String> googleProjects);

  default Optional<DbWorkspace> findActiveByWorkspaceId(long workspaceId) {
    return findById(workspaceId).filter(DbWorkspace::isActive);
  }

  List<DbWorkspace> findAllByWorkspaceNamespace(String workspaceNamespace);

  Optional<DbWorkspace> findFirstByWorkspaceNamespaceOrderByFirecloudNameAsc(
      String workspaceNamespace);

  Optional<DbWorkspace> findFirstByWorkspaceNamespaceAndActiveStatusOrderByLastModifiedTimeDesc(
      String workspaceNamespace, short activeStatus);

  Optional<DbWorkspace> findFirstByGoogleProjectAndActiveStatusOrderByLastModifiedTimeDesc(
      String googleProject, short activeStatus);

  Set<DbWorkspace> findAllByCreator(DbUser user);

  @Query(
      "SELECT w.creator FROM DbWorkspace w "
          + "JOIN DbUserInitialCreditsExpiration uice ON w.creator = uice.user "
          + "WHERE w.billingAccountName in (:initialCreditAccountNames) AND w.creator in (:creators) "
          + "AND w.initialCreditsExhausted = false "
          + "AND uice.expirationTime > CURRENT_TIMESTAMP")
  Set<DbUser> findCreatorsByActiveInitialCredits(
      @Param("initialCreditAccountNames") List<String> initialCreditAccountNames,
      @Param("creators") Set<DbUser> creators);

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
      "SELECT DISTINCT(w.googleProject) AS googleProject "
          + "FROM DbWorkspace w "
          + "WHERE w.creator.userId in (:creatoridList)")
  Set<String> getGoogleProjectForUserList(@Param("creatoridList") List<Long> creatorIdList);
}
