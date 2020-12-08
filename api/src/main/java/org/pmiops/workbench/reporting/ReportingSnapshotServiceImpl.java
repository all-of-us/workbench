package org.pmiops.workbench.reporting;

import com.google.common.base.Stopwatch;
import java.time.Clock;
import java.util.logging.Logger;
import javax.inject.Provider;
import org.pmiops.workbench.cohorts.CohortService;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.jdbc.ReportingNativeQueryService;
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
  private final ReportingNativeQueryService reportingNativeQueryService;
  private final InstitutionService institutionService;
  private final ReportingMapper reportingMapper;
  private final Provider<Stopwatch> stopwatchProvider;
  private final UserService userService;

  public ReportingSnapshotServiceImpl(
      Clock clock,
      CohortService cohortService,
      ReportingNativeQueryService reportingNativeQueryService,
      InstitutionService institutionService,
      ReportingMapper reportingMapper,
      Provider<Stopwatch> stopwatchProvider,
      UserService userService) {
    this.clock = clock;
    this.cohortService = cohortService;
    this.reportingNativeQueryService = reportingNativeQueryService;
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
    final QueryResultBundle queryResultBundle = getApplicationDatabaseRecords();
    return convertToReportingSnapshot(queryResultBundle);
  }

  public ReportingSnapshot convertToReportingSnapshot(QueryResultBundle queryResultBundle) {
    final Stopwatch stopwatch = stopwatchProvider.get().start();
    final ReportingSnapshot result =
        reportingMapper.toReportingSnapshot(queryResultBundle, clock.millis());
    stopwatch.stop();
    log.info(LogFormatters.duration("Conversion to ReportingSnapshot", stopwatch.elapsed()));
    return result;
  }

  /*
   * Gather all the projection instances from the DB rows needed for the current snapshot. Bundle
   * them all together for conversion all at once, and also to allow timing the downloads separately
   * from the conversion
   */
  private QueryResultBundle getApplicationDatabaseRecords() {
    final Stopwatch stopwatch = stopwatchProvider.get().start();
    final QueryResultBundle result =
        new QueryResultBundle(
            cohortService.getReportingCohorts(),
            reportingNativeQueryService.getReportingDatasets(),
            reportingNativeQueryService.getReportingDatasetCohorts(),
            reportingNativeQueryService.getReportingDatasetConceptSets(),
            reportingNativeQueryService.getReportingDatasetDomainIdValues(),
            institutionService.getReportingInstitutions(),
            userService.getReportingUsers(),
            reportingNativeQueryService.getReportingWorkspaces());
    stopwatch.stop();
    log.info(LogFormatters.duration("Application DB Queries", stopwatch.elapsed()));
    return result;
  }
}
