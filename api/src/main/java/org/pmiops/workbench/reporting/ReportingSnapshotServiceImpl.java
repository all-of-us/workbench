package org.pmiops.workbench.reporting;

import com.google.common.base.Stopwatch;
import java.time.Clock;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;
import javax.inject.Provider;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.dao.projection.PrjUser;
import org.pmiops.workbench.db.dao.projection.PrjWorkspace;
import org.pmiops.workbench.db.model.DbWorkspace;
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
  private final Random random;
  private final Provider<Stopwatch> stopwatchProvider;
  private final UserService userService;
  private final WorkspaceService workspaceService;

  // Define immutable value class to hold results of queries within a transaction. Mapping to
  // Reporting DTO classes will happen outside the transaction.
  private static class QueryResultBundle {
    private final List<PrjUser> users;
    private final List<PrjWorkspace> workspaces;

    public QueryResultBundle(List<PrjUser> users, List<PrjWorkspace> workspaces) {
      this.users = users;
      this.workspaces = workspaces;
    }

    public List<PrjUser> getUsers() {
      return users;
    }

    public List<PrjWorkspace> getWorkspaces() {
      return workspaces;
    }
  }

  public ReportingSnapshotServiceImpl(
      Clock clock,
      ReportingMapper reportingMapper,
      Random random,
      Provider<Stopwatch> stopwatchProvider,
      UserService userService,
      WorkspaceService workspaceService) {
    this.clock = clock;
    this.reportingMapper = reportingMapper;
    this.random = random;
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
    final Stopwatch stopwatch = stopwatchProvider.get().start();

    final ReportingSnapshot result =
        new ReportingSnapshot()
            .captureTimestamp(clock.millis())
            .researchers(reportingMapper.toReportingResearcherList(queryResultBundle.getUsers()))
            .workspaces(reportingMapper.toReportingWorkspaceList(queryResultBundle.getWorkspaces()));
    stopwatch.stop();
    log.info(LogFormatters.duration("Conversion to ReportingSnapshot", stopwatch.elapsed()));
    return result;
  }

  private QueryResultBundle getApplicationDbData() {
    final Stopwatch stopwatch = stopwatchProvider.get().start();
    final List<PrjUser> users = userService.getRepotingUsers();
    final List<DbWorkspace> workspaces = workspaceService.getAllActiveWorkspaces();
    final QueryResultBundle result = new QueryResultBundle(users, workspaces);
    stopwatch.stop();
    log.info(LogFormatters.duration("Application DB Queries", stopwatch.elapsed()));
    return result;
  }
}
