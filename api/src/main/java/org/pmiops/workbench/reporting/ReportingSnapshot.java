package org.pmiops.workbench.reporting;

import java.util.List;

public class ReportingSnapshot {
  private long bigQueryPartitionKey;
  private List<PdrResearcherRow> researchers;

  public long getBigQueryPartitionKey() {
    return bigQueryPartitionKey;
  }

  public void setBigQueryPartitionKey(long bigQueryPartitionKey) {
    this.bigQueryPartitionKey = bigQueryPartitionKey;
  }

  public List<PdrResearcherRow> getResearchers() {
    return researchers;
  }

  public void setResearchers(List<PdrResearcherRow> researchers) {
    this.researchers = researchers;
  }
}
