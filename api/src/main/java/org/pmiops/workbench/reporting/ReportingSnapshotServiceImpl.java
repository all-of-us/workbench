package org.pmiops.workbench.reporting;

import com.google.common.base.Stopwatch;
import java.time.Clock;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;
import javax.inject.Provider;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.model.ReportingResearcher;
import org.pmiops.workbench.model.ReportingSnapshot;
import org.pmiops.workbench.model.ReportingWorkspace;
import org.pmiops.workbench.monitoring.LogsBasedMetricServiceImpl;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class ReportingSnapshotServiceImpl implements ReportingSnapshotService {
  private static final Logger log = Logger.getLogger(ReportingSnapshotServiceImpl.class.getName());

  private final Clock clock;
  private final ReportingMapper reportingMapper;
  private final Random random;
  private final PlatformTransactionManager platformTransactionManager;
  private final Provider<Stopwatch> stopwatchProvider;
  private final UserService userService;
  private final WorkspaceService workspaceService;

  public ReportingSnapshotServiceImpl(
      Clock clock,
      ReportingMapper reportingMapper,
      Random random,
      @Qualifier("transactionManager") PlatformTransactionManager platformTransactionManager,
      Provider<Stopwatch> stopwatchProvider,
      UserService userService,
      WorkspaceService workspaceService) {
    this.clock = clock;
    this.reportingMapper = reportingMapper;
    this.random = random;
    this.platformTransactionManager = platformTransactionManager;
    this.stopwatchProvider = stopwatchProvider;
    this.userService = userService;
    this.workspaceService = workspaceService;
  }

  @Override
  public ReportingSnapshot takeSnapshot() {
    final Stopwatch stopwatch = stopwatchProvider.get().start();
    final TransactionTemplate template = new TransactionTemplate(platformTransactionManager);
    template.setName("Reporting Snapshot");
    template.setReadOnly(true);
    final ReportingSnapshot result = template.execute(
        t ->
            new ReportingSnapshot()
                .captureTimestamp(clock.millis())
                .researchers(getResearchers())
                .workspaces(getWorkspaces()));
    stopwatch.stop();
    log.info(String.format("Snapshot created in %s", stopwatch.elapsed().toString()));
    return result;
  }

  private List<ReportingResearcher> getResearchers() {
    final List<DbUser> users = userService.getAllUsers();
    return reportingMapper.toReportingResearcherList(users);
  }

  private List<ReportingWorkspace> getWorkspaces() {
    final List<DbWorkspace> workspaces = workspaceService.getAllActiveWorkspaces();
    final List<ReportingWorkspace> models = reportingMapper.toReportingWorkspaceList(workspaces);
    for (ReportingWorkspace model : models) {
      model.setFakeSize(getFakeSize());
    }
    return models;
  }

  private long getFakeSize() {
    return random.nextLong();
  }
}
