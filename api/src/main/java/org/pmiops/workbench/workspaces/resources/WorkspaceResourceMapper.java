package org.pmiops.workbench.workspaces.resources;

import java.util.regex.Matcher;
import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.pmiops.workbench.cohortreview.mapper.CohortReviewMapper;
import org.pmiops.workbench.cohorts.CohortMapper;
import org.pmiops.workbench.conceptset.mapper.ConceptSetMapper;
import org.pmiops.workbench.dataset.mapper.DataSetMapper;
import org.pmiops.workbench.db.model.DbCohort;
import org.pmiops.workbench.db.model.DbDataset;
import org.pmiops.workbench.db.model.DbUserRecentResource;
import org.pmiops.workbench.db.model.DbWorkspace;
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

  default WorkspaceResource fromDbUserRecentResource(
      DbUserRecentResource dbUserRecentResource,
      FirecloudWorkspaceResponse fcWorkspace,
      DbWorkspace dbWorkspace) {
    return mergeWorkspaceAndResourceFields(
        fromWorkspace(dbWorkspace), fcWorkspace, fromDbUserRecentResource(dbUserRecentResource));
  }

  default FileDetail convertStringToFileDetail(String str) {
    if (str == null) {
      return null;
    }
    if (!str.startsWith("gs://")) {
      return null;
    }
    int pos = str.lastIndexOf('/') + 1;
    String fileName = str.substring(pos);
    String replacement = Matcher.quoteReplacement(fileName) + "$";
    String filePath = str.replaceFirst(replacement, "");
    return new FileDetail().name(fileName).path(filePath);
  }
}
