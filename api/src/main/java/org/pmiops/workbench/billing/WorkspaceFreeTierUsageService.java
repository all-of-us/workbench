package org.pmiops.workbench.billing;

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
public class WorkspaceFreeTierUsageService {

  private final WorkspaceFreeTierUsageDao workspaceFreeTierUsageDao;
  private final WorkspaceDao workspaceDao;
  private static final Logger logger =
      Logger.getLogger(WorkspaceFreeTierUsageService.class.getName());

  @Autowired
  public WorkspaceFreeTierUsageService(
      WorkspaceFreeTierUsageDao workspaceFreeTierUsageDao, WorkspaceDao workspaceDao) {
    this.workspaceFreeTierUsageDao = workspaceFreeTierUsageDao;
    this.workspaceDao = workspaceDao;
  }

  /**
   * This method is called in batches from {@link FreeTierBillingService} to update the cost in the
   * database. The method is transactional to make sure that we do incremental changes to the
   * database.
   *
   * @param dbCostByWorkspace Map that acts as a cache for all workspaces and their costs in the DB
   * @param liveCostByWorkspace Map that links a workspace ID to its live cost in BQ.
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void updateWorkspaceFreeTierUsageInDB(
      Map<Long, Double> dbCostByWorkspace, Map<Long, Double> liveCostByWorkspace) {

    final List<Long> workspacesIdsToUpdate =
        liveCostByWorkspace.keySet().stream()
            .filter(
                currentId ->
                    CostComparisonUtils.compareCosts(
                            dbCostByWorkspace.get(currentId), liveCostByWorkspace.get(currentId))
                        != 0)
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
                liveCostByWorkspace.get(
                    w.getWorkspaceId()))); // TODO updateCost queries for each workspace, can be
    // optimized by getting all needed workspaces in one
    // query

    logger.info(
        String.format(
            "found changed cost information for %d/%d workspaces",
            workspacesIdsToUpdate.size(), dbCostByWorkspace.size()));
  }
}
