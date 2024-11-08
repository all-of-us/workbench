package org.pmiops.workbench.initialcredits;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.dao.WorkspaceFreeTierUsageDao;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.db.model.DbWorkspaceFreeTierUsage;
import org.pmiops.workbench.utils.CostComparisonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** A service to update the workspace free tier usage in the database. */
@Service
public class WorkspaceInitialCreditUsageService {

  private final WorkspaceFreeTierUsageDao workspaceFreeTierUsageDao;
  private final WorkspaceDao workspaceDao;
  private static final Logger logger =
      Logger.getLogger(WorkspaceInitialCreditUsageService.class.getName());

  @Autowired
  public WorkspaceInitialCreditUsageService(
      WorkspaceFreeTierUsageDao workspaceFreeTierUsageDao, WorkspaceDao workspaceDao) {
    this.workspaceFreeTierUsageDao = workspaceFreeTierUsageDao;
    this.workspaceDao = workspaceDao;
  }

  /**
   * This method is called in batches from {@link InitialCreditsService} to update the cost in the
   * database. The method is transactional to make sure that we do incremental changes to the
   * database.
   *
   * @param dbCostByWorkspace Map that acts as a cache for all workspaces and their costs in the DB
   * @param liveCostByProject Map that links a workspace ID to its live cost in BQ.
   * @param workspaceByProject
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void updateWorkspaceFreeTierUsageInDB(
      Map<Long, Double> dbCostByWorkspace,
      Map<String, Double> liveCostByProject,
      Map<String, Long> workspaceByProject) {

    final List<String> projectsToUpdate =
        liveCostByProject.keySet().stream()
            .filter(project -> workspaceByProject.containsKey(project))
            .filter(
                project ->
                    CostComparisonUtils.compareCosts(
                            dbCostByWorkspace.get(workspaceByProject.get(project)),
                            liveCostByProject.get(project))
                        != 0)
            .collect(Collectors.toList());

    List<Long> workspacesIdsToUpdate =
        projectsToUpdate.stream()
            .map(project -> workspaceByProject.get(project))
            .collect(Collectors.toList());

    final Iterable<DbWorkspace> workspaceList = workspaceDao.findAllById(workspacesIdsToUpdate);
    final Iterable<DbWorkspaceFreeTierUsage> workspaceFreeTierUsages =
        workspaceFreeTierUsageDao.findAllByWorkspaceIn(workspaceList);

    // Prepare cache of workspace ID to the free tier use entity
    Map<Long, DbWorkspaceFreeTierUsage> workspaceIdToFreeTierUsageCache =
        StreamSupport.stream(workspaceFreeTierUsages.spliterator(), false)
            .collect(
                Collectors.toMap(
                    wftu -> wftu.getWorkspace().getWorkspaceId(), Function.identity()));

    workspaceList.forEach(
        w ->
            workspaceFreeTierUsageDao.updateCost(
                workspaceIdToFreeTierUsageCache,
                w,
                liveCostByProject.get(
                    w.getGoogleProject()))); // TODO updateCost queries for each workspace, can be
    // optimized by getting all needed workspaces in one
    // query

    logger.info(
        String.format(
            "found changed cost information for %d/%d workspaces",
            workspacesIdsToUpdate.size(), dbCostByWorkspace.size()));
  }
}
