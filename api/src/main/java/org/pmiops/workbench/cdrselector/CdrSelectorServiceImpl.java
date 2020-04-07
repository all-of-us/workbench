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
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.model.WorkspaceResource;
import org.pmiops.workbench.utils.WorkspaceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CdrSelectorServiceImpl implements CdrSelectorService {
  private final CohortReviewService cohortReviewService;
  private final ConceptSetService conceptSetService;
  private final DataSetDao dataSetDao;
  private final WorkspaceMapper workspaceMapper;

  @Autowired
  public CdrSelectorServiceImpl(
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
  public List<WorkspaceResource> getCdrSelectorsInWorkspace(
      DbWorkspace dbWorkspace, WorkspaceAccessLevel workspaceAccessLevel) {
    List<WorkspaceResource> workspaceResources = new ArrayList<WorkspaceResource>();
    final Set<DbCohort> cohorts = dbWorkspace.getCohorts();
    workspaceResources.addAll(
        cohorts.stream()
            .map(
                cohort ->
                    workspaceMapper.workspaceResourceFromDbWorkspaceAndDbCohort(
                        dbWorkspace, workspaceAccessLevel, cohort))
            .collect(Collectors.toList()));
    List<DbCohortReview> reviews =
        cohortReviewService.getRequiredWithCohortReviews(
            dbWorkspace.getWorkspaceNamespace(), dbWorkspace.getFirecloudName());
    workspaceResources.addAll(
        reviews.stream()
            .map(
                cohortReview ->
                    workspaceMapper.workspaceResourceFromDbWorkspaceAndCohortReview(
                        dbWorkspace, workspaceAccessLevel, cohortReview))
            .collect(Collectors.toList()));
    List<DbConceptSet> conceptSets =
        conceptSetService.findByWorkspaceId(dbWorkspace.getWorkspaceId());
    workspaceResources.addAll(
        conceptSets.stream()
            .map(
                dbConceptSet ->
                    workspaceMapper.workspaceResourceFromDbWorkspaceAndConceptSet(
                        dbWorkspace, workspaceAccessLevel, dbConceptSet))
            .collect(Collectors.toList()));
    List<DbDataset> datasets =
        dataSetDao.findByWorkspaceIdAndInvalid(dbWorkspace.getWorkspaceId(), false);
    workspaceResources.addAll(
        datasets.stream()
            .map(
                dbDataset ->
                    workspaceMapper.workspaceResourceFromDbWorkspaceAndDataSet(
                        dbWorkspace, workspaceAccessLevel, dbDataset))
            .collect(Collectors.toList()));
    return workspaceResources;
  }
}
