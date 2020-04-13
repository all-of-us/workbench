package org.pmiops.workbench.cdrselector;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.pmiops.workbench.cohortreview.CohortReviewService;
import org.pmiops.workbench.conceptset.ConceptSetService;
import org.pmiops.workbench.db.dao.DataSetDao;
import org.pmiops.workbench.db.model.DbCohort;
import org.pmiops.workbench.db.model.DbCohortReview;
import org.pmiops.workbench.db.model.DbConceptSet;
import org.pmiops.workbench.db.model.DbDataset;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.model.ResourceType;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.model.WorkspaceResource;
import org.pmiops.workbench.utils.WorkspaceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class WorkspaceResourcesServiceImpl implements WorkspaceResourcesService {
  private final CohortReviewService cohortReviewService;
  private final ConceptSetService conceptSetService;
  private final DataSetDao dataSetDao;
  private final WorkspaceMapper workspaceMapper;

  @Autowired
  public WorkspaceResourcesServiceImpl(
      CohortReviewService cohortReviewService,
      ConceptSetService conceptSetService,
      DataSetDao dataSetDao,
      WorkspaceMapper workspaceMapper) {
    this.cohortReviewService = cohortReviewService;
    this.conceptSetService = conceptSetService;
    this.dataSetDao = dataSetDao;
    this.workspaceMapper = workspaceMapper;
  }

  @Override
  public List<WorkspaceResource> getWorkspaceResources(
      DbWorkspace dbWorkspace,
      WorkspaceAccessLevel workspaceAccessLevel,
      List<ResourceType> resourceTypes) {
    if (resourceTypes.size() == 0) {
      throw new BadRequestException("Must provide at least one resource type");
    }

    List<WorkspaceResource> workspaceResources = new ArrayList<>();

    if (resourceTypes.contains(ResourceType.COHORT)) {
      final Set<DbCohort> cohorts = dbWorkspace.getCohorts();
      workspaceResources.addAll(
          cohorts.stream()
              .map(
                  cohort ->
                      workspaceMapper.dbWorkspaceAndDbCohortToWorkspaceResource(
                          dbWorkspace, workspaceAccessLevel, cohort))
              .collect(Collectors.toList()));
    }
    if (resourceTypes.contains(ResourceType.COHORT_REVIEW)) {
      List<DbCohortReview> reviews =
          cohortReviewService.getRequiredWithCohortReviews(
              dbWorkspace.getWorkspaceNamespace(), dbWorkspace.getFirecloudName());
      workspaceResources.addAll(
          reviews.stream()
              .map(
                  cohortReview ->
                      workspaceMapper.dbWorkspaceAndDbCohortReviewToWorkspaceResource(
                          dbWorkspace, workspaceAccessLevel, cohortReview))
              .collect(Collectors.toList()));
    }
    if (resourceTypes.contains(ResourceType.CONCEPT_SET)) {
      List<DbConceptSet> conceptSets =
          conceptSetService.findByWorkspaceId(dbWorkspace.getWorkspaceId());
      workspaceResources.addAll(
          conceptSets.stream()
              .map(
                  dbConceptSet ->
                      workspaceMapper.dbWorkspaceAndDbConceptSetToWorkspaceResource(
                          dbWorkspace, workspaceAccessLevel, dbConceptSet))
              .collect(Collectors.toList()));
    }
    if (resourceTypes.contains(ResourceType.DATASET)) {
      List<DbDataset> datasets =
          dataSetDao.findByWorkspaceIdAndInvalid(dbWorkspace.getWorkspaceId(), false);
      workspaceResources.addAll(
          datasets.stream()
              .map(
                  dbDataset ->
                      workspaceMapper.dbWorkspaceAndDbDatasetToWorkspaceResource(
                          dbWorkspace, workspaceAccessLevel, dbDataset))
              .collect(Collectors.toList()));
    }
    if (resourceTypes.contains(ResourceType.COHORT_SEARCH_GROUP)
        || resourceTypes.contains(ResourceType.COHORT_SEARCH_ITEM)
        || resourceTypes.contains((ResourceType.NOTEBOOK))
        || resourceTypes.contains((ResourceType.WORKSPACE))) {
      throw new ServerErrorException(
          "Only supported resource types are Cohorts, Cohort Reviews, Concept Sets, and Datasets");
    }
    return workspaceResources;
  }
}
