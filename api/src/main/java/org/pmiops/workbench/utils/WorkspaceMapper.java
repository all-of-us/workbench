package org.pmiops.workbench.utils;

import java.sql.Timestamp;
import java.util.Set;
import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.pmiops.workbench.cohortreview.CohortReviewMapper;
import org.pmiops.workbench.cohorts.CohortMapper;
import org.pmiops.workbench.conceptset.ConceptSetMapper;
import org.pmiops.workbench.dataset.DataSetMapper;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbCohort;
import org.pmiops.workbench.db.model.DbCohortReview;
import org.pmiops.workbench.db.model.DbConceptSet;
import org.pmiops.workbench.db.model.DbDataset;
import org.pmiops.workbench.db.model.DbStorageEnums;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.db.model.DbWorkspace.BillingMigrationStatus;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspace;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceAccessEntry;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceResponse;
import org.pmiops.workbench.model.CdrVersion;
import org.pmiops.workbench.model.RecentWorkspace;
import org.pmiops.workbench.model.ResearchPurpose;
import org.pmiops.workbench.model.UserRole;
import org.pmiops.workbench.model.Workspace;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.model.WorkspaceActiveStatus;
import org.pmiops.workbench.model.WorkspaceResource;
import org.pmiops.workbench.model.WorkspaceResponse;
import org.pmiops.workbench.utils.mappers.CommonMappers;

@Mapper(
    componentModel = "spring",
    collectionMappingStrategy = CollectionMappingStrategy.TARGET_IMMUTABLE,
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_DEFAULT,
    uses = {
      CdrVersionMapper.class,
      CommonMappers.class,
      CohortMapper.class,
      CohortReviewMapper.class,
      ConceptSetMapper.class,
      DataSetMapper.class,
      DbStorageEnums.class
    })
public interface WorkspaceMapper {

  @Mapping(target = "cdrVersionId", source = "dbWorkspace.cdrVersion")
  @Mapping(target = "creator", source = "fcWorkspace.createdBy")
  @Mapping(target = "etag", source = "dbWorkspace.version", qualifiedByName = "cdrVersionToEtag")
  @Mapping(target = "googleBucketName", source = "fcWorkspace.bucketName")
  @Mapping(target = "id", source = "fcWorkspace.name")
  @Mapping(target = "name", source = "dbWorkspace.name")
  @Mapping(target = "researchPurpose", source = "dbWorkspace")
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
  Workspace toApiWorkspace(DbWorkspace dbWorkspace);

  @Mapping(target = "workspace", source = "dbWorkspace")
  @Mapping(target = "accessLevel", source = "firecloudWorkspaceResponse")
  WorkspaceResponse toApiWorkspaceResponse(
      DbWorkspace dbWorkspace, FirecloudWorkspaceResponse firecloudWorkspaceResponse);

  @Mapping(target = "timeReviewed", ignore = true)
  @Mapping(target = "populationDetails", source = "specificPopulationsEnum")
  @Mapping(target = "researchOutcomeList", source = "researchOutcomeEnumSet")
  @Mapping(target = "disseminateResearchFindingList", source = "disseminateResearchEnumSet")
  @Mapping(target = "otherDisseminateResearchFindings", source = "disseminateResearchOther")
  ResearchPurpose dbWorkspaceToResearchPurpose(DbWorkspace dbWorkspace);

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
  @Mapping(target = "specificPopulationsEnum", source = "populationDetails")
  @Mapping(target = "disseminateResearchEnumSet", source = "disseminateResearchFindingList")
  @Mapping(target = "disseminateResearchOther", source = "otherDisseminateResearchFindings")
  @Mapping(target = "researchOutcomeEnumSet", source = "researchOutcomeList")

  // Normally using ignore should be frowned upon. In a merge method
  // like this one, it's unavoidable; otherwise we'd just make a straight-up translation.
  // However,
  @Mapping(target = "approved", ignore = true)
  @Mapping(target = "billingAccountName", ignore = true)
  @Mapping(target = "billingAccountType", ignore = true)
  @Mapping(target = "billingMigrationStatusEnum", ignore = true)
  @Mapping(target = "billingStatus", ignore = true)
  @Mapping(target = "cdrVersion", ignore = true)
  @Mapping(target = "cohorts", ignore = true)
  @Mapping(target = "creationTime", ignore = true)
  @Mapping(target = "creator", ignore = true)
  @Mapping(target = "dataAccessLevel", ignore = true)
  @Mapping(target = "dataAccessLevelEnum", ignore = true)
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
  @Mapping(target = "timeRequested", ignore = true)
  @Mapping(target = "version", ignore = true)
  @Mapping(target = "workspaceActiveStatusEnum", ignore = true)
  @Mapping(target = "workspaceId", ignore = true)
  @Mapping(target = "workspaceNamespace", ignore = true)
  void mergeResearchPurposeIntoWorkspace(
      @MappingTarget DbWorkspace workspace, ResearchPurpose researchPurpose);

  @Mapping(source = "dbUser", target = "creator")
  @Mapping(source = "firecloudWorkspace.name", target = "firecloudName")
  @Mapping(source = "firecloudWorkspace.workspaceId", target = "firecloudUuid")
  @Mapping(source = "workspace.creationTime", target = "creationTime")
  @Mapping(
      source = "workspace.dataAccessLevel",
      target = "dataAccessLevel") // will take care of dataAccessLevelEnum as well
  @Mapping(source = "workspace.lastModifiedTime", target = "lastModifiedTime")
  @Mapping(source = "workspace.name", target = "name")
  @Mapping(source = "workspace.namespace", target = "workspaceNamespace")
  @Mapping(source = "workspace.researchPurpose.additionalNotes", target = "additionalNotes")
  @Mapping(source = "workspace.researchPurpose.ancestry", target = "ancestry")
  @Mapping(source = "workspace.researchPurpose.anticipatedFindings", target = "anticipatedFindings")
  @Mapping(source = "workspace.researchPurpose.approved", target = "approved")
  @Mapping(source = "workspace.researchPurpose.commercialPurpose", target = "commercialPurpose")
  @Mapping(source = "workspace.researchPurpose.controlSet", target = "controlSet")
  @Mapping(
      source = "workspace.researchPurpose.diseaseFocusedResearch",
      target = "diseaseFocusedResearch")
  @Mapping(source = "workspace.researchPurpose.diseaseOfFocus", target = "diseaseOfFocus")
  @Mapping(
      source = "workspace.researchPurpose.disseminateResearchFindingList",
      target = "disseminateResearchEnumSet")
  @Mapping(
      source = "workspace.researchPurpose.disseminateResearchFindingList",
      target = "disseminateResearchSet")
  @Mapping(source = "workspace.researchPurpose.drugDevelopment", target = "drugDevelopment")
  @Mapping(source = "workspace.researchPurpose.educational", target = "educational")
  @Mapping(source = "workspace.researchPurpose.ethics", target = "ethics")
  @Mapping(source = "workspace.researchPurpose.intendedStudy", target = "intendedStudy")
  @Mapping(source = "workspace.researchPurpose.methodsDevelopment", target = "methodsDevelopment")
  @Mapping(
      source = "workspace.researchPurpose.otherPopulationDetails",
      target = "otherPopulationDetails")
  @Mapping(source = "workspace.researchPurpose.otherPurpose", target = "otherPurpose")
  @Mapping(source = "workspace.researchPurpose.otherPurposeDetails", target = "otherPurposeDetails")
  @Mapping(
      source = "workspace.researchPurpose.populationDetails",
      target = "specificPopulationsEnum")
  @Mapping(source = "workspace.researchPurpose.populationHealth", target = "populationHealth")
  @Mapping(source = "workspace.researchPurpose.reasonForAllOfUs", target = "reasonForAllOfUs")
  @Mapping(
      source = "workspace.researchPurpose.researchOutcomeList",
      target = "researchOutcomeEnumSet")
  @Mapping(source = "workspace.researchPurpose.researchOutcomeList", target = "researchOutcomeSet")
  @Mapping(source = "workspace.researchPurpose.reviewRequested", target = "reviewRequested")
  @Mapping(source = "workspace.researchPurpose.scientificApproach", target = "scientificApproach")
  @Mapping(source = "workspace.researchPurpose.socialBehavioral", target = "socialBehavioral")
  @Mapping(source = "workspace.researchPurpose.timeRequested", target = "timeRequested")
  @Mapping(source = "cdrVersion", target = "cdrVersion")
  @Mapping(source = "workspace.researchPurpose.otherDisseminateResearchFindings", target = "disseminateResearchOther")
  @Mapping(source = "dbCohorts", target = "cohorts")
  @Mapping(source = "dbDataSets", target = "dataSets")
  @Mapping(source = "workspaceActiveStatus", target = "workspaceActiveStatusEnum")
  @Mapping(source = "billingMigrationStatus", target = "billingMigrationStatusEnum")
  @Mapping(source = "cdrVersion.isDefault", target = "isDefault")
  DbWorkspace toDbWorkspace(
      Workspace workspace,
      FirecloudWorkspace firecloudWorkspace,
      DbUser dbUser,
      String billingAccountName,
      WorkspaceActiveStatus workspaceActiveStatus,
      Timestamp lastAccessedTime,
      BillingMigrationStatus billingMigrationStatus,
      CdrVersion cdrVersion,
      Set<DbCohort> dbCohorts,
      Set<DbDataSet> dbDataSets);

  @Mapping(target = "email", source = "user.username")
  @Mapping(target = "role", source = "acl")
  UserRole toApiUserRole(DbUser user, FirecloudWorkspaceAccessEntry acl);

  @Mapping(target = "workspaceId", source = "dbWorkspace.workspaceId")
  @Mapping(target = "workspaceFirecloudName", source = "dbWorkspace.firecloudName")
  @Mapping(target = "workspaceBillingStatus", source = "dbWorkspace.billingStatus")
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
  @Mapping(target = "permission", source = "accessLevel")
  @Mapping(target = "cohortReview", source = "dbCohortReview")
  // All workspaceResources have one object and all others are null.
  @Mapping(target = "cohort", ignore = true)
  @Mapping(target = "conceptSet", ignore = true)
  @Mapping(target = "dataSet", ignore = true)
  @Mapping(target = "notebook", ignore = true)
  // This should be set when the resource is set
  @Mapping(target = "modifiedTime", source = "dbCohortReview.lastModifiedTime")
  WorkspaceResource dbWorkspaceAndDbCohortReviewToWorkspaceResource(
      DbWorkspace dbWorkspace, WorkspaceAccessLevel accessLevel, DbCohortReview dbCohortReview);

  @Mapping(target = "workspaceId", source = "dbWorkspace.workspaceId")
  @Mapping(target = "workspaceFirecloudName", source = "dbWorkspace.firecloudName")
  @Mapping(target = "workspaceBillingStatus", source = "dbWorkspace.billingStatus")
  @Mapping(target = "permission", source = "accessLevel")
  @Mapping(target = "conceptSet", source = "dbConceptSet")
  // All workspaceResources have one object and all others are null.
  @Mapping(target = "cohort", ignore = true)
  @Mapping(target = "cohortReview", ignore = true)
  @Mapping(target = "dataSet", ignore = true)
  @Mapping(target = "notebook", ignore = true)
  // This should be set when the resource is set
  @Mapping(target = "modifiedTime", source = "dbConceptSet.lastModifiedTime")
  WorkspaceResource dbWorkspaceAndDbConceptSetToWorkspaceResource(
      DbWorkspace dbWorkspace, WorkspaceAccessLevel accessLevel, DbConceptSet dbConceptSet);

  @Mapping(target = "workspaceId", source = "dbWorkspace.workspaceId")
  @Mapping(target = "workspaceFirecloudName", source = "dbWorkspace.firecloudName")
  @Mapping(target = "workspaceBillingStatus", source = "dbWorkspace.billingStatus")
  @Mapping(target = "permission", source = "accessLevel")
  @Mapping(target = "dataSet", source = "dbDataset")
  // All workspaceResources have one object and all others are null.
  @Mapping(target = "cohort", ignore = true)
  @Mapping(target = "cohortReview", ignore = true)
  @Mapping(target = "conceptSet", ignore = true)
  @Mapping(target = "notebook", ignore = true)
  // This should be set when the resource is set
  @Mapping(target = "modifiedTime", source = "dbDataset.lastModifiedTime")
  WorkspaceResource dbWorkspaceAndDbDatasetToWorkspaceResource(
      DbWorkspace dbWorkspace, WorkspaceAccessLevel accessLevel, DbDataset dbDataset);
}
