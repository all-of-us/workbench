package org.pmiops.workbench.workspaces.resources;

import com.google.common.primitives.Longs;
import java.sql.Timestamp;
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
import org.pmiops.workbench.db.model.DbUserRecentlyModifiedResource;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceResponse;
import org.pmiops.workbench.model.CohortReview;
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

  @Mapping(target = "cohort", source = "dbCohort")
  @Mapping(target = "cohortReview", ignore = true)
  @Mapping(target = "conceptSet", ignore = true)
  @Mapping(target = "dataSet", ignore = true)
  @Mapping(target = "notebook", ignore = true)
  @Mapping(target = "lastModifiedEpochMillis", source = "lastModifiedTime")
  ResourceFields fromDbCohort(DbCohort dbCohort);

  @Mapping(target = "cohort", ignore = true)
  @Mapping(target = "cohortReview")
  @Mapping(target = "conceptSet", ignore = true)
  @Mapping(target = "dataSet", ignore = true)
  @Mapping(target = "notebook", ignore = true)
  @Mapping(target = "lastModifiedEpochMillis", source = "lastModifiedTime")
  ResourceFields fromCohortReview(CohortReview cohortReview);

  @Mapping(target = "cohort", ignore = true)
  @Mapping(target = "cohortReview", ignore = true)
  @Mapping(target = "conceptSet", source = "dbConceptSet")
  @Mapping(target = "dataSet", ignore = true)
  @Mapping(target = "notebook", ignore = true)
  @Mapping(target = "lastModifiedEpochMillis", source = "lastModifiedTime")
  ResourceFields fromDbConceptSet(DbConceptSet dbConceptSet);

  @Mapping(target = "cohort", ignore = true)
  @Mapping(target = "cohortReview", ignore = true)
  @Mapping(target = "conceptSet", ignore = true)
  @Mapping(target = "dataSet", source = "dbDataset", qualifiedByName = "dbModelToClientLight")
  @Mapping(target = "notebook", ignore = true)
  @Mapping(target = "lastModifiedEpochMillis", source = "lastModifiedTime")
  ResourceFields fromDbDataset(DbDataset dbDataset);

  @Mapping(target = "cohort", ignore = true)
  @Mapping(target = "cohortReview", ignore = true)
  @Mapping(target = "conceptSet", ignore = true)
  @Mapping(target = "dataSet", ignore = true)
  @Mapping(target = "lastModifiedBy", ignore = true)
  ResourceFields fromNotebookNameAndLastModified(
      String notebook, Timestamp lastModifiedEpochMillis);

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
      RawlsWorkspaceResponse fcWorkspace,
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

  default WorkspaceResource fromDbConceptSet(
      DbWorkspace dbWorkspace, WorkspaceAccessLevel accessLevel, DbConceptSet dbConceptSet) {
    return mergeWorkspaceAndResourceFields(
        fromWorkspace(dbWorkspace), accessLevel, fromDbConceptSet(dbConceptSet));
  }

  default WorkspaceResource fromDbDataset(
      DbWorkspace dbWorkspace, WorkspaceAccessLevel accessLevel, DbDataset dbDataset) {
    return mergeWorkspaceAndResourceFields(
        fromWorkspace(dbWorkspace), accessLevel, fromDbDataset(dbDataset));
  }

  default WorkspaceResource fromDbUserRecentlyModifiedResource(
      DbUserRecentlyModifiedResource dbUserRecentlyModifiedResource,
      RawlsWorkspaceResponse fcWorkspace,
      DbWorkspace dbWorkspace,
      CohortService cohortService,
      CohortReviewService cohortReviewService,
      ConceptSetService conceptSetService,
      DataSetService dataSetService) {
    return mergeWorkspaceAndResourceFields(
        fromWorkspace(dbWorkspace),
        fcWorkspace,
        fromDbUserRecentlyModifiedResource(
            dbUserRecentlyModifiedResource,
            cohortService,
            cohortReviewService,
            conceptSetService,
            dataSetService));
  }

  default ResourceFields fromDbUserRecentlyModifiedResource(
      DbUserRecentlyModifiedResource dbUserRecentlyModifiedResource,
      CohortService cohortService,
      CohortReviewService cohortReviewService,
      ConceptSetService conceptSetService,
      DataSetService dataSetService) {

    // null if Notebook, Long id otherwise
    final Long resourceId = Longs.tryParse(dbUserRecentlyModifiedResource.getResourceId());
    final long workspaceId = dbUserRecentlyModifiedResource.getWorkspaceId();

    switch (dbUserRecentlyModifiedResource.getResourceType()) {
      case COHORT:
        return fromDbCohort(cohortService.findByCohortIdOrThrow(resourceId));
      case COHORT_REVIEW:
        return fromCohortReview(
            cohortReviewService.findCohortReviewForWorkspace(workspaceId, resourceId));
      case CONCEPT_SET:
        return fromDbConceptSet(conceptSetService.getDbConceptSet(workspaceId, resourceId));
      case DATA_SET:
        return fromDbDataset(dataSetService.mustGetDbDataset(workspaceId, resourceId));
      case NOTEBOOK:
        return fromNotebookNameAndLastModified(
            dbUserRecentlyModifiedResource.getResourceId(),
            dbUserRecentlyModifiedResource.getLastAccessDate());
      default:
        throw new ServerErrorException("Recent resource: bad resource type ");
    }
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
