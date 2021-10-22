package org.pmiops.workbench.workspaces.resources;

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.pmiops.workbench.cohortreview.mapper.CohortReviewMapper;
import org.pmiops.workbench.cohorts.CohortMapper;
import org.pmiops.workbench.conceptset.mapper.ConceptSetMapper;
import org.pmiops.workbench.dataset.mapper.DataSetMapper;
import org.pmiops.workbench.db.model.DbCohort;
import org.pmiops.workbench.db.model.DbDataset;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.model.CohortReview;
import org.pmiops.workbench.model.ConceptSet;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.model.WorkspaceResource;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.pmiops.workbench.utils.mappers.MapStructConfig;

@Mapper(
    config = MapStructConfig.class,
    collectionMappingStrategy = CollectionMappingStrategy.TARGET_IMMUTABLE,
    uses = {
      CohortMapper.class,
      CohortReviewMapper.class,
      CommonMappers.class,
      ConceptSetMapper.class,
      DataSetMapper.class,
    })
public interface WorkspaceResourceMapper {

  @Mapping(target = "workspaceId", source = "dbWorkspace.workspaceId")
  @Mapping(target = "workspaceFirecloudName", source = "dbWorkspace.firecloudName")
  @Mapping(target = "workspaceBillingStatus", source = "dbWorkspace.billingStatus")
  @Mapping(target = "cdrVersionId", source = "dbWorkspace.cdrVersion")
  @Mapping(target = "accessTierShortName", source = "dbWorkspace.cdrVersion.accessTier.shortName")
  @Mapping(target = "permission", source = "accessLevel")
  @Mapping(target = "cohort", source = "dbCohort")
  // All workspaceResources have one object and all others are null.
  @Mapping(target = "cohortReview", ignore = true)
  @Mapping(target = "conceptSet", ignore = true)
  @Mapping(target = "dataSet", ignore = true)
  @Mapping(target = "notebook", ignore = true)
  // This should be set when the resource is set
  @Mapping(target = "modifiedTime", source = "dbCohort.lastModifiedTime")
  @Mapping(target = "lastModifiedEpochMillis", source = "dbCohort.lastModifiedTime")
  WorkspaceResource dbWorkspaceAndDbCohortToWorkspaceResource(
      DbWorkspace dbWorkspace, WorkspaceAccessLevel accessLevel, DbCohort dbCohort);

  @Mapping(target = "workspaceId", source = "dbWorkspace.workspaceId")
  @Mapping(target = "workspaceFirecloudName", source = "dbWorkspace.firecloudName")
  @Mapping(target = "workspaceBillingStatus", source = "dbWorkspace.billingStatus")
  @Mapping(target = "cdrVersionId", source = "dbWorkspace.cdrVersion")
  @Mapping(target = "accessTierShortName", source = "dbWorkspace.cdrVersion.accessTier.shortName")
  @Mapping(target = "permission", source = "accessLevel")
  @Mapping(target = "cohortReview", source = "cohortReview")
  // All workspaceResources have one object and all others are null.
  @Mapping(target = "cohort", ignore = true)
  @Mapping(target = "conceptSet", ignore = true)
  @Mapping(target = "dataSet", ignore = true)
  @Mapping(target = "notebook", ignore = true)
  // This should be set when the resource is set
  @Mapping(target = "modifiedTime", source = "cohortReview.lastModifiedTime")
  @Mapping(target = "lastModifiedEpochMillis", source = "cohortReview.lastModifiedTime")
  WorkspaceResource dbWorkspaceAndCohortReviewToWorkspaceResource(
      DbWorkspace dbWorkspace, WorkspaceAccessLevel accessLevel, CohortReview cohortReview);

  @Mapping(target = "workspaceId", source = "dbWorkspace.workspaceId")
  @Mapping(target = "workspaceFirecloudName", source = "dbWorkspace.firecloudName")
  @Mapping(target = "workspaceBillingStatus", source = "dbWorkspace.billingStatus")
  @Mapping(target = "cdrVersionId", source = "dbWorkspace.cdrVersion")
  @Mapping(target = "accessTierShortName", source = "dbWorkspace.cdrVersion.accessTier.shortName")
  @Mapping(target = "permission", source = "accessLevel")
  @Mapping(target = "conceptSet", source = "conceptSet")
  // All workspaceResources have one object and all others are null.
  @Mapping(target = "cohort", ignore = true)
  @Mapping(target = "cohortReview", ignore = true)
  @Mapping(target = "dataSet", ignore = true)
  @Mapping(target = "notebook", ignore = true)
  // This should be set when the resource is set
  @Mapping(target = "modifiedTime", source = "conceptSet.lastModifiedTime")
  @Mapping(target = "lastModifiedEpochMillis", source = "conceptSet.lastModifiedTime")
  WorkspaceResource dbWorkspaceAndConceptSetToWorkspaceResource(
      DbWorkspace dbWorkspace, WorkspaceAccessLevel accessLevel, ConceptSet conceptSet);

  @Mapping(target = "workspaceId", source = "dbWorkspace.workspaceId")
  @Mapping(target = "workspaceFirecloudName", source = "dbWorkspace.firecloudName")
  @Mapping(target = "workspaceBillingStatus", source = "dbWorkspace.billingStatus")
  @Mapping(target = "cdrVersionId", source = "dbWorkspace.cdrVersion")
  @Mapping(target = "accessTierShortName", source = "dbWorkspace.cdrVersion.accessTier.shortName")
  @Mapping(target = "permission", source = "accessLevel")
  @Mapping(target = "dataSet", source = "dbDataset", qualifiedByName = "dbModelToClientLight")
  // All workspaceResources have one object and all others are null.
  @Mapping(target = "cohort", ignore = true)
  @Mapping(target = "cohortReview", ignore = true)
  @Mapping(target = "conceptSet", ignore = true)
  @Mapping(target = "notebook", ignore = true)
  // This should be set when the resource is set
  @Mapping(target = "modifiedTime", source = "dbDataset.lastModifiedTime")
  @Mapping(target = "lastModifiedEpochMillis", source = "dbDataset.lastModifiedTime")
  WorkspaceResource dbWorkspaceAndDbDatasetToWorkspaceResource(
      DbWorkspace dbWorkspace, WorkspaceAccessLevel accessLevel, DbDataset dbDataset);
}
