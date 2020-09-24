package org.pmiops.workbench.reporting;

import com.google.common.base.Stopwatch;
import java.time.Clock;
import java.util.List;
import java.util.logging.Logger;
import javax.inject.Provider;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.dao.projection.ProjectedReportingUser;
import org.pmiops.workbench.db.dao.projection.ProjectedReportingWorkspace;
import org.pmiops.workbench.model.ReportingSnapshot;
import org.pmiops.workbench.utils.LogFormatters;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportingSnapshotServiceImpl implements ReportingSnapshotService {
  private static final Logger log = Logger.getLogger(ReportingSnapshotServiceImpl.class.getName());

  private final Clock clock;
  private final ReportingMapper reportingMapper;
  private final Provider<Stopwatch> stopwatchProvider;
  private final UserService userService;
  private final WorkspaceService workspaceService;

  // Define immutable value class to hold results of queries within a transaction. Mapping to
  // Reporting DTO classes will happen outside the transaction.
  private static class QueryResultBundle {
    private final List<ProjectedReportingUser> users;
    private final List<ProjectedReportingWorkspace> workspaces;

    public QueryResultBundle(
        List<ProjectedReportingUser> users, List<ProjectedReportingWorkspace> workspaces) {
      this.users = users;
      this.workspaces = workspaces;
    }

    public List<ProjectedReportingUser> getUsers() {
      return users;
    }

    public List<ProjectedReportingWorkspace> getWorkspaces() {
      return workspaces;
    }
  }

  public ReportingSnapshotServiceImpl(
      Clock clock,
      ReportingMapper reportingMapper,
      Provider<Stopwatch> stopwatchProvider,
      UserService userService,
      WorkspaceService workspaceService) {
    this.clock = clock;
    this.reportingMapper = reportingMapper;
    this.stopwatchProvider = stopwatchProvider;
    this.userService = userService;
    this.workspaceService = workspaceService;
  }

  // Retrieve all the data we need from the MySQL database in a single transaction for
  // consistency.
  @Transactional(readOnly = true)
  @Override
  public ReportingSnapshot takeSnapshot() {
    final QueryResultBundle queryResultBundle = getApplicationDbData();
    return convertToReportingSnapshot(queryResultBundle);
  }

  public ReportingSnapshot convertToReportingSnapshot(QueryResultBundle queryResultBundle) {
    final Stopwatch stopwatch = stopwatchProvider.get().start();
    final ReportingSnapshot result =
        new ReportingSnapshot()
            .captureTimestamp(clock.millis())
            .users(reportingMapper.toReportingUserList(queryResultBundle.getUsers()))
            .workspaces(
                reportingMapper.toReportingWorkspaceList(queryResultBundle.getWorkspaces()));
    stopwatch.stop();
    log.info(LogFormatters.duration("Conversion to ReportingSnapshot", stopwatch.elapsed()));
    return result;
  }

  /*
   * Gather all the projection instances from the DB rows needed for the current snapshot. Bundle
   * them all together for conversion all at once, and also to allow timing the downloads separately
   * from the conversion
   */
  private QueryResultBundle getApplicationDbData() {
    final Stopwatch stopwatch = stopwatchProvider.get().start();
    final QueryResultBundle result =
        new QueryResultBundle(
            userService.getReportingUsers(), workspaceService.getReportingWorkspaces());
    stopwatch.stop();
    log.info(LogFormatters.duration("Application DB Queries", stopwatch.elapsed()));
    return result;
  }
}
