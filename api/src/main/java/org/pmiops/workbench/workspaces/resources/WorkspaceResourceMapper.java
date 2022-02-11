package org.pmiops.workbench.workspaces.resources;

import java.util.Optional;
import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.pmiops.workbench.cohortreview.CohortReviewService;
import org.pmiops.workbench.cohortreview.mapper.CohortReviewMapper;
import org.pmiops.workbench.cohorts.CohortMapper;
import org.pmiops.workbench.cohorts.CohortService;
import org.pmiops.workbench.conceptset.ConceptSetService;
import org.pmiops.workbench.conceptset.mapper.ConceptSetMapper;
import org.pmiops.workbench.dataset.DataSetService;
import org.pmiops.workbench.dataset.mapper.DataSetMapper;
import org.pmiops.workbench.db.model.DbCohort;
import org.pmiops.workbench.db.model.DbConceptSet;
import org.pmiops.workbench.db.model.DbDataset;
import org.pmiops.workbench.db.model.DbUserRecentResource;
import org.pmiops.workbench.db.model.DbUserRecentlyModifiedResource;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceResponse;
import org.pmiops.workbench.model.CohortReview;
import org.pmiops.workbench.model.ConceptSet;
import org.pmiops.workbench.model.FileDetail;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.model.WorkspaceResource;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.pmiops.workbench.utils.mappers.FirecloudMapper;
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
      FirecloudMapper.class,
    })
public interface WorkspaceResourceMapper {
  @Mapping(target = "workspaceId", source = "dbWorkspace.workspaceId")
  @Mapping(target = "workspaceFirecloudName", source = "dbWorkspace.firecloudName")
  @Mapping(target = "workspaceBillingStatus", source = "dbWorkspace.billingStatus")
  @Mapping(target = "cdrVersionId", source = "dbWorkspace.cdrVersion")
  @Mapping(target = "accessTierShortName", source = "dbWorkspace.cdrVersion.accessTier.shortName")
  WorkspaceFields fromWorkspace(DbWorkspace dbWorkspace);

  // a WorkspaceResource has one resource object.  Assign it and ignore all others (keep as NULL).

  @Mapping(target = "cohortReview", ignore = true)
  @Mapping(target = "conceptSet", ignore = true)
  @Mapping(target = "dataSet", ignore = true)
  @Mapping(target = "notebook", ignore = true)
  @Mapping(target = "cohort", source = "dbCohort")
  @Mapping(target = "lastModifiedEpochMillis", source = "dbCohort.lastModifiedTime")
  ResourceFields fromDbCohort(DbCohort dbCohort);

  @Mapping(target = "cohort", ignore = true)
  @Mapping(target = "conceptSet", ignore = true)
  @Mapping(target = "dataSet", ignore = true)
  @Mapping(target = "notebook", ignore = true)
  @Mapping(target = "cohortReview", source = "cohortReview")
  @Mapping(target = "lastModifiedEpochMillis", source = "cohortReview.lastModifiedTime")
  ResourceFields fromCohortReview(CohortReview cohortReview);

  @Mapping(target = "cohort", ignore = true)
  @Mapping(target = "cohortReview", ignore = true)
  @Mapping(target = "dataSet", ignore = true)
  @Mapping(target = "notebook", ignore = true)
  @Mapping(target = "conceptSet", source = "conceptSet")
  @Mapping(target = "lastModifiedEpochMillis", source = "conceptSet.lastModifiedTime")
  ResourceFields fromConceptSet(ConceptSet conceptSet);

  @Mapping(target = "cohort", ignore = true)
  @Mapping(target = "cohortReview", ignore = true)
  @Mapping(target = "conceptSet", ignore = true)
  @Mapping(target = "notebook", ignore = true)
  @Mapping(target = "dataSet", source = "dbDataset", qualifiedByName = "dbModelToClientLight")
  @Mapping(target = "lastModifiedEpochMillis", source = "dbDataset.lastModifiedTime")
  ResourceFields fromDbDataset(DbDataset dbDataset);

  // TODO why are Cohort Review and Dataset not present in DbUserRecentResource?
  @Mapping(target = "cohortReview", ignore = true)
  @Mapping(target = "dataSet", ignore = true)
  @Mapping(target = "notebook", source = "notebookName")
  @Mapping(target = "lastModifiedEpochMillis", source = "dbUserRecentResource.lastAccessDate")
  ResourceFields fromDbUserRecentResource(DbUserRecentResource dbUserRecentResource);

  @Mapping(target = "cohortReview", ignore = true)
  @Mapping(target = "cohort", source = "dbCohort")
  @Mapping(target = "conceptSet", ignore = true)
  @Mapping(target = "notebook", ignore = true)
  @Mapping(target = "dataSet", ignore = true)
  @Mapping(
      target = "lastModifiedEpochMillis",
      source = "dbUserRecentlyModifiedResource.lastAccessDate")
  ResourceFields fromDbUserRecentlyModifiedResource(
      DbUserRecentlyModifiedResource dbUserRecentlyModifiedResource, DbCohort dbCohort);

  @Mapping(target = "cohortReview", source = "cohortReview")
  @Mapping(target = "cohort", ignore = true)
  @Mapping(target = "conceptSet", ignore = true)
  @Mapping(target = "notebook", ignore = true)
  @Mapping(target = "dataSet", ignore = true)
  @Mapping(
      target = "lastModifiedEpochMillis",
      source = "dbUserRecentlyModifiedResource.lastAccessDate")
  ResourceFields fromDbUserRecentlyModifiedResource(
      DbUserRecentlyModifiedResource dbUserRecentlyModifiedResource, CohortReview cohortReview);

  @Mapping(target = "cohortReview", ignore = true)
  @Mapping(target = "cohort", ignore = true)
  @Mapping(target = "conceptSet", source = "dbConceptSet")
  @Mapping(target = "notebook", ignore = true)
  @Mapping(target = "dataSet", ignore = true)
  @Mapping(
      target = "lastModifiedEpochMillis",
      source = "dbUserRecentlyModifiedResource.lastAccessDate")
  ResourceFields fromDbUserRecentlyModifiedResource(
      DbUserRecentlyModifiedResource dbUserRecentlyModifiedResource, DbConceptSet dbConceptSet);

  @Mapping(target = "cohortReview", ignore = true)
  @Mapping(target = "cohort", ignore = true)
  @Mapping(target = "conceptSet", ignore = true)
  @Mapping(target = "notebook", ignore = true)
  @Mapping(target = "dataSet", source = "dbDataset")
  @Mapping(
      target = "lastModifiedEpochMillis",
      source = "dbUserRecentlyModifiedResource.lastAccessDate")
  ResourceFields fromDbUserRecentlyModifiedResource(
      DbUserRecentlyModifiedResource dbUserRecentlyModifiedResource, DbDataset dbDataset);

  @Mapping(target = "cohortReview", ignore = true)
  @Mapping(target = "cohort", ignore = true)
  @Mapping(target = "conceptSet", ignore = true)
  @Mapping(target = "dataSet", ignore = true)
  @Mapping(target = "notebook", source = "notebookName")
  @Mapping(
      target = "lastModifiedEpochMillis",
      source = "dbUserRecentlyModifiedResource.lastAccessDate")
  ResourceFields fromDbUserRecentlyModifiedResource(
      DbUserRecentlyModifiedResource dbUserRecentlyModifiedResource, String notebookName);

  @Mapping(target = "permission", source = "accessLevel")
  WorkspaceResource mergeWorkspaceAndResourceFields(
      WorkspaceFields workspaceFields,
      WorkspaceAccessLevel accessLevel,
      ResourceFields resourceFields);

  @Mapping(
      target = "permission",
      source = "fcWorkspace.accessLevel",
      qualifiedByName = "fcToApiWorkspaceAccessLevel")
  WorkspaceResource mergeWorkspaceAndResourceFields(
      WorkspaceFields workspaceFields,
      FirecloudWorkspaceResponse fcWorkspace,
      ResourceFields resourceFields);

  default WorkspaceResource fromDbCohort(
      DbWorkspace dbWorkspace, WorkspaceAccessLevel accessLevel, DbCohort dbCohort) {
    return mergeWorkspaceAndResourceFields(
        fromWorkspace(dbWorkspace), accessLevel, fromDbCohort(dbCohort));
  }

  default WorkspaceResource fromCohortReview(
      DbWorkspace dbWorkspace, WorkspaceAccessLevel accessLevel, CohortReview cohortReview) {
    return mergeWorkspaceAndResourceFields(
        fromWorkspace(dbWorkspace), accessLevel, fromCohortReview(cohortReview));
  }

  default WorkspaceResource fromConceptSet(
      DbWorkspace dbWorkspace, WorkspaceAccessLevel accessLevel, ConceptSet conceptSet) {
    return mergeWorkspaceAndResourceFields(
        fromWorkspace(dbWorkspace), accessLevel, fromConceptSet(conceptSet));
  }

  default WorkspaceResource fromDbDataset(
      DbWorkspace dbWorkspace, WorkspaceAccessLevel accessLevel, DbDataset dbDataset) {
    return mergeWorkspaceAndResourceFields(
        fromWorkspace(dbWorkspace), accessLevel, fromDbDataset(dbDataset));
  }

  @Deprecated
  default WorkspaceResource fromDbUserRecentResource(
      DbUserRecentResource dbUserRecentResource,
      FirecloudWorkspaceResponse fcWorkspace,
      DbWorkspace dbWorkspace) {
    return mergeWorkspaceAndResourceFields(
        fromWorkspace(dbWorkspace), fcWorkspace, fromDbUserRecentResource(dbUserRecentResource));
  }

  default WorkspaceResource fromDbUserRecentlyModifiedResourceAndCohort(
      DbUserRecentlyModifiedResource dbUserRecentResource,
      FirecloudWorkspaceResponse fcWorkspace,
      DbWorkspace dbWorkspace,
      DbCohort dbCohort) {
    return mergeWorkspaceAndResourceFields(
        fromWorkspace(dbWorkspace),
        fcWorkspace,
        fromDbUserRecentlyModifiedResource(dbUserRecentResource, dbCohort));
  }

  default WorkspaceResource fromDbUserRecentlyModifiedResourceAndConceptSet(
      DbUserRecentlyModifiedResource dbUserRecentResource,
      FirecloudWorkspaceResponse fcWorkspace,
      DbWorkspace dbWorkspace,
      DbConceptSet dbConceptSet) {
    return mergeWorkspaceAndResourceFields(
        fromWorkspace(dbWorkspace),
        fcWorkspace,
        fromDbUserRecentlyModifiedResource(dbUserRecentResource, dbConceptSet));
  }

  default WorkspaceResource fromDbUserRecentlyModifiedResourceAndDataSet(
      DbUserRecentlyModifiedResource dbUserRecentResource,
      FirecloudWorkspaceResponse fcWorkspace,
      DbWorkspace dbWorkspace,
      DbDataset dbDataset) {
    return mergeWorkspaceAndResourceFields(
        fromWorkspace(dbWorkspace),
        fcWorkspace,
        fromDbUserRecentlyModifiedResource(dbUserRecentResource, dbDataset));
  }

  default WorkspaceResource fromDbUserRecentlyModifiedResourceAndNotebookName(
      DbUserRecentlyModifiedResource dbUserRecentResource,
      FirecloudWorkspaceResponse fcWorkspace,
      DbWorkspace dbWorkspace,
      String notebookName) {
    return mergeWorkspaceAndResourceFields(
        fromWorkspace(dbWorkspace),
        fcWorkspace,
        fromDbUserRecentlyModifiedResource(dbUserRecentResource, notebookName));
  }

  default WorkspaceResource fromDbUserRecentlyModifiedResourceAndCohortReview(
      DbUserRecentlyModifiedResource dbUserRecentResource,
      FirecloudWorkspaceResponse fcWorkspace,
      DbWorkspace dbWorkspace,
      CohortReview cohortReview) {
    return mergeWorkspaceAndResourceFields(
        fromWorkspace(dbWorkspace),
        fcWorkspace,
        fromDbUserRecentlyModifiedResource(dbUserRecentResource, cohortReview));
  }

  default WorkspaceResource fromDbUserRecentlyModifiedResource(
      DbUserRecentlyModifiedResource dbUserRecentlyModifiedResource,
      FirecloudWorkspaceResponse fcWorkspace,
      DbWorkspace dbWorkspace,
      CohortService cohortService,
      CohortReviewService cohortReviewService,
      ConceptSetService conceptSetService,
      DataSetService dataSetService) {
    final long workspaceId = dbUserRecentlyModifiedResource.getWorkspaceId();
    switch (dbUserRecentlyModifiedResource.getResourceType()) {
      case COHORT:
        return fromDbUserRecentlyModifiedResourceAndCohort(
            dbUserRecentlyModifiedResource,
            fcWorkspace,
            dbWorkspace,
            cohortService.findDbCohortByCohortId(
                getResourceIdInLong(dbUserRecentlyModifiedResource)));
      case CONCEPT_SET:
        return fromDbUserRecentlyModifiedResourceAndConceptSet(
            dbUserRecentlyModifiedResource,
            fcWorkspace,
            dbWorkspace,
            conceptSetService.getDbConceptSet(
                workspaceId, getResourceIdInLong(dbUserRecentlyModifiedResource)));
      case DATA_SET:
        return fromDbUserRecentlyModifiedResourceAndDataSet(
            dbUserRecentlyModifiedResource,
            fcWorkspace,
            dbWorkspace,
            dataSetService
                .getDbDataSet(workspaceId, getResourceIdInLong(dbUserRecentlyModifiedResource))
                .orElseThrow(
                    () ->
                        new NotFoundException(
                            String.format(
                                "Dataset not found for dataSetId: %s",
                                dbUserRecentlyModifiedResource.getResourceId()))));
      case NOTEBOOK:
        return fromDbUserRecentlyModifiedResourceAndNotebookName(
            dbUserRecentlyModifiedResource,
            fcWorkspace,
            dbWorkspace,
            dbUserRecentlyModifiedResource.getResourceId());
      case COHORT_REVIEW:
        return fromDbUserRecentlyModifiedResourceAndCohortReview(
            dbUserRecentlyModifiedResource,
            fcWorkspace,
            dbWorkspace,
            cohortReviewService.findCohortReviewForWorkspace(
                workspaceId, getResourceIdInLong(dbUserRecentlyModifiedResource)));

      default:
        throw new ServerErrorException("Recent resource: bad resource type ");
    }
  }

  default long getResourceIdInLong(DbUserRecentlyModifiedResource dbUserRecentlyModifiedResource) {
    return Long.parseLong(
        Optional.ofNullable(dbUserRecentlyModifiedResource.getResourceId()).orElse("0"));
  }

  default FileDetail convertStringToFileDetail(String str) {
    if (str == null) {
      return null;
    }
    if (!str.startsWith("gs://")) {
      return null;
    }
    int filenameStart = str.lastIndexOf('/') + 1;
    return new FileDetail()
        .name(str.substring(filenameStart))
        .path(str.substring(0, filenameStart));
  }
}
