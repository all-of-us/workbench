package org.pmiops.workbench.db.jdbc;

import static org.pmiops.workbench.db.model.DbStorageEnums.billingAccountTypeFromStorage;
import static org.pmiops.workbench.db.model.DbStorageEnums.billingStatusFromStorage;
import static org.pmiops.workbench.db.model.DbStorageEnums.degreeFromStorage;
import static org.pmiops.workbench.db.model.DbStorageEnums.disabilityFromStorage;
import static org.pmiops.workbench.db.model.DbStorageEnums.educationFromStorage;
import static org.pmiops.workbench.db.model.DbStorageEnums.ethnicityFromStorage;
import static org.pmiops.workbench.db.model.DbStorageEnums.genderIdentityFromStorage;
import static org.pmiops.workbench.db.model.DbStorageEnums.institutionDUATypeFromStorage;
import static org.pmiops.workbench.db.model.DbStorageEnums.institutionalRoleFromStorage;
import static org.pmiops.workbench.db.model.DbStorageEnums.organizationTypeFromStorage;
import static org.pmiops.workbench.db.model.DbStorageEnums.raceFromStorage;
import static org.pmiops.workbench.db.model.DbStorageEnums.sexAtBirthFromStorage;
import static org.pmiops.workbench.utils.mappers.CommonMappers.offsetDateTimeUtc;

import com.google.common.base.Strings;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.model.ReportingCohort;
import org.pmiops.workbench.model.ReportingDataset;
import org.pmiops.workbench.model.ReportingDatasetCohort;
import org.pmiops.workbench.model.ReportingDatasetConceptSet;
import org.pmiops.workbench.model.ReportingDatasetDomainIdValue;
import org.pmiops.workbench.model.ReportingInstitution;
import org.pmiops.workbench.model.ReportingUser;
import org.pmiops.workbench.model.ReportingWorkspace;
import org.pmiops.workbench.model.ReportingWorkspaceFreeTierUsage;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class ReportingQueryServiceImpl implements ReportingQueryService {
  private static final long MAX_ROWS_PER_INSERT_ALL_REQUEST = 10_000;
  private final JdbcTemplate jdbcTemplate;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;

  public ReportingQueryServiceImpl(
      JdbcTemplate jdbcTemplate, Provider<WorkbenchConfig> workbenchConfigProvider) {
    this.jdbcTemplate = jdbcTemplate;
    this.workbenchConfigProvider = workbenchConfigProvider;
  }

  public long getQueryBatchSize() {
    return Math.min(
        MAX_ROWS_PER_INSERT_ALL_REQUEST, workbenchConfigProvider.get().reporting.maxRowsPerInsert);
  }

  @Override
  public List<ReportingWorkspaceFreeTierUsage> getWorkspaceFreeTierUsage() {
    return jdbcTemplate.query(
        "SELECT\n"
            + "  cost,\n"
            + "  user_id,\n"
            + "  workspace_id\n"
            + "FROM workspace_free_tier_usage",
        (rs, unused) ->
            new ReportingWorkspaceFreeTierUsage()
                .cost(rs.getDouble("cost"))
                .userId(rs.getLong("user_id"))
                .workspaceId(rs.getLong("workspace_id")));
  }

  @Override
  public List<ReportingCohort> getCohorts() {
    return jdbcTemplate.query(
        "SELECT \n"
            + "  cohort_id,\n"
            + "  creation_time,\n"
            + "  creator_id,\n"
            + "  criteria,\n"
            + "  description,\n"
            + "  last_modified_time,\n"
            + "  name,\n"
            + "  workspace_id\n"
            + "FROM cohort",
        (rs, unused) ->
            new ReportingCohort()
                .cohortId(rs.getLong("cohort_id"))
                .creationTime(offsetDateTimeUtc(rs.getTimestamp("creation_time")))
                .creatorId(rs.getLong("creator_id"))
                .criteria(rs.getString("criteria"))
                .description(rs.getString("description"))
                .lastModifiedTime(offsetDateTimeUtc(rs.getTimestamp("last_modified_time")))
                .name(rs.getString("name"))
                .workspaceId(rs.getLong("workspace_id")));
  }

  @Override
  public List<ReportingDataset> getDatasets() {
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
  public List<ReportingDatasetCohort> getDatasetCohorts() {
    return jdbcTemplate.query(
        "SELECT data_set_id, cohort_id\n" + "FROM data_set_cohort",
        (rs, unused) ->
            new ReportingDatasetCohort()
                .cohortId(rs.getLong("cohort_id"))
                .datasetId(rs.getLong("data_set_id")));
  }

  @Override
  public List<ReportingDatasetConceptSet> getDatasetConceptSets() {
    return jdbcTemplate.query(
        "SELECT data_set_id, concept_set_id\n" + "FROM data_set_concept_set",
        (rs, unused) ->
            new ReportingDatasetConceptSet()
                .datasetId(rs.getLong("data_set_id"))
                .conceptSetId(rs.getLong("concept_set_id")));
  }

  @Override
  public List<ReportingDatasetDomainIdValue> getDatasetDomainIdValues() {
    return jdbcTemplate.query(
        "SELECT data_set_id, domain_id, value\n" + "FROM data_set_values",
        (rs, unused) ->
            new ReportingDatasetDomainIdValue()
                .datasetId(rs.getLong("data_set_id"))
                .domainId(rs.getString("domain_id"))
                .value(rs.getString("value")));
  }

  @Override
  public List<ReportingInstitution> getInstitutions() {
    return jdbcTemplate.query(
        "SELECT \n"
            + "  display_name,\n"
            + "  dua_type_enum,\n"
            + "  institution_id,\n"
            + "  organization_type_enum,\n"
            + "  organization_type_other_text,\n"
            + "  short_name\n"
            + "FROM institution",
        (rs, unused) ->
            new ReportingInstitution()
                .displayName(rs.getString("display_name"))
                .duaTypeEnum(institutionDUATypeFromStorage(rs.getShort("dua_type_enum")))
                .institutionId(rs.getLong("institution_id"))
                .organizationTypeEnum(
                    organizationTypeFromStorage(rs.getShort("organization_type_enum")))
                .organizationTypeOtherText(rs.getString("organization_type_other_text"))
                .shortName(rs.getString("short_name")));
  }

  @Override
  public List<ReportingUser> getUsers(long limit, long offset) {
    return jdbcTemplate.query(
        String.format(
            "SELECT \n"
                + "  u.user_id,\n"
                + "  u.area_of_research,\n"
                + "  u.compliance_training_bypass_time,\n"
                + "  u.compliance_training_completion_time,\n"
                + "  u.compliance_training_expiration_time,\n"
                + "  u.contact_email,\n"
                + "  u.creation_time,\n"
                + "  u.data_use_agreement_bypass_time,\n"
                + "  u.data_use_agreement_completion_time,\n"
                + "  u.data_use_agreement_signed_version,\n"
                + "  u.demographic_survey_completion_time,\n"
                + "  u.disabled,\n"
                + "  u.era_commons_bypass_time,\n"
                + "  u.era_commons_completion_time,\n"
                + "  u.family_name,\n"
                // temporary solution to RW-6566
                + "  uat.first_enabled AS first_registration_completion_time,\n"
                + "  u.first_sign_in_time,\n"
                + "  u.free_tier_credits_limit_dollars_override,\n"
                + "  u.given_name,\n"
                + "  u.last_modified_time,\n"
                + "  u.professional_url,\n"
                + "  u.two_factor_auth_bypass_time,\n"
                + "  u.two_factor_auth_completion_time,\n"
                + "  u.email AS username,\n"
                + "  a.city,\n"
                + "  a.country,\n"
                + "  a.state,\n"
                + "  a.street_address_1,\n"
                + "  a.street_address_2,\n"
                + "  a.zip_code,\n"
                + "  via.institution_id AS institution_id,\n"
                + "  via.institutional_role_enum,\n"
                + "  via.institutional_role_other_text,\n"
                + "  dm.degrees,\n"
                + "  dm.ethnicity,\n"
                + "  dm.year_of_birth,\n"
                + "  dm.disability,\n"
                + "  dm.education,\n"
                + "  dm.identifies_as_lgbtq,\n"
                + "  dm.lgbtq_identity,\n"
                + "  dm.gender_identity,\n"
                + "  dm.race,\n"
                + "  dm.sex_at_birth,\n"
                + "  t.access_tier_short_names\n"
                + "FROM user u"
                + "  LEFT OUTER JOIN address AS a ON u.user_id = a.user_id\n"
                + "  LEFT OUTER JOIN user_verified_institutional_affiliation AS via on u.user_id = via.user_id\n"
                + "  LEFT OUTER JOIN "
                + "  ( "
                + "       SELECT \n"
                + "             demo.user_id, "
                + "             GROUP_CONCAT(DISTINCT ud.degree) as degrees, "
                + "             GROUP_CONCAT(DISTINCT demo.ethnicity) as ethnicity, "
                + "             GROUP_CONCAT(DISTINCT demo.year_of_birth) as year_of_birth, "
                + "             GROUP_CONCAT(DISTINCT demo.education) as education, "
                + "             GROUP_CONCAT(DISTINCT demo.disability) as disability, "
                + "             GROUP_CONCAT(DISTINCT demo.identifies_as_lgbtq) as identifies_as_lgbtq, "
                + "             GROUP_CONCAT(DISTINCT demo.lgbtq_identity) as lgbtq_identity, "
                + "             GROUP_CONCAT(DISTINCT di.gender_identity) as gender_identity, "
                + "             GROUP_CONCAT(DISTINCT dr.race) as race, "
                + "             GROUP_CONCAT(DISTINCT ds.sex_at_birth) as sex_at_birth"
                + "       FROM demographic_survey as demo "
                + "         LEFT OUTER JOIN demographic_survey_gender_identity as di "
                + "             ON demo.demographic_survey_id = di.demographic_survey_id\n"
                + "         LEFT OUTER JOIN demographic_survey_race as dr "
                + "             ON demo.demographic_survey_id = dr.demographic_survey_id\n"
                + "         LEFT OUTER JOIN demographic_survey_sex_at_birth as ds "
                + "             ON demo.demographic_survey_id = ds.demographic_survey_id\n"
                + "         LEFT OUTER JOIN user_degree AS ud on demo.user_id = ud.user_id "
                + "         GROUP BY demo.user_id "
                + "  ) AS dm on u.user_id = dm.user_id"
                + "  LEFT OUTER JOIN ("
                + "    SELECT u.user_id, GROUP_CONCAT(DISTINCT a.short_name) AS access_tier_short_names "
                + "    FROM user u "
                + "      JOIN user_access_tier uat ON u.user_id = uat.user_id "
                + "      JOIN access_tier a ON a.access_tier_id = uat.access_tier_id "
                + "      WHERE uat.access_status = 1 " // ENABLED
                + "      GROUP BY u.user_id"
                + "  ) as t ON t.user_id = u.user_id "
                // temporary solution to RW-6566: retrieve first_enabled from user_access_tier
                // for 'registered' entries as a substitute for first_registration_completion_time
                + "  LEFT OUTER JOIN ( "
                + "    SELECT uat.user_id, uat.first_enabled FROM user_access_tier uat "
                + "    JOIN access_tier at ON at.access_tier_id = uat.access_tier_id "
                + "    WHERE uat.access_status = 1 AND at.short_name = 'registered' "
                + "  ) uat ON u.user_id = uat.user_id "
                // end temporary solution for RW-6566
                + "  ORDER BY u.user_id"
                + "  LIMIT %d\n"
                + "  OFFSET %d",
            limit, offset),
        (rs, unused) ->
            new ReportingUser()
                .areaOfResearch(rs.getString("area_of_research"))
                .complianceTrainingBypassTime(
                    offsetDateTimeUtc(rs.getTimestamp("compliance_training_bypass_time")))
                .complianceTrainingCompletionTime(
                    offsetDateTimeUtc(rs.getTimestamp("compliance_training_completion_time")))
                .complianceTrainingExpirationTime(
                    offsetDateTimeUtc(rs.getTimestamp("compliance_training_expiration_time")))
                .contactEmail(rs.getString("contact_email"))
                .creationTime(offsetDateTimeUtc(rs.getTimestamp("creation_time")))
                .accessTierShortNames(rs.getString("access_tier_short_names"))
                .dataUseAgreementBypassTime(
                    offsetDateTimeUtc(rs.getTimestamp("data_use_agreement_bypass_time")))
                .dataUseAgreementCompletionTime(
                    offsetDateTimeUtc(rs.getTimestamp("data_use_agreement_completion_time")))
                .dataUseAgreementSignedVersion(rs.getInt("data_use_agreement_signed_version"))
                .demographicSurveyCompletionTime(
                    offsetDateTimeUtc(rs.getTimestamp("demographic_survey_completion_time")))
                .disabled(rs.getBoolean("disabled"))
                .eraCommonsBypassTime(offsetDateTimeUtc(rs.getTimestamp("era_commons_bypass_time")))
                .eraCommonsCompletionTime(
                    offsetDateTimeUtc(rs.getTimestamp("era_commons_completion_time")))
                .familyName(rs.getString("family_name"))
                .firstRegistrationCompletionTime(
                    offsetDateTimeUtc(rs.getTimestamp("first_registration_completion_time")))
                .firstSignInTime(offsetDateTimeUtc(rs.getTimestamp("first_sign_in_time")))
                .freeTierCreditsLimitDollarsOverride(
                    rs.getDouble("free_tier_credits_limit_dollars_override"))
                .givenName(rs.getString("given_name"))
                .lastModifiedTime(offsetDateTimeUtc(rs.getTimestamp("last_modified_time")))
                .professionalUrl(rs.getString("professional_url"))
                .twoFactorAuthBypassTime(
                    offsetDateTimeUtc(rs.getTimestamp("two_factor_auth_bypass_time")))
                .twoFactorAuthCompletionTime(
                    offsetDateTimeUtc(rs.getTimestamp("two_factor_auth_completion_time")))
                .userId(rs.getLong("user_id"))
                .username(rs.getString("username"))
                .city(rs.getString("city"))
                .country(rs.getString("country"))
                .state(rs.getString("state"))
                .streetAddress1(rs.getString("street_address_1"))
                .streetAddress2(rs.getString("street_address_2"))
                .zipCode(rs.getString("zip_code"))
                .institutionId(rs.getLong("institution_id"))
                .institutionalRoleEnum(
                    institutionalRoleFromStorage(rs.getShort("institutional_role_enum")))
                .institutionalRoleOtherText(rs.getString("institutional_role_other_text"))
                .highestEducation(educationFromStorage(rs.getShort("education")))
                .ethnicity(ethnicityFromStorage(rs.getShort("ethnicity")))
                .disability(disabilityFromStorage(rs.getShort("disability")))
                .races(
                    convertListEnumFromStorage(
                        rs.getString("race"), e -> raceFromStorage(e).toString()))
                .genderIdentities(
                    convertListEnumFromStorage(
                        rs.getString("gender_identity"),
                        e -> genderIdentityFromStorage(e).toString()))
                .sexesAtBirth(
                    convertListEnumFromStorage(
                        rs.getString("sex_at_birth"), e -> sexAtBirthFromStorage(e).toString()))
                .lgbtqIdentity(rs.getString("lgbtq_identity"))
                .identifiesAsLgbtq(rs.getBoolean("identifies_as_lgbtq"))
                .yearOfBirth(rs.getBigDecimal("year_of_birth"))
                .degrees(
                    convertListEnumFromStorage(
                        rs.getString("degrees"), e -> degreeFromStorage(e).toString())));
  }

  @Override
  public List<ReportingWorkspace> getWorkspaces(long limit, long offset) {
    return jdbcTemplate.query(
        String.format(
            "SELECT \n"
                + "  billing_account_type,\n"
                + "  billing_status,\n"
                + "  w.cdr_version_id AS cdr_version_id,\n"
                + "  w.creation_time AS creation_time,\n"
                + "  creator_id,\n"
                + "  disseminate_research_other,\n"
                + "  last_accessed_time,\n"
                + "  last_modified_time,\n"
                + "  w.name AS name,\n"
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
                + "  workspace_id,\n"
                + "  workspace_namespace,\n"
                + "  a.short_name AS access_tier_short_name\n"
                + "FROM workspace w\n"
                + "  JOIN cdr_version c ON w.cdr_version_id = c.cdr_version_id\n"
                + "  JOIN access_tier a ON c.access_tier = a.access_tier_id\n"
                + "ORDER BY workspace_id\n"
                + "LIMIT %d\n"
                + "OFFSET %d",
            limit, offset),
        (rs, unused) ->
            new ReportingWorkspace()
                .accessTierShortName(rs.getString("access_tier_short_name"))
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
                .workspaceId(rs.getLong("workspace_id"))
                .workspaceNamespace(rs.getString("workspace_namespace")));
  }

  @Override
  public int getWorkspacesCount() {
    return jdbcTemplate.queryForObject("SELECT count(*) FROM workspace", Integer.class);
  }

  @Override
  public int getUserCount() {
    return jdbcTemplate.queryForObject("SELECT count(*) FROM user", Integer.class);
  }

  /** Converts aggregated storage enums to String value. e.g. 0. 8 -> BA, MS. */
  private static String convertListEnumFromStorage(
      String stringEnums, Function<Short, String> convertDbEnum) {
    if (Strings.isNullOrEmpty(stringEnums)) {
      return "";
    }
    return Arrays.stream(stringEnums.split(","))
        .map(e -> convertDbEnum.apply(Short.parseShort(e)))
        .collect(Collectors.joining(","));
  }
}
