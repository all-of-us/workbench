package org.pmiops.workbench.reporting;

import com.google.common.base.Stopwatch;
import java.time.Clock;
import java.util.logging.Logger;
import javax.inject.Provider;
import org.pmiops.workbench.cohorts.CohortService;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.jdbc.ReportingQueryService;
import org.pmiops.workbench.institution.InstitutionService;
import org.pmiops.workbench.model.ReportingSnapshot;
import org.pmiops.workbench.utils.LogFormatters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportingSnapshotServiceImpl implements ReportingSnapshotService {
  private static final Logger log = Logger.getLogger(ReportingSnapshotServiceImpl.class.getName());

  private final Clock clock;
  private final CohortService cohortService;
  private final ReportingQueryService queryService;
  private final InstitutionService institutionService;
  private final ReportingMapper reportingMapper;
  private final Provider<Stopwatch> stopwatchProvider;
  private final UserService userService;

  public ReportingSnapshotServiceImpl(
      Clock clock,
      CohortService cohortService,
      ReportingQueryService queryService,
      InstitutionService institutionService,
      ReportingMapper reportingMapper,
      Provider<Stopwatch> stopwatchProvider,
      UserService userService) {
    this.clock = clock;
    this.cohortService = cohortService;
    this.queryService = queryService;
    this.institutionService = institutionService;
    this.reportingMapper = reportingMapper;
    this.stopwatchProvider = stopwatchProvider;
    this.userService = userService;
  }

  // Retrieve all the data we need from the MySQL database in a single transaction for
  // consistency.
  @Transactional(readOnly = true)
  @Override
  public ReportingSnapshot takeSnapshot() {
    final Stopwatch stopwatch = stopwatchProvider.get().start();
    final ReportingSnapshot result =
        new ReportingSnapshot()
            .cohorts(queryService.getReportingCohorts())
            .datasets(queryService.getDatasets())
            .datasetCohorts(queryService.getDatasetCohorts())
            .datasetConceptSets(queryService.getDatasetConceptSets())
            .datasetDomainIdValues(queryService.getDatasetDomainIdValues())
            .institutions(queryService.getInstitutions())
            .users(userService.getReportingUsers())
            .workspaces(queryService.getReportingWorkspaces());
    stopwatch.stop();
    log.info(LogFormatters.duration("Application DB Queries", stopwatch.elapsed()));
    return result;
  }
}
