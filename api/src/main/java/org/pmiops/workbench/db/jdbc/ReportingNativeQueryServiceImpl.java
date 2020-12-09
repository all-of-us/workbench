package org.pmiops.workbench.db.jdbc;

import static org.pmiops.workbench.db.model.DbStorageEnums.billingAccountTypeFromStorage;
import static org.pmiops.workbench.db.model.DbStorageEnums.billingStatusFromStorage;
import static org.pmiops.workbench.utils.mappers.CommonMappers.offsetDateTimeUtc;

import java.util.List;
import org.pmiops.workbench.model.ReportingDataset;
import org.pmiops.workbench.model.ReportingDatasetCohort;
import org.pmiops.workbench.model.ReportingDatasetConceptSet;
import org.pmiops.workbench.model.ReportingDatasetDomainIdValue;
import org.pmiops.workbench.model.ReportingInstitution;
import org.pmiops.workbench.model.ReportingWorkspace;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class ReportingNativeQueryServiceImpl implements ReportingNativeQueryService {

  private final JdbcTemplate jdbcTemplate;

  public ReportingNativeQueryServiceImpl(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public List<ReportingWorkspace> getReportingWorkspaces() {
    return jdbcTemplate.query(
        "SELECT \n"
            + "  billing_account_type,\n"
            + "  billing_status,\n"
            + "  cdr_version_id,\n"
            + "  creation_time,\n"
            + "  creator_id,\n"
            + "  disseminate_research_other,\n"
            + "  last_accessed_time,\n"
            + "  last_modified_time,\n"
            + "  name,\n"
            + "  needs_rp_review_prompt,\n"
            + "  published,\n"
            + "  rp_additional_notes,\n"
            + "  rp_ancestry,\n"
            + "  rp_anticipated_findings,\n"
            + "  rp_approved,\n"
            + "  rp_commercial_purpose,\n"
            + "  rp_control_set,\n"
            + "  rp_disease_focused_research,\n"
            + "  rp_disease_of_focus,\n"
            + "  rp_drug_development,\n"
            + "  rp_educational,\n"
            + "  rp_ethics,\n"
            + "  rp_intended_study,\n"
            + "  rp_methods_development,\n"
            + "  rp_other_population_details,\n"
            + "  rp_other_purpose,\n"
            + "  rp_other_purpose_details,\n"
            + "  rp_population_health,\n"
            + "  rp_reason_for_all_of_us,\n"
            + "  rp_review_requested,\n"
            + "  rp_scientific_approach,\n"
            + "  rp_social_behavioral,\n"
            + "  rp_time_requested,\n"
            + "  workspace_id\n"
            + "FROM workspace",
        (rs, unused) ->
            new ReportingWorkspace()
                .billingAccountType(
                    billingAccountTypeFromStorage(rs.getShort("billing_account_type")))
                .billingStatus(billingStatusFromStorage(rs.getShort("billing_status")))
                .cdrVersionId(rs.getLong("cdr_version_id"))
                .creationTime(offsetDateTimeUtc(rs.getTimestamp("creation_time")))
                .creatorId(rs.getLong("creator_id"))
                .disseminateResearchOther(rs.getString("disseminate_research_other"))
                .lastAccessedTime(offsetDateTimeUtc(rs.getTimestamp("last_accessed_time")))
                .lastModifiedTime(offsetDateTimeUtc(rs.getTimestamp("last_modified_time")))
                .name(rs.getString("name"))
                .needsRpReviewPrompt((int) rs.getShort("needs_rp_review_prompt"))
                .published(rs.getBoolean("published"))
                .rpAdditionalNotes(rs.getString("rp_additional_notes"))
                .rpAncestry(rs.getBoolean("rp_ancestry"))
                .rpAnticipatedFindings(rs.getString("rp_anticipated_findings"))
                .rpApproved(rs.getBoolean("rp_approved"))
                .rpCommercialPurpose(rs.getBoolean("rp_commercial_purpose"))
                .rpControlSet(rs.getBoolean("rp_control_set"))
                .rpDiseaseFocusedResearch(rs.getBoolean("rp_disease_focused_research"))
                .rpDiseaseOfFocus(rs.getString("rp_disease_of_focus"))
                .rpDrugDevelopment(rs.getBoolean("rp_drug_development"))
                .rpEducational(rs.getBoolean("rp_educational"))
                .rpEthics(rs.getBoolean("rp_ethics"))
                .rpIntendedStudy(rs.getString("rp_intended_study"))
                .rpMethodsDevelopment(rs.getBoolean("rp_methods_development"))
                .rpOtherPopulationDetails(rs.getString("rp_other_population_details"))
                .rpOtherPurpose(rs.getBoolean("rp_other_purpose"))
                .rpOtherPurposeDetails(rs.getString("rp_other_purpose_details"))
                .rpPopulationHealth(rs.getBoolean("rp_population_health"))
                .rpReasonForAllOfUs(rs.getString("rp_reason_for_all_of_us"))
                .rpReviewRequested(rs.getBoolean("rp_review_requested"))
                .rpScientificApproach(rs.getString("rp_scientific_approach"))
                .rpSocialBehavioral(rs.getBoolean("rp_social_behavioral"))
                .rpTimeRequested(offsetDateTimeUtc(rs.getTimestamp("rp_time_requested")))
                .workspaceId(rs.getLong("workspace_id")));
  }

  @Override
  public List<ReportingDataset> getReportingDatasets() {
    return jdbcTemplate.query(
        "SELECT \n"
            + "  creation_time,\n"
            + "  creator_id,\n"
            + "  data_set_id,\n"
            + "  description,\n"
            + "  includes_all_participants,\n"
            + "  last_modified_time,\n"
            + "  name,\n"
            + "  workspace_id\n"
            + "FROM data_set",
        (rs, unused) ->
            new ReportingDataset()
                .creationTime(offsetDateTimeUtc(rs.getTimestamp("creation_time")))
                .creatorId(rs.getLong("creator_id"))
                .datasetId(rs.getLong("data_set_id"))
                .description(rs.getString("description"))
                .includesAllParticipants(rs.getBoolean("includes_all_participants"))
                .lastModifiedTime(offsetDateTimeUtc(rs.getTimestamp("last_modified_time")))
                .name(rs.getString("name"))
                .workspaceId(rs.getLong("workspace_id")));
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

  @Override
  public List<ReportingInstitution> getReportingInstitutions() {
    return null;
  }
}
