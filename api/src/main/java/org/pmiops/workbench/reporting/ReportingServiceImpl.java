package org.pmiops.workbench.reporting;

import java.time.Clock;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.pmiops.workbench.db.dao.PdrResearcherDao;
import org.pmiops.workbench.db.model.DbPdrResearcher;
import org.springframework.stereotype.Service;

@Service
public class ReportingServiceImpl implements ReportingService{

  private static final int MILLI_TO_MICRO = 1000;
  private final Clock clock;
  private final PdrResearcherDao pdrResearcherDao;
  private final ReportingMapper reportingMapper;

  public ReportingServiceImpl(Clock clock,
      PdrResearcherDao pdrResearcherDao,
        ReportingMapper reportingMapper) {
    this.clock = clock;
    this.pdrResearcherDao = pdrResearcherDao;
    this.reportingMapper = reportingMapper;
  }

  // The partition key must be an integer (can't use a timestamp),
  // but we can certainly cast it into a BigQuery timestamp easily.
  private long getBigQueryPartitionKey() {
    return clock.millis() * MILLI_TO_MICRO;
  }

  @Override
  public ReportingSnapshot getSnapshot() {
    final ReportingSnapshot result = new ReportingSnapshot();
    result.setBigQueryPartitionKey(getBigQueryPartitionKey());
    result.setResearchers(StreamSupport.stream(pdrResearcherDao.findAll().spliterator(), false)
        .map(reportingMapper::toModel)
        .collect(Collectors.toList()));
    return result;
  }
}

