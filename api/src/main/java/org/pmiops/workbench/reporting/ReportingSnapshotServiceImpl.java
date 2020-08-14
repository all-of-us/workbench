package org.pmiops.workbench.reporting;

import java.time.Clock;
import java.util.List;
import java.util.Random;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.model.ReportingResearcher;
import org.pmiops.workbench.model.ReportingSnapshot;
import org.pmiops.workbench.model.ReportingWorkspace;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class ReportingSnapshotServiceImpl implements ReportingSnapshotService {

  private final Clock clock;
  private final ReportingMapper reportingMapper;
  private final Random random;
  private final PlatformTransactionManager platformTransactionManager;
  private final UserService userService;
  private final WorkspaceService workspaceService;

  public ReportingSnapshotServiceImpl(
      Clock clock,
      ReportingMapper reportingMapper,
      Random random,
      @Qualifier("transactionManager") PlatformTransactionManager platformTransactionManager,
      UserService userService,
      WorkspaceService workspaceService) {
    this.clock = clock;
    this.reportingMapper = reportingMapper;
    this.random = random;
    this.platformTransactionManager = platformTransactionManager;
    this.userService = userService;
    this.workspaceService = workspaceService;
  }

  @Override
  public ReportingSnapshot takeSnapshot() {
    final TransactionTemplate template = new TransactionTemplate(platformTransactionManager);
    template.setName("Reporting Snapshot");
    template.setReadOnly(true);
    return template.execute(
        t ->
            new ReportingSnapshot()
                .captureTimestamp(clock.millis())
                .researchers(getResearchers())
                .workspaces(getWorkspaces()));
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
