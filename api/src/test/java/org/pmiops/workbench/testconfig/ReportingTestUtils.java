package org.pmiops.workbench.testconfig;

import static com.google.common.truth.Truth.assertThat;
import static org.pmiops.workbench.utils.TimeAssertions.assertTimeApprox;
import static org.pmiops.workbench.utils.mappers.CommonMappers.offsetDateTimeUtc;

import com.google.common.collect.ImmutableList;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import org.pmiops.workbench.access.AccessTierService;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbCohort;
import org.pmiops.workbench.db.model.DbDataset;
import org.pmiops.workbench.db.model.DbNewUserSatisfactionSurvey;
import org.pmiops.workbench.db.model.DbNewUserSatisfactionSurvey.Satisfaction;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.model.BillingAccountType;
import org.pmiops.workbench.model.BillingStatus;
import org.pmiops.workbench.model.InstitutionMembershipRequirement;
import org.pmiops.workbench.model.NewUserSatisfactionSurveySatisfaction;
import org.pmiops.workbench.model.OrganizationType;
import org.pmiops.workbench.model.ReportingCohort;
import org.pmiops.workbench.model.ReportingDataset;
import org.pmiops.workbench.model.ReportingInstitution;
import org.pmiops.workbench.model.ReportingNewUserSatisfactionSurvey;
import org.pmiops.workbench.model.ReportingSnapshot;
import org.pmiops.workbench.model.ReportingWorkspace;
import org.pmiops.workbench.model.ReportingWorkspaceFreeTierUsage;

public class ReportingTestUtils {

  public static final String WORKSPACE__ACCESS_TIER_SHORT_NAME =
      AccessTierService.REGISTERED_TIER_SHORT_NAME;
  public static final BillingAccountType WORKSPACE__BILLING_ACCOUNT_TYPE =
      BillingAccountType.FREE_TIER;
  public static final String WORKSPACE__BILLING_ACCOUNT_ID = "free_tier";
  public static final BillingStatus WORKSPACE__BILLING_STATUS = BillingStatus.ACTIVE;
  public static final Long WORKSPACE__CDR_VERSION_ID = 2L;
  public static final Timestamp WORKSPACE__CREATION_TIME =
      Timestamp.from(Instant.parse("2015-05-08T00:00:00.00Z"));
  public static final Long WORKSPACE__CREATOR_ID = 4L;
  public static final String WORKSPACE__DISSEMINATE_RESEARCH_OTHER = "foo_6";
  public static final Timestamp WORKSPACE__LAST_MODIFIED_TIME =
      Timestamp.from(Instant.parse("2015-05-13T00:00:00.00Z"));
  public static final String WORKSPACE__NAME = "foo_9";
  public static final Boolean WORKSPACE__PUBLISHED = false;
  public static final String WORKSPACE__RP_ADDITIONAL_NOTES = "foo_12";
  public static final Boolean WORKSPACE__RP_ANCESTRY = false;
  public static final String WORKSPACE__RP_ANTICIPATED_FINDINGS = "foo_14";
  public static final Boolean WORKSPACE__RP_APPROVED = false;
  public static final Boolean WORKSPACE__RP_COMMERCIAL_PURPOSE = true;
  public static final Boolean WORKSPACE__RP_CONTROL_SET = false;
  public static final Boolean WORKSPACE__RP_DISEASE_FOCUSED_RESEARCH = true;
  public static final String WORKSPACE__RP_DISEASE_OF_FOCUS = "foo_19";
  public static final Boolean WORKSPACE__RP_DRUG_DEVELOPMENT = true;
  public static final Boolean WORKSPACE__RP_EDUCATIONAL = false;
  public static final Boolean WORKSPACE__RP_ETHICS = true;
  public static final String WORKSPACE__RP_INTENDED_STUDY = "foo_23";
  public static final Boolean WORKSPACE__RP_METHODS_DEVELOPMENT = true;
  public static final String WORKSPACE__RP_OTHER_POPULATION_DETAILS = "foo_25";
  public static final Boolean WORKSPACE__RP_OTHER_PURPOSE = true;
  public static final String WORKSPACE__RP_OTHER_PURPOSE_DETAILS = "foo_27";
  public static final Boolean WORKSPACE__RP_POPULATION_HEALTH = true;
  public static final String WORKSPACE__RP_REASON_FOR_ALL_OF_US = "foo_29";
  public static final Boolean WORKSPACE__RP_REVIEW_REQUESTED = true;
  public static final String WORKSPACE__RP_SCIENTIFIC_APPROACH = "foo_31";
  public static final Boolean WORKSPACE__RP_SOCIAL_BEHAVIORAL = true;
  public static final Timestamp WORKSPACE__RP_TIME_REQUESTED =
      Timestamp.from(Instant.parse("2015-06-07T00:00:00.00Z"));
  public static final Long WORKSPACE__WORKSPACE_ID = 34L;
  public static final String WORKSPACE__WORKSPACE_NAMESPACE = "aou-rw-12345";

  public static final Double WORKSPACE_FREE_TIER_USAGE__COST = 0.500000;
  public static final Long WORKSPACE_FREE_TIER_USAGE__USER_ID = 1L;
  public static final Long WORKSPACE_FREE_TIER_USAGE__WORKSPACE_ID = 2L;

  public static final Long COHORT__COHORT_ID = 0L;
  public static final Timestamp COHORT__CREATION_TIME =
      Timestamp.from(Instant.parse("2015-05-06T00:00:00.00Z"));
  public static final Long COHORT__CREATOR_ID = 2L;
  public static final String COHORT__DESCRIPTION = "foo_4";
  public static final Timestamp COHORT__LAST_MODIFIED_TIME =
      Timestamp.from(Instant.parse("2015-05-10T00:00:00.00Z"));
  public static final String COHORT__NAME = "foo_6";
  public static final Long COHORT__WORKSPACE_ID = 7L;

  public static final String INSTITUTION__DISPLAY_NAME = "foo_0";
  public static final Long INSTITUTION__INSTITUTION_ID = 2L;
  public static final OrganizationType INSTITUTION__ORGANIZATION_TYPE_ENUM =
      OrganizationType.ACADEMIC_RESEARCH_INSTITUTION;
  public static final String INSTITUTION__ORGANIZATION_TYPE_OTHER_TEXT = "foo_4";
  public static final String INSTITUTION__SHORT_NAME = "foo_5";
  public static final InstitutionMembershipRequirement INSTITUTION__REGISTERED_TIER_REQUIREMENT =
      InstitutionMembershipRequirement.DOMAINS;

  public static final Timestamp DATASET__CREATION_TIME =
      Timestamp.from(Instant.parse("2015-05-05T00:00:00.00Z"));
  public static final Long DATASET__CREATOR_ID = 1L;
  public static final Long DATASET__DATASET_ID = 2L;
  public static final String DATASET__DESCRIPTION = "foo_3";
  public static final Boolean DATASET__INCLUDES_ALL_PARTICIPANTS = true;
  public static final Timestamp DATASET__LAST_MODIFIED_TIME =
      Timestamp.from(Instant.parse("2015-05-10T00:00:00.00Z"));
  public static final String DATASET__NAME = "foo_6";
  public static final Short DATASET__PRE_PACKAGED_CONCEPT_SET = 7;
  public static final Long DATASET__WORKSPACE_ID = 8L;

  public static final Long NEW_USER_SATISFACTION_SURVEY__ID = 1L;
  public static final Long NEW_USER_SATISFACTION_SURVEY__USER_ID = 2L;
  public static final Timestamp NEW_USER_SATISFACTION_SURVEY__CREATED =
      Timestamp.from(Instant.parse("2001-01-01T00:00:00.00Z"));
  public static final Timestamp NEW_USER_SATISFACTION_SURVEY__MODIFIED =
      Timestamp.from(Instant.parse("2002-01-01T00:00:00.00Z"));
  public static final NewUserSatisfactionSurveySatisfaction
      NEW_USER_SATISFACTION_SURVEY__SATISFACTION = NewUserSatisfactionSurveySatisfaction.NEUTRAL;
  public static final String NEW_USER_SATISFACTION_SURVEY__ADDITIONAL_INFO = "It's ok.";

  public static ReportingWorkspace createDtoWorkspace() {
    return new ReportingWorkspace()
        .accessTierShortName(WORKSPACE__ACCESS_TIER_SHORT_NAME)
        .billingAccountType(WORKSPACE__BILLING_ACCOUNT_TYPE)
        .billingStatus(WORKSPACE__BILLING_STATUS)
        .cdrVersionId(WORKSPACE__CDR_VERSION_ID)
        .creationTime(offsetDateTimeUtc(WORKSPACE__CREATION_TIME))
        .creatorId(WORKSPACE__CREATOR_ID)
        .disseminateResearchOther(WORKSPACE__DISSEMINATE_RESEARCH_OTHER)
        .lastModifiedTime(offsetDateTimeUtc(WORKSPACE__LAST_MODIFIED_TIME))
        .name(WORKSPACE__NAME)
        .published(WORKSPACE__PUBLISHED)
        .rpAdditionalNotes(WORKSPACE__RP_ADDITIONAL_NOTES)
        .rpAncestry(WORKSPACE__RP_ANCESTRY)
        .rpAnticipatedFindings(WORKSPACE__RP_ANTICIPATED_FINDINGS)
        .rpApproved(WORKSPACE__RP_APPROVED)
        .rpCommercialPurpose(WORKSPACE__RP_COMMERCIAL_PURPOSE)
        .rpControlSet(WORKSPACE__RP_CONTROL_SET)
        .rpDiseaseFocusedResearch(WORKSPACE__RP_DISEASE_FOCUSED_RESEARCH)
        .rpDiseaseOfFocus(WORKSPACE__RP_DISEASE_OF_FOCUS)
        .rpDrugDevelopment(WORKSPACE__RP_DRUG_DEVELOPMENT)
        .rpEducational(WORKSPACE__RP_EDUCATIONAL)
        .rpEthics(WORKSPACE__RP_ETHICS)
        .rpIntendedStudy(WORKSPACE__RP_INTENDED_STUDY)
        .rpMethodsDevelopment(WORKSPACE__RP_METHODS_DEVELOPMENT)
        .rpOtherPopulationDetails(WORKSPACE__RP_OTHER_POPULATION_DETAILS)
        .rpOtherPurpose(WORKSPACE__RP_OTHER_PURPOSE)
        .rpOtherPurposeDetails(WORKSPACE__RP_OTHER_PURPOSE_DETAILS)
        .rpPopulationHealth(WORKSPACE__RP_POPULATION_HEALTH)
        .rpReasonForAllOfUs(WORKSPACE__RP_REASON_FOR_ALL_OF_US)
        .rpReviewRequested(WORKSPACE__RP_REVIEW_REQUESTED)
        .rpScientificApproach(WORKSPACE__RP_SCIENTIFIC_APPROACH)
        .rpSocialBehavioral(WORKSPACE__RP_SOCIAL_BEHAVIORAL)
        .rpTimeRequested(offsetDateTimeUtc(WORKSPACE__RP_TIME_REQUESTED))
        .workspaceId(WORKSPACE__WORKSPACE_ID)
        .workspaceNamespace(WORKSPACE__WORKSPACE_NAMESPACE);
  }

  public static DbWorkspace createDbWorkspace(DbUser creator, DbCdrVersion cdrVersion) {
    final DbWorkspace workspace = new DbWorkspace();
    workspace.setCdrVersion(cdrVersion);
    workspace.setCreationTime(WORKSPACE__CREATION_TIME);
    workspace.setCreator(creator);
    workspace.setDisseminateResearchOther(WORKSPACE__DISSEMINATE_RESEARCH_OTHER);
    workspace.setLastModifiedTime(WORKSPACE__LAST_MODIFIED_TIME);
    workspace.setName(WORKSPACE__NAME);
    workspace.setPublished(WORKSPACE__PUBLISHED);
    workspace.setAdditionalNotes(WORKSPACE__RP_ADDITIONAL_NOTES);
    workspace.setAncestry(WORKSPACE__RP_ANCESTRY);
    workspace.setAnticipatedFindings(WORKSPACE__RP_ANTICIPATED_FINDINGS);
    workspace.setApproved(WORKSPACE__RP_APPROVED);
    workspace.setCommercialPurpose(WORKSPACE__RP_COMMERCIAL_PURPOSE);
    workspace.setControlSet(WORKSPACE__RP_CONTROL_SET);
    workspace.setDiseaseFocusedResearch(WORKSPACE__RP_DISEASE_FOCUSED_RESEARCH);
    workspace.setDiseaseOfFocus(WORKSPACE__RP_DISEASE_OF_FOCUS);
    workspace.setDrugDevelopment(WORKSPACE__RP_DRUG_DEVELOPMENT);
    workspace.setEducational(WORKSPACE__RP_EDUCATIONAL);
    workspace.setEthics(WORKSPACE__RP_ETHICS);
    workspace.setIntendedStudy(WORKSPACE__RP_INTENDED_STUDY);
    workspace.setMethodsDevelopment(WORKSPACE__RP_METHODS_DEVELOPMENT);
    workspace.setOtherPopulationDetails(WORKSPACE__RP_OTHER_POPULATION_DETAILS);
    workspace.setOtherPurpose(WORKSPACE__RP_OTHER_PURPOSE);
    workspace.setOtherPurposeDetails(WORKSPACE__RP_OTHER_PURPOSE_DETAILS);
    workspace.setPopulationHealth(WORKSPACE__RP_POPULATION_HEALTH);
    workspace.setReasonForAllOfUs(WORKSPACE__RP_REASON_FOR_ALL_OF_US);
    workspace.setReviewRequested(WORKSPACE__RP_REVIEW_REQUESTED);
    workspace.setScientificApproach(WORKSPACE__RP_SCIENTIFIC_APPROACH);
    workspace.setSocialBehavioral(WORKSPACE__RP_SOCIAL_BEHAVIORAL);
    workspace.setTimeRequested(WORKSPACE__RP_TIME_REQUESTED);
    workspace.setWorkspaceId(WORKSPACE__WORKSPACE_ID);
    workspace.setWorkspaceNamespace(WORKSPACE__WORKSPACE_NAMESPACE);
    workspace.setBillingAccountName("billingAccounts/" + WORKSPACE__BILLING_ACCOUNT_ID);
    return workspace;
  }

  public static void assertDtoWorkspaceFreeTierUsageFields(
      ReportingWorkspaceFreeTierUsage workspaceFreeTierUsage) {
    assertThat(workspaceFreeTierUsage.getCost()).isEqualTo(WORKSPACE_FREE_TIER_USAGE__COST);
    assertThat(workspaceFreeTierUsage.getUserId()).isEqualTo(WORKSPACE_FREE_TIER_USAGE__USER_ID);
    assertThat(workspaceFreeTierUsage.getWorkspaceId())
        .isEqualTo(WORKSPACE_FREE_TIER_USAGE__WORKSPACE_ID);
  }

  public static ReportingWorkspaceFreeTierUsage createDtoWorkspaceFreeTierUsage() {
    return new ReportingWorkspaceFreeTierUsage()
        .cost(WORKSPACE_FREE_TIER_USAGE__COST)
        .userId(WORKSPACE_FREE_TIER_USAGE__USER_ID)
        .workspaceId(WORKSPACE_FREE_TIER_USAGE__WORKSPACE_ID);
  }

  public static ReportingCohort createReportingCohort() {
    return new ReportingCohort()
        .cohortId(COHORT__COHORT_ID)
        .creationTime(offsetDateTimeUtc(COHORT__CREATION_TIME))
        .creatorId(COHORT__CREATOR_ID)
        .description(COHORT__DESCRIPTION)
        .lastModifiedTime(offsetDateTimeUtc(COHORT__LAST_MODIFIED_TIME))
        .name(COHORT__NAME)
        .workspaceId(COHORT__WORKSPACE_ID);
  }

  public static DbCohort createDbCohort(DbUser creator, DbWorkspace dbWorkspace) {
    final DbCohort cohort = new DbCohort();
    cohort.setCohortId(COHORT__COHORT_ID);
    cohort.setCreationTime(COHORT__CREATION_TIME);
    cohort.setCreator(creator);
    cohort.setDescription(COHORT__DESCRIPTION);
    cohort.setLastModifiedTime(COHORT__LAST_MODIFIED_TIME);
    cohort.setName(COHORT__NAME);
    cohort.setWorkspaceId(dbWorkspace.getWorkspaceId());
    return cohort;
  }

  public static final Instant SNAPSHOT_INSTANT = Instant.parse("2020-01-01T00:00:00.00Z");

  public static ReportingSnapshot EMPTY_SNAPSHOT =
      createEmptySnapshot().captureTimestamp(SNAPSHOT_INSTANT.toEpochMilli());

  private static <T> int oneForNonEmpty(Collection<T> collection) {
    return Math.min(collection.size(), 1);
  }

  public static int countPopulatedTables(ReportingSnapshot reportingSnapshot) {
    return oneForNonEmpty(reportingSnapshot.getCohorts())
        + oneForNonEmpty(reportingSnapshot.getDatasets())
        + oneForNonEmpty(reportingSnapshot.getDatasetCohorts())
        + oneForNonEmpty(reportingSnapshot.getDatasetConceptSets())
        + oneForNonEmpty(reportingSnapshot.getDatasetDomainIdValues())
        + oneForNonEmpty(reportingSnapshot.getInstitutions());
  }

  public static ReportingInstitution createReportingInstitution() {
    return new ReportingInstitution()
        .displayName(INSTITUTION__DISPLAY_NAME)
        .institutionId(INSTITUTION__INSTITUTION_ID)
        .organizationTypeEnum(INSTITUTION__ORGANIZATION_TYPE_ENUM)
        .organizationTypeOtherText(INSTITUTION__ORGANIZATION_TYPE_OTHER_TEXT)
        .shortName(INSTITUTION__SHORT_NAME)
        .registeredTierRequirement(INSTITUTION__REGISTERED_TIER_REQUIREMENT);
  }

  public static void assertInstitutionFields(ReportingInstitution institution) {
    assertThat(institution.getDisplayName()).isEqualTo(INSTITUTION__DISPLAY_NAME);
    assertThat(institution.getInstitutionId()).isEqualTo(INSTITUTION__INSTITUTION_ID);
    assertThat(institution.getOrganizationTypeEnum())
        .isEqualTo(INSTITUTION__ORGANIZATION_TYPE_ENUM);
    assertThat(institution.getOrganizationTypeOtherText())
        .isEqualTo(INSTITUTION__ORGANIZATION_TYPE_OTHER_TEXT);
    assertThat(institution.getShortName()).isEqualTo(INSTITUTION__SHORT_NAME);
  }

  public static ReportingDataset createReportingDataset() {
    return new ReportingDataset()
        .creationTime(offsetDateTimeUtc(DATASET__CREATION_TIME))
        .creatorId(DATASET__CREATOR_ID)
        .datasetId(DATASET__DATASET_ID)
        .description(DATASET__DESCRIPTION)
        .includesAllParticipants(DATASET__INCLUDES_ALL_PARTICIPANTS)
        .lastModifiedTime(offsetDateTimeUtc(DATASET__LAST_MODIFIED_TIME))
        .name(DATASET__NAME)
        .workspaceId(DATASET__WORKSPACE_ID);
  }

  public static void assertDatasetFields(ReportingDataset dataset) {
    assertTimeApprox(dataset.getCreationTime(), DATASET__CREATION_TIME);
    assertThat(dataset.getCreatorId()).isEqualTo(DATASET__CREATOR_ID);
    assertThat(dataset.getDatasetId()).isEqualTo(DATASET__DATASET_ID);
    assertThat(dataset.getDescription()).isEqualTo(DATASET__DESCRIPTION);
    assertThat(dataset.isIncludesAllParticipants()).isEqualTo(DATASET__INCLUDES_ALL_PARTICIPANTS);
    assertTimeApprox(dataset.getLastModifiedTime(), DATASET__LAST_MODIFIED_TIME);
    assertThat(dataset.getName()).isEqualTo(DATASET__NAME);
    assertThat(dataset.getWorkspaceId()).isEqualTo(DATASET__WORKSPACE_ID);
  }

  public static DbDataset createDbDataset(long workspaceId) {
    final DbDataset dataset = new DbDataset();
    dataset.setCreationTime(DATASET__CREATION_TIME);
    dataset.setCreatorId(DATASET__CREATOR_ID);
    dataset.setDescription(DATASET__DESCRIPTION);
    dataset.setIncludesAllParticipants(DATASET__INCLUDES_ALL_PARTICIPANTS);
    dataset.setLastModifiedTime(DATASET__LAST_MODIFIED_TIME);
    dataset.setName(DATASET__NAME);
    dataset.setPrePackagedConceptSet(ImmutableList.of(DATASET__PRE_PACKAGED_CONCEPT_SET));
    dataset.setWorkspaceId(workspaceId);
    return dataset;
  }

  public static ReportingNewUserSatisfactionSurvey createReportingNewUserSatisfactionSurvey() {
    return new ReportingNewUserSatisfactionSurvey()
        .id(NEW_USER_SATISFACTION_SURVEY__ID)
        .userId(NEW_USER_SATISFACTION_SURVEY__USER_ID)
        .created(offsetDateTimeUtc(NEW_USER_SATISFACTION_SURVEY__CREATED))
        .modified(offsetDateTimeUtc(NEW_USER_SATISFACTION_SURVEY__MODIFIED))
        .satisfaction(NEW_USER_SATISFACTION_SURVEY__SATISFACTION)
        .additionalInfo(NEW_USER_SATISFACTION_SURVEY__ADDITIONAL_INFO);
  }

  public static DbNewUserSatisfactionSurvey createDbNewUserSatisfactionSurvey(DbUser user) {
    return new DbNewUserSatisfactionSurvey()
        .setUser(user)
        .setSatisfaction(Satisfaction.SATISFIED)
        .setAdditionalInfo("Love it!");
  }

  public static ReportingSnapshot createEmptySnapshot() {
    return new ReportingSnapshot()
        .cohorts(new ArrayList<>())
        .datasets(new ArrayList<>())
        .datasetConceptSets(new ArrayList<>())
        .datasetDomainIdValues(new ArrayList<>())
        .datasetCohorts(new ArrayList<>())
        .institutions(new ArrayList<>())
        .workspaceFreeTierUsage(new ArrayList<>())
        .newUserSatisfactionSurveys(new ArrayList<>());
  }
}
