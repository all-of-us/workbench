package org.pmiops.workbench.db.jdbc;

import static org.pmiops.workbench.db.model.DbStorageEnums.degreeFromStorage;
import static org.pmiops.workbench.db.model.DbStorageEnums.disabilityFromStorage;
import static org.pmiops.workbench.db.model.DbStorageEnums.educationFromStorage;
import static org.pmiops.workbench.db.model.DbStorageEnums.ethnicityFromStorage;
import static org.pmiops.workbench.db.model.DbStorageEnums.genderIdentityFromStorage;
import static org.pmiops.workbench.db.model.DbStorageEnums.institutionalRoleFromStorage;
import static org.pmiops.workbench.db.model.DbStorageEnums.organizationTypeFromStorage;
import static org.pmiops.workbench.db.model.DbStorageEnums.raceFromStorage;
import static org.pmiops.workbench.db.model.DbStorageEnums.sexAtBirthFromStorage;
import static org.pmiops.workbench.db.model.DbStorageEnums.workspaceActiveStatusToStorage;
import static org.pmiops.workbench.db.model.DbUser.USER_APP_NAME_PREFIX;
import static org.pmiops.workbench.leonardo.LeonardoAppUtils.appServiceNameToAppType;
import static org.pmiops.workbench.utils.BillingUtils.getBillingAccountType;
import static org.pmiops.workbench.utils.mappers.CommonMappers.offsetDateTimeUtc;

import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.common.base.Strings;
import jakarta.inject.Provider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.pmiops.workbench.access.AccessTierService;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.model.DbUser.DbGeneralDiscoverySource;
import org.pmiops.workbench.model.AppType;
import org.pmiops.workbench.model.InstitutionMembershipRequirement;
import org.pmiops.workbench.model.NewUserSatisfactionSurveySatisfaction;
import org.pmiops.workbench.model.PartnerDiscoverySource;
import org.pmiops.workbench.model.ReportingCohort;
import org.pmiops.workbench.model.ReportingDataset;
import org.pmiops.workbench.model.ReportingDatasetDomainIdValue;
import org.pmiops.workbench.model.ReportingInstitution;
import org.pmiops.workbench.model.ReportingLeonardoAppUsage;
import org.pmiops.workbench.model.ReportingNewUserSatisfactionSurvey;
import org.pmiops.workbench.model.ReportingUser;
import org.pmiops.workbench.model.ReportingUserGeneralDiscoverySource;
import org.pmiops.workbench.model.ReportingUserPartnerDiscoverySource;
import org.pmiops.workbench.model.ReportingWorkspace;
import org.pmiops.workbench.model.ReportingWorkspaceFreeTierUsage;
import org.pmiops.workbench.model.WorkspaceActiveStatus;
import org.pmiops.workbench.utils.FieldValues;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class ReportingQueryServiceImpl implements ReportingQueryService {
  private final JdbcTemplate jdbcTemplate;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;
  private final BigQueryService bigQueryService;

  public ReportingQueryServiceImpl(
      JdbcTemplate jdbcTemplate,
      Provider<WorkbenchConfig> workbenchConfigProvider,
      BigQueryService bigQueryService) {
    this.jdbcTemplate = jdbcTemplate;
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.bigQueryService = bigQueryService;
  }

  @Override
  public List<ReportingWorkspaceFreeTierUsage> getWorkspaceFreeTierUsageBatch(
      long limit, long offset) {
    return jdbcTemplate.query(
        String.format(
            "SELECT\n"
                + "  cost,\n"
                + "  user_id,\n"
                + "  workspace_id\n"
                + "FROM workspace_free_tier_usage\n"
                + "  LIMIT %d\n"
                + "  OFFSET %d",
            limit, offset),
        (rs, unused) ->
            new ReportingWorkspaceFreeTierUsage()
                .cost(rs.getDouble("cost"))
                .userId(rs.getLong("user_id"))
                .workspaceId(rs.getLong("workspace_id")));
  }

  @Override
  public List<ReportingNewUserSatisfactionSurvey> getNewUserSatisfactionSurveyBatch(
      long limit, long offset) {
    return jdbcTemplate.query(
        String.format(
            "SELECT\n"
                + "  new_user_satisfaction_survey_id,\n"
                + "  user_id,\n"
                + "  creation_time,\n"
                + "  satisfaction,\n"
                + "  additional_info\n"
                + "FROM new_user_satisfaction_survey\n"
                + "  LIMIT %d\n"
                + "  OFFSET %d",
            limit, offset),
        (rs, unused) ->
            new ReportingNewUserSatisfactionSurvey()
                .id(rs.getLong("new_user_satisfaction_survey_id"))
                .userId(rs.getLong("user_id"))
                .created(offsetDateTimeUtc(rs.getTimestamp("creation_time")))
                // Users cannot modify this survey, so `modified` is the same as `created`.
                .modified(offsetDateTimeUtc(rs.getTimestamp("creation_time")))
                .satisfaction(
                    NewUserSatisfactionSurveySatisfaction.valueOf(rs.getString("satisfaction")))
                .additionalInfo(rs.getString("additional_info")));
  }

  @Override
  public List<ReportingUserGeneralDiscoverySource> getUserGeneralDiscoverySourceBatch(
      long limit, long offset) {
    // Note: Only populate other_text when user_general_discovery_source is OTHER
    return jdbcTemplate.query(
        String.format(
            "SELECT\n"
                + "  us.user_id as user_id,\n"
                + "  us.source as answer,\n"
                + "  u.user_general_discovery_source_other_text as other_text\n"
                + "FROM user_general_discovery_source us\n"
                + " JOIN user u\n"
                + "   ON us.user_id = u.user_id\n"
                + "  LIMIT %d\n"
                + "  OFFSET %d",
            limit, offset),
        (rs, unused) ->
            new ReportingUserGeneralDiscoverySource()
                .userId(rs.getLong("user_id"))
                .answer(rs.getString("answer"))
                .otherText(
                    rs.getString("answer").equals(DbGeneralDiscoverySource.OTHER.toString())
                        ? rs.getString("other_text")
                        : null));
  }

  @Override
  public List<ReportingUserPartnerDiscoverySource> getUserPartnerDiscoverySourceBatch(
      long limit, long offset) {
    // Note: Only populate other_text when user_general_discovery_source is OTHER
    return jdbcTemplate.query(
        String.format(
            "SELECT\n"
                + "  us.user_id as user_id,\n"
                + "  us.source as answer,\n"
                + "  u.user_partner_discovery_source_other_text as other_text\n"
                + "FROM user_partner_discovery_source us\n"
                + " JOIN user u\n"
                + "   ON us.user_id = u.user_id\n"
                + "  LIMIT %d\n"
                + "  OFFSET %d",
            limit, offset),
        (rs, unused) ->
            new ReportingUserPartnerDiscoverySource()
                .userId(rs.getLong("user_id"))
                .answer(rs.getString("answer"))
                .otherText(
                    rs.getString("answer").equals(PartnerDiscoverySource.OTHER.toString())
                        ? rs.getString("other_text")
                        : null));
  }

  @Override
  public List<ReportingCohort> getCohortBatch(long limit, long offset) {
    return jdbcTemplate.query(
        String.format(
            "SELECT \n"
                + "  cohort_id,\n"
                + "  creation_time,\n"
                + "  creator_id,\n"
                + "  description,\n"
                + "  last_modified_time,\n"
                + "  name,\n"
                + "  workspace_id\n"
                + "FROM cohort"
                + "  LIMIT %d\n"
                + "  OFFSET %d",
            limit, offset),
        (rs, unused) ->
            new ReportingCohort()
                .cohortId(rs.getLong("cohort_id"))
                .creationTime(offsetDateTimeUtc(rs.getTimestamp("creation_time")))
                .creatorId(rs.getLong("creator_id"))
                .description(rs.getString("description"))
                .lastModifiedTime(offsetDateTimeUtc(rs.getTimestamp("last_modified_time")))
                .name(rs.getString("name"))
                .workspaceId(rs.getLong("workspace_id")));
  }

  @Override
  public List<ReportingDataset> getDatasetBatch(long limit, long offset) {
    return jdbcTemplate.query(
        String.format(
            "SELECT \n"
                + "  creation_time,\n"
                + "  creator_id,\n"
                + "  data_set_id,\n"
                + "  description,\n"
                + "  includes_all_participants,\n"
                + "  last_modified_time,\n"
                + "  name,\n"
                + "  workspace_id\n"
                + "FROM data_set\n"
                + "  LIMIT %d\n"
                + "  OFFSET %d",
            limit, offset),
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
  public List<ReportingDatasetDomainIdValue> getDatasetDomainIdValueBatch(long limit, long offset) {
    return jdbcTemplate.query(
        String.format(
            "SELECT data_set_id, domain_id, value\n"
                + "FROM data_set_values\n"
                + "  LIMIT %d\n"
                + "  OFFSET %d",
            limit, offset),
        (rs, unused) ->
            new ReportingDatasetDomainIdValue()
                .datasetId(rs.getLong("data_set_id"))
                .domainId(rs.getString("domain_id"))
                .value(rs.getString("value")));
  }

  @Override
  public List<ReportingInstitution> getInstitutionBatch(long limit, long offset) {
    return jdbcTemplate.query(
        String.format(
            "SELECT \n"
                + "  i.display_name,\n"
                + "  i.institution_id,\n"
                + "  i.organization_type_enum,\n"
                + "  i.organization_type_other_text,\n"
                + "  i.short_name,\n"
                + "  itr.requirement_enum\n"
                + "FROM institution i\n"
                + "JOIN institution_tier_requirement itr\n"
                + "   ON i.institution_id=itr.institution_id\n"
                + "JOIN access_tier at\n"
                + "   ON itr.access_tier_id=at.access_tier_id\n"
                + "WHERE at.short_name='"
                + AccessTierService.REGISTERED_TIER_SHORT_NAME
                + "'\n"
                + "  LIMIT %d\n"
                + "  OFFSET %d",
            limit,
            offset),
        (rs, unused) ->
            new ReportingInstitution()
                .displayName(rs.getString("display_name"))
                .institutionId(rs.getLong("institution_id"))
                .organizationTypeEnum(
                    organizationTypeFromStorage(rs.getShort("organization_type_enum")))
                .organizationTypeOtherText(rs.getString("organization_type_other_text"))
                .shortName(rs.getString("short_name"))
                .registeredTierRequirement(
                    InstitutionMembershipRequirement.valueOf(rs.getString("requirement_enum"))));
  }

  @Override
  public List<ReportingUser> getUserBatch(long limit, long offset) {
    return jdbcTemplate.query(
        String.format(
            "SELECT \n"
                + "  u.user_id,\n"
                + "  u.area_of_research,\n"
                + "  uamrt.compliance_training_bypass_time,\n"
                + "  uamrt.compliance_training_completion_time,\n"
                + "  u.contact_email,\n"
                + "  u.creation_time,\n"
                + "  uamd.ducc_bypass_time,\n"
                + "  uamd.ducc_completion_time,\n"
                + "  ducc.signed_version AS ducc_signed_version,\n"
                + "  u.demographic_survey_completion_time,\n"
                + "  u.disabled,\n"
                + "  uame.era_commons_bypass_time,\n"
                + "  uame.era_commons_completion_time,\n"
                + "  uami.identity_bypass_time,\n"
                + "  uami.identity_completion_time,\n"
                + "  iv.identity_verification_system,\n"
                + "  u.family_name,\n"
                // temporary solution for RW-6566
                + "  uat.first_enabled AS first_registration_completion_time,\n"
                + "  uatr.first_enabled AS registered_tier_first_enabled_time,\n"
                + "  uatc.first_enabled AS controlled_tier_first_enabled_time,\n"
                + "  u.first_sign_in_time,\n"
                + "  u.free_tier_credits_limit_dollars_override,\n"
                + "  u.given_name,\n"
                + "  u.last_modified_time,\n"
                + "  u.professional_url,\n"
                + "  uamt.two_factor_auth_bypass_time,\n"
                + "  uamt.two_factor_auth_completion_time,\n"
                + "  u.email AS username,\n"
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
                + "  LEFT OUTER JOIN user_verified_institutional_affiliation AS via on u.user_id = via.user_id\n"
                + "  LEFT OUTER JOIN user_code_of_conduct_agreement AS ducc on u.user_id = ducc.user_id\n"
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
                + "  LEFT OUTER JOIN ( "
                + "    SELECT uat.user_id, uat.first_enabled FROM user_access_tier uat "
                + "    JOIN access_tier at ON at.access_tier_id = uat.access_tier_id "
                + "    WHERE at.short_name = 'registered' "
                + "  ) uatr ON u.user_id = uatr.user_id "
                + "  LEFT OUTER JOIN ( "
                + "    SELECT uat.user_id, uat.first_enabled FROM user_access_tier uat "
                + "    JOIN access_tier at ON at.access_tier_id = uat.access_tier_id "
                + "    WHERE at.short_name = 'controlled' "
                + "  ) uatc ON u.user_id = uatc.user_id "
                // temporary solution to RW-6566: retrieve first_enabled from user_access_tier
                // for 'registered' entries as a substitute for first_registration_completion_time
                + "  LEFT OUTER JOIN ( "
                + "    SELECT uat.user_id, uat.first_enabled FROM user_access_tier uat "
                + "    JOIN access_tier at ON at.access_tier_id = uat.access_tier_id "
                + "    WHERE uat.access_status = 1 AND at.short_name = 'registered' "
                + "  ) uat ON u.user_id = uat.user_id "
                // end temporary solution to RW-6566
                + "  LEFT OUTER JOIN ( "
                + "    SELECT uam.user_id, "
                + "      uam.bypass_time AS era_commons_bypass_time, "
                + "      uam.completion_time AS era_commons_completion_time "
                + "    FROM user_access_module uam "
                + "    JOIN access_module am ON am.access_module_id=uam.access_module_id "
                + "    WHERE am.name = 'ERA_COMMONS' "
                + "  ) uame ON u.user_id = uame.user_id "
                + "  LEFT OUTER JOIN ( "
                + "    SELECT uam.user_id, "
                + "      uam.bypass_time AS identity_bypass_time, "
                + "      uam.completion_time AS identity_completion_time "
                + "    FROM user_access_module uam "
                + "    JOIN access_module am ON am.access_module_id=uam.access_module_id "
                + "    WHERE am.name = 'IDENTITY' "
                + "  ) uami ON u.user_id = uami.user_id "
                + "  LEFT OUTER JOIN identity_verification AS iv on u.user_id = iv.user_id"
                + "  LEFT OUTER JOIN ( "
                + "    SELECT uam.user_id, "
                + "      uam.bypass_time AS two_factor_auth_bypass_time, "
                + "      uam.completion_time AS two_factor_auth_completion_time "
                + "    FROM user_access_module uam "
                + "    JOIN access_module am ON am.access_module_id=uam.access_module_id "
                + "    WHERE am.name = 'TWO_FACTOR_AUTH' "
                + "  ) uamt ON u.user_id = uamt.user_id "
                + "  LEFT OUTER JOIN ( "
                + "    SELECT uam.user_id, "
                + "      uam.bypass_time AS compliance_training_bypass_time, "
                + "      uam.completion_time AS compliance_training_completion_time "
                + "    FROM user_access_module uam "
                + "    JOIN access_module am ON am.access_module_id=uam.access_module_id "
                + "    WHERE am.name = 'RT_COMPLIANCE_TRAINING' "
                + "  ) uamrt ON u.user_id = uamrt.user_id "
                + "  LEFT OUTER JOIN ( "
                + "    SELECT uam.user_id, "
                + "      uam.bypass_time AS ducc_bypass_time, "
                + "      uam.completion_time AS ducc_completion_time "
                + "    FROM user_access_module uam "
                + "    JOIN access_module am ON am.access_module_id=uam.access_module_id "
                + "    WHERE am.name = 'DATA_USER_CODE_OF_CONDUCT' "
                + "  ) uamd ON u.user_id = uamd.user_id "
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
                .contactEmail(rs.getString("contact_email"))
                .creationTime(offsetDateTimeUtc(rs.getTimestamp("creation_time")))
                .accessTierShortNames(rs.getString("access_tier_short_names"))
                .duccBypassTime(offsetDateTimeUtc(rs.getTimestamp("ducc_bypass_time")))
                .duccCompletionTime(offsetDateTimeUtc(rs.getTimestamp("ducc_completion_time")))
                .duccSignedVersion(rs.getInt("ducc_signed_version"))
                .demographicSurveyCompletionTime(
                    offsetDateTimeUtc(rs.getTimestamp("demographic_survey_completion_time")))
                .disabled(rs.getBoolean("disabled"))
                .eraCommonsBypassTime(offsetDateTimeUtc(rs.getTimestamp("era_commons_bypass_time")))
                .eraCommonsCompletionTime(
                    offsetDateTimeUtc(rs.getTimestamp("era_commons_completion_time")))
                .identityBypassTime(offsetDateTimeUtc(rs.getTimestamp("identity_bypass_time")))
                .identityCompletionTime(
                    offsetDateTimeUtc(rs.getTimestamp("identity_completion_time")))
                .identityVerificationSystem(rs.getString("identity_verification_system"))
                .familyName(rs.getString("family_name"))
                .firstRegistrationCompletionTime(
                    offsetDateTimeUtc(rs.getTimestamp("first_registration_completion_time")))
                .registeredTierFirstEnabledTime(
                    offsetDateTimeUtc(rs.getTimestamp("registered_tier_first_enabled_time")))
                .controlledTierFirstEnabledTime(
                    offsetDateTimeUtc(rs.getTimestamp("controlled_tier_first_enabled_time")))
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
  public List<ReportingWorkspace> getWorkspaceBatch(long limit, long offset) {
    String sql =
        "SELECT \n"
            + "  a.short_name AS access_tier_short_name,\n"
            + "  billing_account_name,\n"
            + "  w.cdr_version_id AS cdr_version_id,\n"
            + "  w.creation_time AS creation_time,\n"
            + "  creator_id,\n"
            + "  disseminate_research_other,\n"
            + "  fw.category AS featured_workspace_category,\n"
            + "  last_modified_time,\n"
            + "  w.name AS name,\n"
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
            + "  w.workspace_id,\n"
            + "  workspace_namespace\n"
            + "FROM workspace w\n"
            // some Tanagra workspaces don't have CDR version IDs
            + "  LEFT JOIN cdr_version c ON w.cdr_version_id = c.cdr_version_id\n"
            + "  LEFT JOIN access_tier a ON c.access_tier = a.access_tier_id\n"
            // most workspaces are not Featured
            + "  LEFT OUTER JOIN featured_workspace fw ON w.workspace_id = fw.workspace_id\n"
            + "WHERE active_status = ? \n"
            + "ORDER BY w.workspace_id\n"
            + "LIMIT ? \n"
            + "OFFSET ?";
    return jdbcTemplate.query(
        sql,
        (rs, unused) ->
            new ReportingWorkspace()
                .accessTierShortName(rs.getString("access_tier_short_name"))
                .billingAccountType(
                    getBillingAccountType(
                        rs.getString("billing_account_name"), workbenchConfigProvider.get()))
                .cdrVersionId(rs.getLong("cdr_version_id"))
                .creationTime(offsetDateTimeUtc(rs.getTimestamp("creation_time")))
                .creatorId(rs.getLong("creator_id"))
                .disseminateResearchOther(rs.getString("disseminate_research_other"))
                .featuredWorkspaceCategory(rs.getString("featured_workspace_category"))
                .lastModifiedTime(offsetDateTimeUtc(rs.getTimestamp("last_modified_time")))
                .name(rs.getString("name"))
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
                .workspaceNamespace(rs.getString("workspace_namespace")),
        workspaceActiveStatusToStorage(WorkspaceActiveStatus.ACTIVE),
        limit,
        offset);
  }

  @Override
  public int getTableRowCount(String rwbTableName) {
    return jdbcTemplate.queryForObject("SELECT count(*) FROM " + rwbTableName, Integer.class);
  }

  @Override
  public int getAppUsageRowCount() {
    if (!workbenchConfigProvider.get().reporting.exportTerraDataWarehouse) {
      return 0;
    }

    var queryString =
        "SELECT count(*) "
            + "FROM `"
            + workbenchConfigProvider.get().reporting.terraWarehouseLeoAppUsageTableId
            + "` au "
            + "JOIN `"
            + workbenchConfigProvider.get().reporting.terraWarehouseLeoAppTableId
            + "` a on a.id = au.appId where STARTS_WITH(appName, \""
            + USER_APP_NAME_PREFIX
            + "\")";

    final QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(queryString).build();
    var res = bigQueryService.executeQuery(queryConfig);
    return res.getValues().iterator().next().get(0).getNumericValue().intValue();
  }

  @Override
  public int getActiveWorkspaceCount() {
    return jdbcTemplate.queryForObject(
        "SELECT count(*) FROM workspace WHERE active_status = "
            + workspaceActiveStatusToStorage(WorkspaceActiveStatus.ACTIVE),
        Integer.class);
  }

  @Override
  public List<ReportingLeonardoAppUsage> getLeonardoAppUsageBatch(long limit, long offset) {
    if (!workbenchConfigProvider.get().reporting.exportTerraDataWarehouse) {
      // Skip querying data if not enabled, and just return empty list instead.
      return new ArrayList<>();
    }
    var queryString =
        String.format(
            "SELECT appId, appName, status, createdDate, destroyedDate, "
                + "startTime, stopTime, creator, customEnvironmentVariables "
                + "FROM `"
                + workbenchConfigProvider.get().reporting.terraWarehouseLeoAppUsageTableId
                + "` au "
                + "JOIN `"
                + workbenchConfigProvider.get().reporting.terraWarehouseLeoAppTableId
                + "` a on a.id = au.appId where STARTS_WITH(appName, \""
                + USER_APP_NAME_PREFIX
                + "\") "
                + "LIMIT %d\n"
                + "OFFSET %d",
            limit,
            offset);

    final QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(queryString).build();

    List<ReportingLeonardoAppUsage> queryResults = new ArrayList<>();
    for (FieldValueList row : bigQueryService.executeQuery(queryConfig).getValues()) {
      ReportingLeonardoAppUsage res = new ReportingLeonardoAppUsage();
      FieldValues.getLong(row, "appId").ifPresent(res::setAppId);
      FieldValues.getString(row, "status").ifPresent(res::setStatus);
      FieldValues.getString(row, "creator").ifPresent(res::setCreator);
      FieldValues.getDateTime(row, "createdDate").ifPresent(res::setCreatedDate);
      FieldValues.getDateTime(row, "destroyedDate").ifPresent(res::setDestroyedDate);
      FieldValues.getDateTime(row, "startTime").ifPresent(res::setStartTime);
      FieldValues.getDateTime(row, "stopTime").ifPresent(res::setStopTime);
      FieldValues.getString(row, "customEnvironmentVariables")
          .ifPresent(res::setEnvironmentVariables);
      FieldValues.getString(row, "appName")
          .ifPresent(
              appName -> {
                res.setAppName(appName);
                res.setAppType(
                    appServiceNameToAppType(appName)
                        .map(AppType::toString)
                        .orElse("[unknown app type]"));
              });
      queryResults.add(res);
    }

    return queryResults;
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
