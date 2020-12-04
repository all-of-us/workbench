package org.pmiops.workbench.db.jdbc;

import java.util.List;
import org.pmiops.workbench.model.ReportingDatasetCohort;
import org.pmiops.workbench.model.ReportingDatasetConceptSet;
import org.pmiops.workbench.model.ReportingDatasetDomainIdValue;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class ReportingNativeQueryServiceImpl implements ReportingNativeQueryService {

  private final JdbcTemplate jdbcTemplate;

  public ReportingNativeQueryServiceImpl(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public List<ReportingDatasetCohort> getReportingDatasetCohorts() {
    return jdbcTemplate.query(
        "SELECT data_set_id, cohort_id\n"
            + "FROM data_set_cohort\n"
            + "ORDER BY data_set_id, cohort_id;",
        (rs, unused) ->
            new ReportingDatasetCohort()
                .cohortId(rs.getLong("cohort_id"))
                .datasetId(rs.getLong("data_set_id")));
  }

  @Override
  public List<ReportingDatasetConceptSet> getReportingDatasetConceptSets() {
    return jdbcTemplate.query(
        "SELECT data_set_id, concept_set_id\n"
            + "FROM data_set_concept_set\n"
            + "ORDER BY data_set_id, concept_set_id;",
        (rs, unused) ->
            new ReportingDatasetConceptSet()
                .datasetId(rs.getLong("data_set_id"))
                .conceptSetId(rs.getLong("concept_set_id")));
  }

  @Override
  public List<ReportingDatasetDomainIdValue> getReportingDatasetDomainIdValues() {
    return jdbcTemplate.query(
        "SELECT data_set_id, domain_id, value\n"
            + "FROM data_set_values\n"
            + "ORDER BY data_set_id, domain_id;",
        (rs, unused) ->
            new ReportingDatasetDomainIdValue()
                .datasetId(rs.getLong("data_set_id"))
                .domainId(rs.getString("domain_id"))
                .value(rs.getString("value")));
  }
}
