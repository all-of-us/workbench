package org.pmiops.workbench.initialcredits;

import com.google.common.collect.Streams;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.dao.WorkspaceFreeTierUsageDao;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.db.model.DbWorkspaceFreeTierUsage;
import org.pmiops.workbench.utils.CostComparisonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** A service to update the workspace initial credit usage in the database. */
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
  public void updateWorkspaceInitialCreditsUsageInDB(
      Map<Long, Double> dbCostByWorkspace,
      Map<String, Double> liveCostByProject,
      Map<String, Long> workspaceByProject) {

    final List<String> projectsToUpdate =
        liveCostByProject.keySet().stream()
            .filter(workspaceByProject::containsKey)
            .filter(
                project ->
                    CostComparisonUtils.compareCosts(
                            dbCostByWorkspace.get(workspaceByProject.get(project)),
                            liveCostByProject.get(project))
                        != 0)
            .toList();

    List<Long> workspacesIdsToUpdate =
        projectsToUpdate.stream().map(workspaceByProject::get).toList();

    final Iterable<DbWorkspace> workspaceList = workspaceDao.findAllById(workspacesIdsToUpdate);
    final Iterable<DbWorkspaceFreeTierUsage> initialCreditsUsages =
        workspaceFreeTierUsageDao.findAllByWorkspaceIn(workspaceList);

    // Prepare cache of workspace ID to the initial credits usage entity
    Map<Long, DbWorkspaceFreeTierUsage> workspaceIdToUsageCache =
        Streams.stream(initialCreditsUsages)
            .collect(
                Collectors.toMap(
                    usage -> usage.getWorkspace().getWorkspaceId(), Function.identity()));

    // TODO updateCost queries for each workspace, can be optimized by getting all needed workspaces
    // in one query
    workspaceList.forEach(
        w ->
            workspaceFreeTierUsageDao.updateCost(
                workspaceIdToUsageCache, w, liveCostByProject.get(w.getGoogleProject())));

    logger.info(
        String.format(
            "found changed cost information for %d/%d workspaces",
            workspacesIdsToUpdate.size(), dbCostByWorkspace.size()));
  }
}
