package org.pmiops.workbench.reporting;

import java.time.Clock;
import java.util.List;
import java.util.logging.Logger;
import org.joda.time.DateTime;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.model.ReportingResearcher;
import org.pmiops.workbench.model.ReportingSnapshot;
import org.pmiops.workbench.model.ReportingWorkspace;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.stereotype.Service;

@Service
public class ReportingServiceImpl implements ReportingService {

  private static final int MILLI_TO_MICRO = 1000;
  private final Clock clock;
  private final ReportingMapper reportingMapper;
  private final UserService userService;
  private final WorkspaceService workspaceService;

  public ReportingServiceImpl(
      Clock clock,
      ReportingMapper reportingMapper,
      UserService userService,
      WorkspaceService workspaceService) {
    this.clock = clock;
    this.reportingMapper = reportingMapper;
    this.userService = userService;
    this.workspaceService = workspaceService;
  }

  // The partition key must be an integer (can't use a timestamp),
  // but we can certainly cast it into a BigQuery timestamp easily.
  private long getBigQueryPartitionKey() {
    return clock.millis() * MILLI_TO_MICRO;
  }

  @Override
  public ReportingSnapshot getSnapshot() {
    return new ReportingSnapshot()
        .captureTimestamp(new DateTime(clock.millis()))
        .researchers(getResearchers())
        .workspaces(getWorkspaces());
  }

  @Override
  public void uploadSnapshot() {
    final ReportingSnapshot snapshot = getSnapshot();
  }

  private List<ReportingResearcher> getResearchers() {
    final List<DbUser> users = userService.getAllUsers();
    return reportingMapper.toReportingResearcherList(users);
  }

  private List<ReportingWorkspace> getWorkspaces() {
    final List<DbWorkspace> workspaces = workspaceService.getAllActiveWorkspaces();
    return reportingMapper.toReportingWorkspaceList(workspaces);
  }
}
