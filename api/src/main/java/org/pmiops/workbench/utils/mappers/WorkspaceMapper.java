package org.pmiops.workbench.utils.mappers;

import static org.mapstruct.NullValuePropertyMappingStrategy.SET_TO_DEFAULT;

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.pmiops.workbench.cohortreview.mapper.CohortReviewMapper;
import org.pmiops.workbench.cohorts.CohortMapper;
import org.pmiops.workbench.conceptset.mapper.ConceptSetMapper;
import org.pmiops.workbench.dataset.mapper.DataSetMapper;
import org.pmiops.workbench.db.model.DbCohort;
import org.pmiops.workbench.db.model.DbDataset;
import org.pmiops.workbench.db.model.DbStorageEnums;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspace;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceResponse;
import org.pmiops.workbench.model.CdrVersion;
import org.pmiops.workbench.model.CohortReview;
import org.pmiops.workbench.model.ConceptSet;
import org.pmiops.workbench.model.RecentWorkspace;
import org.pmiops.workbench.model.ResearchPurpose;
import org.pmiops.workbench.model.Workspace;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.model.WorkspaceResource;
import org.pmiops.workbench.model.WorkspaceResponse;

@Mapper(
    config = MapStructConfig.class,
    collectionMappingStrategy = CollectionMappingStrategy.TARGET_IMMUTABLE,
    uses = {
      CohortMapper.class,
      CohortReviewMapper.class,
      CommonMappers.class,
      ConceptSetMapper.class,
      DataSetMapper.class,
      DbStorageEnums.class,
      FirecloudMapper.class
    })
public interface WorkspaceMapper {

  @Mapping(target = "researchPurpose", source = "dbWorkspace")
  @Mapping(target = "etag", source = "dbWorkspace.version", qualifiedByName = "cdrVersionToEtag")
  @Mapping(target = "name", source = "dbWorkspace.name")
  @Mapping(target = "id", source = "fcWorkspace.name")
  @Mapping(target = "googleBucketName", source = "fcWorkspace.bucketName")
  @Mapping(target = "creator", source = "fcWorkspace.createdBy")
  @Mapping(target = "cdrVersionId", source = "dbWorkspace.cdrVersion")
  @Mapping(target = "accessTierShortName", source = "dbWorkspace.cdrVersion.accessTier.shortName")
  @Mapping(target = "googleProject", source = "fcWorkspace.googleProject")
  Workspace toApiWorkspace(DbWorkspace dbWorkspace, FirecloudWorkspace fcWorkspace);

  @Mapping(target = "cdrVersionId", source = "cdrVersion")
  @Mapping(target = "creator", source = "creator.username")
  @Mapping(target = "etag", source = "version", qualifiedByName = "cdrVersionToEtag")
  @Mapping(
      target = "googleBucketName",
      ignore = true) // available via toApiWorkspace(DbWorkspace dbWorkspace, FirecloudWorkspace
  // fcWorkspace)
  @Mapping(target = "id", source = "firecloudName")
  @Mapping(target = "namespace", source = "workspaceNamespace")
  @Mapping(target = "researchPurpose", source = "dbWorkspace")
  @Mapping(target = "accessTierShortName", source = "dbWorkspace.cdrVersion.accessTier.shortName")
  Workspace toApiWorkspace(DbWorkspace dbWorkspace);

  WorkspaceResponse toApiWorkspaceResponse(Workspace workspace, String accessLevel);

  default WorkspaceResponse toApiWorkspaceResponse(
      DbWorkspace dbWorkspace, FirecloudWorkspaceResponse firecloudWorkspaceResponse) {
    return toApiWorkspaceResponse(
        toApiWorkspace(dbWorkspace, firecloudWorkspaceResponse.getWorkspace()),
        firecloudWorkspaceResponse.getAccessLevel());
  };

  @Mapping(target = "timeReviewed", ignore = true)
  @Mapping(target = "populationDetails", source = "specificPopulationsEnum")
  @Mapping(target = "researchOutcomeList", source = "researchOutcomeEnumSet")
  @Mapping(target = "disseminateResearchFindingList", source = "disseminateResearchEnumSet")
  @Mapping(target = "otherDisseminateResearchFindings", source = "disseminateResearchOther")
  ResearchPurpose workspaceToResearchPurpose(DbWorkspace dbWorkspace);

  @Mapping(target = "workspace", source = "dbWorkspace")
  RecentWorkspace toApiRecentWorkspace(DbWorkspace dbWorkspace, WorkspaceAccessLevel accessLevel);

  /**
   * This method was written I think before we realized we could have multiple input arguments.
   *
   * @deprecated
   * @param workspace
   * @param researchPurpose
   */
  @Deprecated
  @Mapping(
      target = "specificPopulationsEnum",
      source = "populationDetails",
      nullValuePropertyMappingStrategy = SET_TO_DEFAULT)
  @Mapping(
      target = "disseminateResearchEnumSet",
      source = "disseminateResearchFindingList",
      nullValuePropertyMappingStrategy = SET_TO_DEFAULT)
  @Mapping(target = "disseminateResearchOther", source = "otherDisseminateResearchFindings")
  @Mapping(
      target = "researchOutcomeEnumSet",
      source = "researchOutcomeList",
      nullValuePropertyMappingStrategy = SET_TO_DEFAULT)

  // This method isn't a full conversion, so we need to mask out the values that don't
  // get set here.
  @Mapping(target = "approved", ignore = true)
  @Mapping(target = "billingAccountName", ignore = true)
  @Mapping(target = "billingAccountType", ignore = true)
  @Mapping(target = "billingMigrationStatusEnum", ignore = true)
  @Mapping(target = "billingStatus", ignore = true)
  @Mapping(target = "cdrVersion", ignore = true)
  @Mapping(target = "cohorts", ignore = true)
  @Mapping(target = "conceptSets", ignore = true)
  @Mapping(target = "creationTime", ignore = true)
  @Mapping(target = "creator", ignore = true)
  @Mapping(target = "dataSets", ignore = true)
  @Mapping(target = "disseminateResearchSet", ignore = true)
  @Mapping(target = "firecloudName", ignore = true)
  @Mapping(target = "firecloudUuid", ignore = true)
  @Mapping(target = "lastAccessedTime", ignore = true)
  @Mapping(target = "lastModifiedTime", ignore = true)
  @Mapping(target = "name", ignore = true)
  @Mapping(target = "published", ignore = true)
  @Mapping(target = "researchOutcomeSet", ignore = true)
  @Mapping(target = "reviewRequested", ignore = true)
  @Mapping(target = "needsResearchPurposeReviewPrompt", ignore = true)
  @Mapping(target = "timeRequested", ignore = true)
  @Mapping(target = "version", ignore = true)
  @Mapping(target = "workspaceActiveStatusEnum", ignore = true)
  @Mapping(target = "workspaceId", ignore = true)
  @Mapping(target = "workspaceNamespace", ignore = true)
  @Mapping(target = "googleProject", ignore = true)
  void mergeResearchPurposeIntoWorkspace(
      @MappingTarget DbWorkspace workspace, ResearchPurpose researchPurpose);

  @Mapping(target = "workspaceId", source = "dbWorkspace.workspaceId")
  @Mapping(target = "workspaceFirecloudName", source = "dbWorkspace.firecloudName")
  @Mapping(target = "workspaceBillingStatus", source = "dbWorkspace.billingStatus")
  @Mapping(target = "cdrVersionId", source = "dbWorkspace.cdrVersion")
  @Mapping(target = "permission", source = "accessLevel")
  @Mapping(target = "cohort", source = "dbCohort")
  // All workspaceResources have one object and all others are null.
  @Mapping(target = "cohortReview", ignore = true)
  @Mapping(target = "conceptSet", ignore = true)
  @Mapping(target = "dataSet", ignore = true)
  @Mapping(target = "notebook", ignore = true)
  // This should be set when the resource is set
  @Mapping(target = "modifiedTime", source = "dbCohort.lastModifiedTime")
  WorkspaceResource dbWorkspaceAndDbCohortToWorkspaceResource(
      DbWorkspace dbWorkspace, WorkspaceAccessLevel accessLevel, DbCohort dbCohort);

  @Mapping(target = "workspaceId", source = "dbWorkspace.workspaceId")
  @Mapping(target = "workspaceFirecloudName", source = "dbWorkspace.firecloudName")
  @Mapping(target = "workspaceBillingStatus", source = "dbWorkspace.billingStatus")
  @Mapping(target = "cdrVersionId", source = "dbWorkspace.cdrVersion")
  @Mapping(target = "permission", source = "accessLevel")
  @Mapping(target = "cohortReview", source = "cohortReview")
  // All workspaceResources have one object and all others are null.
  @Mapping(target = "cohort", ignore = true)
  @Mapping(target = "conceptSet", ignore = true)
  @Mapping(target = "dataSet", ignore = true)
  @Mapping(target = "notebook", ignore = true)
  // This should be set when the resource is set
  @Mapping(target = "modifiedTime", source = "cohortReview.lastModifiedTime")
  WorkspaceResource dbWorkspaceAndDbCohortReviewToWorkspaceResource(
      DbWorkspace dbWorkspace, WorkspaceAccessLevel accessLevel, CohortReview cohortReview);

  @Mapping(target = "workspaceId", source = "dbWorkspace.workspaceId")
  @Mapping(target = "workspaceFirecloudName", source = "dbWorkspace.firecloudName")
  @Mapping(target = "workspaceBillingStatus", source = "dbWorkspace.billingStatus")
  @Mapping(target = "cdrVersionId", source = "dbWorkspace.cdrVersion")
  @Mapping(target = "permission", source = "accessLevel")
  @Mapping(target = "conceptSet", source = "conceptSet")
  // All workspaceResources have one object and all others are null.
  @Mapping(target = "cohort", ignore = true)
  @Mapping(target = "cohortReview", ignore = true)
  @Mapping(target = "dataSet", ignore = true)
  @Mapping(target = "notebook", ignore = true)
  // This should be set when the resource is set
  @Mapping(target = "modifiedTime", source = "conceptSet.lastModifiedTime")
  WorkspaceResource dbWorkspaceSetToWorkspaceResource(
      DbWorkspace dbWorkspace, WorkspaceAccessLevel accessLevel, ConceptSet conceptSet);

  @Mapping(target = "workspaceId", source = "dbWorkspace.workspaceId")
  @Mapping(target = "workspaceFirecloudName", source = "dbWorkspace.firecloudName")
  @Mapping(target = "workspaceBillingStatus", source = "dbWorkspace.billingStatus")
  @Mapping(target = "cdrVersionId", source = "dbWorkspace.cdrVersion")
  @Mapping(target = "permission", source = "accessLevel")
  @Mapping(target = "dataSet", source = "dbDataset", qualifiedByName = "dbModelToClientLight")
  // All workspaceResources have one object and all others are null.
  @Mapping(target = "cohort", ignore = true)
  @Mapping(target = "cohortReview", ignore = true)
  @Mapping(target = "conceptSet", ignore = true)
  @Mapping(target = "notebook", ignore = true)
  // This should be set when the resource is set
  @Mapping(target = "modifiedTime", source = "dbDataset.lastModifiedTime")
  WorkspaceResource dbWorkspaceAndDbDatasetToWorkspaceResource(
      DbWorkspace dbWorkspace, WorkspaceAccessLevel accessLevel, DbDataset dbDataset);

  default String cdrVersionId(CdrVersion cdrVersion) {
    return String.valueOf(cdrVersion.getCdrVersionId());
  }
}
