package org.pmiops.workbench.reporting;

import com.google.common.base.Stopwatch;
import jakarta.inject.Provider;
import java.time.Clock;
import java.util.logging.Logger;
import org.pmiops.workbench.model.ReportingSnapshot;
import org.pmiops.workbench.utils.LogFormatters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportingSnapshotServiceImpl implements ReportingSnapshotService {
  private static final Logger log = Logger.getLogger(ReportingSnapshotServiceImpl.class.getName());

  private final Clock clock;
  private final Provider<Stopwatch> stopwatchProvider;

  public ReportingSnapshotServiceImpl(Clock clock, Provider<Stopwatch> stopwatchProvider) {
    this.clock = clock;
    this.stopwatchProvider = stopwatchProvider;
  }

  // Retrieve all the data we need from the MySQL database in a single transaction for
  // consistency.
  @Transactional(readOnly = true)
  @Override
  public ReportingSnapshot takeSnapshot() {
    final Stopwatch stopwatch = stopwatchProvider.get().reset().start();
    final ReportingSnapshot result = new ReportingSnapshot().captureTimestamp(clock.millis());
    stopwatch.stop();
    log.info(LogFormatters.duration("Application DB Queries", stopwatch.elapsed()));
    return result;
  }
}
