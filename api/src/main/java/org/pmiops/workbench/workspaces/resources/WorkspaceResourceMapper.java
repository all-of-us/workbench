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
  WorkspaceResource mergeWorkspaceAndResourceFields(
      WorkspaceFields workspaceFields, ResourceFields resourceFields);

  @Mapping(target = "workspaceId", source = "dbWorkspace.workspaceId")
  @Mapping(target = "workspaceFirecloudName", source = "dbWorkspace.firecloudName")
  @Mapping(target = "workspaceBillingStatus", source = "dbWorkspace.billingStatus")
  @Mapping(target = "cdrVersionId", source = "dbWorkspace.cdrVersion")
  @Mapping(target = "accessTierShortName", source = "dbWorkspace.cdrVersion.accessTier.shortName")
  @Mapping(target = "permission", source = "accessLevel")
  WorkspaceFields workspaceResourceFromWorkspace(
      DbWorkspace dbWorkspace, WorkspaceAccessLevel accessLevel);

  // a WorkspaceResource has one resource object.  Assign it and ignore all others (keep as NULL).

  @Mapping(target = "cohort", source = "dbCohort")
  @Mapping(target = "cohortReview", ignore = true)
  @Mapping(target = "conceptSet", ignore = true)
  @Mapping(target = "dataSet", ignore = true)
  @Mapping(target = "notebook", ignore = true)
  // This should be set when the resource is set
  @Mapping(target = "lastModifiedEpochMillis", source = "dbCohort.lastModifiedTime")
  ResourceFields workspaceResourceFromDbCohort(DbCohort dbCohort);

  @Mapping(target = "cohort", ignore = true)
  @Mapping(target = "cohortReview", source = "cohortReview")
  @Mapping(target = "conceptSet", ignore = true)
  @Mapping(target = "dataSet", ignore = true)
  @Mapping(target = "notebook", ignore = true)
  // This should be set when the resource is set
  @Mapping(target = "lastModifiedEpochMillis", source = "cohortReview.lastModifiedTime")
  ResourceFields workspaceResourceFromCohortReview(CohortReview cohortReview);

  @Mapping(target = "cohort", ignore = true)
  @Mapping(target = "cohortReview", ignore = true)
  @Mapping(target = "conceptSet", source = "conceptSet")
  @Mapping(target = "dataSet", ignore = true)
  @Mapping(target = "notebook", ignore = true)
  // This should be set when the resource is set
  @Mapping(target = "lastModifiedEpochMillis", source = "conceptSet.lastModifiedTime")
  ResourceFields workspaceResourceFromConceptSet(ConceptSet conceptSet);

  @Mapping(target = "cohort", ignore = true)
  @Mapping(target = "cohortReview", ignore = true)
  @Mapping(target = "conceptSet", ignore = true)
  @Mapping(target = "dataSet", source = "dbDataset", qualifiedByName = "dbModelToClientLight")
  @Mapping(target = "notebook", ignore = true)
  // This should be set when the resource is set
  @Mapping(target = "lastModifiedEpochMillis", source = "dbDataset.lastModifiedTime")
  ResourceFields workspaceResourceFromDbDataset(DbDataset dbDataset);
  // TODO: can MapStruct generate these automatically?

  default WorkspaceResource dbWorkspaceAndDbCohortToWorkspaceResource(
      DbWorkspace dbWorkspace, WorkspaceAccessLevel accessLevel, DbCohort dbCohort) {
    final WorkspaceFields workspaceFields =
        workspaceResourceFromWorkspace(dbWorkspace, accessLevel);
    final ResourceFields resourceFields = workspaceResourceFromDbCohort(dbCohort);
    return mergeWorkspaceAndResourceFields(workspaceFields, resourceFields);
  }

  default WorkspaceResource dbWorkspaceAndCohortReviewToWorkspaceResource(
      DbWorkspace dbWorkspace, WorkspaceAccessLevel accessLevel, CohortReview cohortReview) {
    final WorkspaceFields workspaceFields =
        workspaceResourceFromWorkspace(dbWorkspace, accessLevel);
    final ResourceFields resourceFields = workspaceResourceFromCohortReview(cohortReview);
    return mergeWorkspaceAndResourceFields(workspaceFields, resourceFields);
  }

  default WorkspaceResource dbWorkspaceAndConceptSetToWorkspaceResource(
      DbWorkspace dbWorkspace, WorkspaceAccessLevel accessLevel, ConceptSet conceptSet) {
    final WorkspaceFields workspaceFields =
        workspaceResourceFromWorkspace(dbWorkspace, accessLevel);
    final ResourceFields resourceFields = workspaceResourceFromConceptSet(conceptSet);
    return mergeWorkspaceAndResourceFields(workspaceFields, resourceFields);
  }

  default WorkspaceResource dbWorkspaceAndDbDatasetToWorkspaceResource(
      DbWorkspace dbWorkspace, WorkspaceAccessLevel accessLevel, DbDataset dbDataset) {
    final WorkspaceFields workspaceFields =
        workspaceResourceFromWorkspace(dbWorkspace, accessLevel);
    final ResourceFields resourceFields = workspaceResourceFromDbDataset(dbDataset);
    return mergeWorkspaceAndResourceFields(workspaceFields, resourceFields);
  }
}
