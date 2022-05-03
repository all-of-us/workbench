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
import org.pmiops.workbench.db.model.DbUserRecentResource;
import org.pmiops.workbench.db.model.DbUserRecentlyModifiedResource;
import org.pmiops.workbench.db.model.DbWorkspace;
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

  // TODO combine fromConceptSet/fromDbConceptSet

  @Mapping(target = "cohort", ignore = true)
  @Mapping(target = "cohortReview", ignore = true)
  @Mapping(target = "conceptSet")
  @Mapping(target = "dataSet", ignore = true)
  @Mapping(target = "notebook", ignore = true)
  @Mapping(target = "lastModifiedEpochMillis", source = "lastModifiedTime")
  ResourceFields fromConceptSet(ConceptSet conceptSet);

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
  ResourceFields fromNotebookNameAndLastModified(
      String notebook, Timestamp lastModifiedEpochMillis);

  @Mapping(target = "cohortReview", ignore = true)
  @Mapping(target = "dataSet", ignore = true)
  @Mapping(target = "notebook", source = "notebookName")
  @Mapping(target = "lastModifiedEpochMillis", source = "lastAccessDate")
  ResourceFields fromDbUserRecentResource(DbUserRecentResource dbUserRecentResource);

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

  default WorkspaceResource fromDbUserRecentlyModifiedResource(
      DbUserRecentlyModifiedResource dbUserRecentlyModifiedResource,
      FirecloudWorkspaceResponse fcWorkspace,
      DbWorkspace dbWorkspace,
      CohortService cohortService,
      CohortReviewService cohortReviewService,
      ConceptSetService conceptSetService,
      DataSetService dataSetService) {

    // null if Notebook, Long id otherwise
    final Long resourceId = Longs.tryParse(dbUserRecentlyModifiedResource.getResourceId());
    final long workspaceId = dbUserRecentlyModifiedResource.getWorkspaceId();

    final ResourceFields resourceFields;
    switch (dbUserRecentlyModifiedResource.getResourceType()) {
      case COHORT:
        resourceFields = fromDbCohort(cohortService.findDbCohortByCohortId(resourceId));
        break;
      case COHORT_REVIEW:
        resourceFields =
            fromCohortReview(
                cohortReviewService.findCohortReviewForWorkspace(workspaceId, resourceId));
        break;
      case CONCEPT_SET:
        resourceFields =
            fromDbConceptSet(conceptSetService.getDbConceptSet(workspaceId, resourceId));
        break;
      case DATA_SET:
        resourceFields = fromDbDataset(dataSetService.mustGetDbDataset(workspaceId, resourceId));
        break;
      case NOTEBOOK:
        resourceFields =
            fromNotebookNameAndLastModified(
                dbUserRecentlyModifiedResource.getResourceId(),
                dbUserRecentlyModifiedResource.getLastAccessDate());
        break;
      default:
        throw new ServerErrorException("Recent resource: bad resource type ");
    }
    return mergeWorkspaceAndResourceFields(fromWorkspace(dbWorkspace), fcWorkspace, resourceFields);
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
