package org.pmiops.workbench.db.dao;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.pmiops.workbench.db.dao.projection.PrjWorkspace;
import org.pmiops.workbench.db.model.DbStorageEnums;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.db.model.DbWorkspace.BillingMigrationStatus;
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
public interface WorkspaceDao extends CrudRepository<DbWorkspace, Long> {

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

  List<DbWorkspace> findByApprovedIsNullAndReviewRequestedTrueOrderByTimeRequested();

  List<DbWorkspace> findAllByFirecloudUuidIn(Collection<String> firecloudUuids);

  List<DbWorkspace> findAllByWorkspaceIdIn(Collection<Long> dbIds);

  List<DbWorkspace> findAllByWorkspaceNamespace(String workspaceNamespace);

  Optional<DbWorkspace> findFirstByWorkspaceNamespaceOrderByFirecloudNameAsc(
      String workspaceNamespace);

  Optional<DbWorkspace> findFirstByWorkspaceNamespaceAndActiveStatusOrderByLastModifiedTimeDesc(
      String workspaceNamespace, short activeStatus);

  List<DbWorkspace> findAllByBillingMigrationStatus(Short billingMigrationStatus);

  DbWorkspace findDbWorkspaceByWorkspaceId(long workspaceId);

  Set<DbWorkspace> findAllByCreator(DbUser user);

  List<DbWorkspace> findAllByNeedsResearchPurposeReviewPrompt(short researchPurposeReviewed);

  default List<DbWorkspace> findAllByBillingMigrationStatus(BillingMigrationStatus status) {
    return findAllByBillingMigrationStatus(DbStorageEnums.billingMigrationStatusToStorage(status));
  }

  default void updateBillingStatus(long workspaceId, BillingStatus status) {
    DbWorkspace toUpdate = findOne(workspaceId);
    toUpdate.setBillingStatus(status);
    save(toUpdate);
  }

  @Query("SELECT w.creator FROM DbWorkspace w WHERE w.billingStatus = (:status)")
  Set<DbUser> findAllCreatorsByBillingStatus(@Param("status") BillingStatus status);

  @Query(
      "SELECT activeStatus, dataAccessLevel, COUNT(workspaceId) FROM DbWorkspace "
          + "GROUP BY activeStatus, dataAccessLevel ORDER BY activeStatus, dataAccessLevel")
  List<ActiveStatusAndDataAccessLevelToCountResult> getActiveStatusAndDataAccessLevelToCount();

  List<DbWorkspace> findAllByActiveStatusIn(Short workspaceActiveStatusOrdinal);

  interface ActiveStatusAndDataAccessLevelToCountResult {
    Short getWorkspaceActiveStatus();

    Short getDataAccessLevel();

    Long getWorkspaceCount();
  }
  @Query("SELECT\n"
      + "  w.activeStatus,\n"
      + "  w.billingAccountName,\n"
      + "  w.billingAccountType,\n"
      + "  w.billingMigrationStatus,\n"
      + "  w.billingStatus,\n"
      + "  w.cdrVersion.cdrVersionId AS cdrVersionId,\n"
      + "  w.creationTime,\n"
      + "  w.creator.userId AS creatorId,\n"
      + "  w.dataAccessLevel,\n"
      + "  w.disseminateResearchOther,\n"
      + "  w.firecloudName,\n"
      + "  w.firecloudUuid,\n"
      + "  w.lastAccessedTime,\n"
      + "  w.lastModifiedTime,\n"
      + "  w.name,\n"
      + "  w.needsResearchPurposeReviewPrompt AS needsRpReviewPrompt,\n"
      + "  w.published,\n"
      + "  w.additionalNotes AS rpAdditionalNotes,\n"
      + "  w.rpAncestry,\n"
      + "  w.rpAnticipatedFindings,\n"
      + "  w.rpApproved,\n"
      + "  w.rpCommercialPurpose,\n"
      + "  w.rpControlSet,\n"
      + "  w.rpDiseaseFocusedResearch,\n"
      + "  w.rpDiseaseOfFocus,\n"
      + "  w.rpDrugDevelopment,\n"
      + "  w.rpEducational,\n"
      + "  w.rpEthics,\n"
      + "  w.rpIntendedStudy,\n"
      + "  w.rpMethodsDevelopment,\n"
      + "  w.rpOtherPopulationDetails,\n"
      + "  w.rpOtherPurpose,\n"
      + "  w.rpOtherPurposeDetails,\n"
      + "  w.rpPopulation,\n"
      + "  w.rpPopulationHealth,\n"
      + "  w.rpReasonForAllOfUs,\n"
      + "  w.rpReviewRequested,\n"
      + "  w.rpScientificApproach,\n"
      + "  w.rpSocialBehavioral,\n"
      + "  w.rpTimeRequested,\n"
      + "  w.version,\n"
      + "  w.workspaceId,\n"
      + "  w.workspaceNamespace\n"
      + "FROM DbWorkspace w\n"
      + "WHERE w.activeStatus = 0")
  List<PrjWorkspace> getReportingWorkspaces();
}
