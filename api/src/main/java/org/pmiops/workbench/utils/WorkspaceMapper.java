package org.pmiops.workbench.utils;

import java.sql.Timestamp;
import java.util.Set;
import org.mapstruct.AfterMapping;
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

  @Mapping(source = "additionalNotes", target = "additionalNotes")
  @Mapping(source = "ancestry", target = "ancestry")
  @Mapping(source = "anticipatedFindings", target = "anticipatedFindings")
  @Mapping(source = "approved", target = "approved", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_DEFAULT)
  @Mapping(source = "commercialPurpose", target = "commercialPurpose")
  @Mapping(source = "controlSet", target = "controlSet")
  @Mapping(source = "diseaseFocusedResearch",      target = "diseaseFocusedResearch")
  @Mapping(source = "diseaseOfFocus", target = "diseaseOfFocus")
  @Mapping(source = "disseminateResearchFindingList",      target = "disseminateResearchSet")
  @Mapping(source = "drugDevelopment", target = "drugDevelopment")
  @Mapping(source = "educational", target = "educational")
  @Mapping(source = "ethics", target = "ethics")
  @Mapping(source = "intendedStudy", target = "intendedStudy")
  @Mapping(source = "methodsDevelopment", target = "methodsDevelopment")
  @Mapping(source = "otherDisseminateResearchFindings", target = "disseminateResearchOther")
  @Mapping(source = "otherPopulationDetails",      target = "otherPopulationDetails")
  @Mapping(source = "otherPurpose", target = "otherPurpose")
  @Mapping(source = "otherPurposeDetails", target = "otherPurposeDetails")
  @Mapping(source = "populationDetails", target = "specificPopulationsEnum")
  @Mapping(source = "populationHealth", target = "populationHealth")
  @Mapping(source = "reasonForAllOfUs", target = "reasonForAllOfUs")
  @Mapping(source = "researchOutcomeList", target = "researchOutcomeSet")
  @Mapping(source = "reviewRequested", target = "reviewRequested", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_DEFAULT)
  @Mapping(source = "scientificApproach", target = "scientificApproach")
  @Mapping(source = "socialBehavioral", target = "socialBehavioral")
  @Mapping(source = "timeRequested", target = "timeRequested")
  @Mapping(target = "billingAccountName", ignore = true) // not updated by Research Purpose
  @Mapping(target = "billingAccountType", ignore = true) // not updated by Research Purpose
  @Mapping(target = "billingMigrationStatusEnum", ignore = true) // not updated by Research Purpose
  @Mapping(target = "billingStatus", ignore = true) // not updated by Research Purpose
  @Mapping(target = "cdrVersion", ignore = true) // not updated by Research Purpose
  @Mapping(target = "cohorts", ignore = true) // not updated by Research Purpose
  @Mapping(target = "creationTime", ignore = true) // not updated by Research Purpose
  @Mapping(target = "creator", ignore = true) // not updated by Research Purpose
  @Mapping(target = "dataAccessLevel", ignore = true) // not updated by Research Purpose
  @Mapping(target = "dataAccessLevelEnum", ignore = true) // not updated by Research Purpose
  @Mapping(target = "dataSets", ignore = true) // not updated by Research Purpose
  @Mapping(target = "disseminateResearchEnumSet", ignore = true) // transient
  @Mapping(target = "firecloudName", ignore = true) // not updated by Research Purpose
  @Mapping(target = "firecloudUuid", ignore = true) // not updated by Research Purpose
  @Mapping(target = "lastAccessedTime", ignore = true) // not updated by Research Purpose
  @Mapping(target = "lastModifiedTime", ignore = true) // not updated by Research Purpose
  @Mapping(target = "name", ignore = true) // not updated by Research Purpose
  @Mapping(target = "published", ignore = true) // not updated by Research Purpose
  @Mapping(target = "researchOutcomeEnumSet", ignore = true) // transient
  @Mapping(target = "version", ignore = true) // not updated by Research Purpose
  @Mapping(target = "workspaceActiveStatusEnum", ignore = true) // not updated by Research Purpose
  @Mapping(target = "workspaceId", ignore = true) // not updated by Research Purpose
  @Mapping(target = "workspaceNamespace", ignore = true) // not updated by Research Purpose
  void setResearchPurpose(
      @MappingTarget DbWorkspace workspace, ResearchPurpose researchPurpose);

  @Mapping(source = "billingMigrationStatus", target = "billingMigrationStatusEnum")
  @Mapping(source = "dbCdrVersion", target = "cdrVersion")
  @Mapping(source = "dbCohorts", target = "cohorts", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_DEFAULT)
  @Mapping(source = "dbDatasets", target = "dataSets", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_DEFAULT)
  @Mapping(source = "dbUser", target = "creator")
  @Mapping(source = "firecloudWorkspace.name", target = "firecloudName")
  @Mapping(source = "firecloudWorkspace.workspaceId", target = "firecloudUuid")
  @Mapping(source = "workspace.creationTime", target = "creationTime")
  @Mapping(source = "workspace.dataAccessLevel",      target = "dataAccessLevel")
  @Mapping(source = "workspace.lastModifiedTime", target = "lastModifiedTime")
  @Mapping(source = "workspace.name", target = "name")
  @Mapping(source = "workspace.namespace", target = "workspaceNamespace")
  @Mapping(source = "workspaceActiveStatus", target = "workspaceActiveStatusEnum") // transient
  @Mapping(target = "additionalNotes", ignore = true) // mapped in setResearchPurpose()
  @Mapping(target = "ancestry", ignore = true) // mapped in setResearchPurpose()
  @Mapping(target = "anticipatedFindings", ignore = true) // mapped in setResearchPurpose()
  @Mapping(target = "approved", ignore = true) // mapped in setResearchPurpose()
  @Mapping(target = "commercialPurpose", ignore = true) // mapped in setResearchPurpose()
  @Mapping(target = "controlSet", ignore = true) // mapped in setResearchPurpose()
  @Mapping(target = "dataAccessLevelEnum", ignore = true) // transient // set via dataAccessLevel
  @Mapping(target = "diseaseFocusedResearch", ignore = true) // mapped in setResearchPurpose()
  @Mapping(target = "diseaseOfFocus", ignore = true) // mapped in setResearchPurpose()
  @Mapping(target = "disseminateResearchEnumSet", ignore = true) // transient
  @Mapping(target = "disseminateResearchOther", ignore = true) // mapped in setResearchPurpose()
  @Mapping(target = "disseminateResearchSet", ignore = true) // mapped in setResearchPurpose()
  @Mapping(target = "drugDevelopment", ignore = true) // mapped in setResearchPurpose()
  @Mapping(target = "educational", ignore = true) // mapped in setResearchPurpose()
  @Mapping(target = "ethics", ignore = true) // mapped in setResearchPurpose()
  @Mapping(target = "intendedStudy", ignore = true) // mapped in setResearchPurpose()
  @Mapping(target = "methodsDevelopment", ignore = true) // mapped in setResearchPurpose()
  @Mapping(target = "otherPopulationDetails", ignore = true) // mapped in setResearchPurpose()
  @Mapping(target = "otherPurpose", ignore = true) // mapped in setResearchPurpose()
  @Mapping(target = "otherPurposeDetails", ignore = true) // mapped in setResearchPurpose()
  @Mapping(target = "populationHealth", ignore = true) // mapped in setResearchPurpose()
  @Mapping(target = "reasonForAllOfUs", ignore = true) // mapped in setResearchPurpose()
  @Mapping(target = "researchOutcomeEnumSet", ignore = true) // transient
  @Mapping(target = "researchOutcomeSet", ignore = true) // mapped in setResearchPurpose()
  @Mapping(target = "reviewRequested", ignore = true) // mapped in setResearchPurpose()
  @Mapping(target = "scientificApproach", ignore = true) // mapped in setResearchPurpose()
  @Mapping(target = "socialBehavioral", ignore = true) // mapped in setResearchPurpose()
  @Mapping(target = "specificPopulationsEnum", ignore = true) // mapped in setResearchPurpose()
  @Mapping(target = "timeRequested", ignore = true) // mapped in setResearchPurpose()
  DbWorkspace toDbWorkspace(
      Workspace workspace,
      FirecloudWorkspace firecloudWorkspace,
      DbUser dbUser,
      String billingAccountName,
      WorkspaceActiveStatus workspaceActiveStatus,
      Timestamp lastAccessedTime,
      BillingMigrationStatus billingMigrationStatus,
      int version,
      DbCdrVersion dbCdrVersion,
      Set<DbCohort> dbCohorts,
      Set<DbDataset> dbDatasets);

  /**
   * Helper method to insert the Research Purpose fields into the DbWorkspace after mapping all
   * other fields. While it's certainly possible to do
   * <code>@Mapping(source = "workspace.researchPurpose.ethics", target = "ethics")</code>
   * above, that dplicates teh setResearchPurpose code (and the generated code isn't as nice, as it
   * extracts the RP and tests it for null many times).
   * @param dbWorkspace
   * @param workspace
   */
  @AfterMapping
  default void insertResearchPurpose(@MappingTarget DbWorkspace dbWorkspace, Workspace workspace) {
    setResearchPurpose(dbWorkspace, workspace.getResearchPurpose());
  }

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
