package org.pmiops.workbench.testconfig;

import static com.google.common.truth.Truth.assertThat;
import static org.pmiops.workbench.utils.TimeAssertions.assertTimeApprox;
import static org.pmiops.workbench.utils.mappers.CommonMappers.offsetDateTimeUtc;

import com.google.common.collect.ImmutableList;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbCohort;
import org.pmiops.workbench.db.model.DbDataset;
import org.pmiops.workbench.db.model.DbInstitution;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.model.BillingAccountType;
import org.pmiops.workbench.model.BillingStatus;
import org.pmiops.workbench.model.DuaType;
import org.pmiops.workbench.model.OrganizationType;
import org.pmiops.workbench.model.ReportingCohort;
import org.pmiops.workbench.model.ReportingDataset;
import org.pmiops.workbench.model.ReportingInstitution;
import org.pmiops.workbench.model.ReportingSnapshot;
import org.pmiops.workbench.model.ReportingWorkspace;
import org.pmiops.workbench.model.ReportingWorkspaceFreeTierUsage;

public class ReportingTestUtils {

  public static final BillingAccountType WORKSPACE__BILLING_ACCOUNT_TYPE =
      BillingAccountType.FREE_TIER;
  public static final BillingStatus WORKSPACE__BILLING_STATUS = BillingStatus.ACTIVE;
  public static final Long WORKSPACE__CDR_VERSION_ID = 2L;
  public static final Timestamp WORKSPACE__CREATION_TIME =
      Timestamp.from(Instant.parse("2015-05-08T00:00:00.00Z"));
  public static final Long WORKSPACE__CREATOR_ID = 4L;
  public static final String WORKSPACE__DISSEMINATE_RESEARCH_OTHER = "foo_6";
  public static final Timestamp WORKSPACE__LAST_ACCESSED_TIME =
      Timestamp.from(Instant.parse("2015-05-12T00:00:00.00Z"));
  public static final Timestamp WORKSPACE__LAST_MODIFIED_TIME =
      Timestamp.from(Instant.parse("2015-05-13T00:00:00.00Z"));
  public static final String WORKSPACE__NAME = "foo_9";
  public static final Short WORKSPACE__NEEDS_RP_REVIEW_PROMPT = 10;
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

  // This code was generated using reporting-wizard.rb at 2021-01-05T17:36:27-08:00.
  // Manual modification should be avoided if possible as this is a one-time generation
  // and does not run on every build and updates must be merged manually for now.

  public static final Double WORKSPACE_FREE_TIER_USAGE__COST = 0.500000;
  public static final Long WORKSPACE_FREE_TIER_USAGE__USER_ID = 1L;
  public static final Long WORKSPACE_FREE_TIER_USAGE__WORKSPACE_ID = 2L;

  // All constant values, mocking statements, and assertions in this file are generated. The values
  // are chosen so that errors with transposed columns can be caught.
  // Mapping Short values with valid enums can be tricky, and currently there are
  // a handful of places where we have to use use a Short in the projection interface but an Enum
  //  type in the model class. An example of such a manual fix is the following:
  // .dataUseAgreementSignedVersion(USER__DATA_USE_AGREEMENT_SIGNED_VERSION.longValue())

  // This code was generated using reporting-wizard.rb at 2020-09-24T13:40:02-04:00.
  // Manual modification should be avoided if possible as this is a one-time generation
  // and does not run on every build and updates must be merged manually for now.

  public static final Long COHORT__COHORT_ID = 0L;
  public static final Timestamp COHORT__CREATION_TIME =
      Timestamp.from(Instant.parse("2015-05-06T00:00:00.00Z"));
  public static final Long COHORT__CREATOR_ID = 2L;
  public static final String COHORT__CRITERIA = "foo_3";
  public static final String COHORT__DESCRIPTION = "foo_4";
  public static final Timestamp COHORT__LAST_MODIFIED_TIME =
      Timestamp.from(Instant.parse("2015-05-10T00:00:00.00Z"));
  public static final String COHORT__NAME = "foo_6";
  public static final Long COHORT__WORKSPACE_ID = 7L;

  // All constant values, mocking statements, and assertions in this file are generated. The values
  // are chosen so that errors with transposed columns can be caught.
  // Mapping Short values with valid enums can be tricky, and currently there are
  // a handful of places where we have to use use a Short in the projection interface but an Enum
  //  type in the model class. An example of such a manual fix is the following:
  // .dataUseAgreementSignedVersion(USER__DATA_USE_AGREEMENT_SIGNED_VERSION.longValue())

  // This code was generated using reporting-wizard.rb at 2020-10-05T09:51:25-04:00.
  // Manual modification should be avoided if possible as this is a one-time generation
  // and does not run on every build and updates must be merged manually for now.

  public static final String INSTITUTION__DISPLAY_NAME = "foo_0";
  public static final DuaType INSTITUTION__DUA_TYPE_ENUM = DuaType.MASTER;
  public static final Long INSTITUTION__INSTITUTION_ID = 2L;
  public static final OrganizationType INSTITUTION__ORGANIZATION_TYPE_ENUM =
      OrganizationType.ACADEMIC_RESEARCH_INSTITUTION;
  public static final String INSTITUTION__ORGANIZATION_TYPE_OTHER_TEXT = "foo_4";
  public static final String INSTITUTION__SHORT_NAME = "foo_5";
  // All constant values, mocking statements, and assertions in this file are generated. The values
  // are chosen so that errors with transposed columns can be caught.
  // Mapping Short values with valid enums can be tricky, and currently there are
  // a handful of places where we have to use use a Short in the projection interface but an Enum
  //  type in the model class. An example of such a manual fix is the following:
  // .dataUseAgreementSignedVersion(USER__DATA_USE_AGREEMENT_SIGNED_VERSION.longValue())

  // This code was generated using reporting-wizard.rb at 2020-11-05T14:31:23-05:00.
  // Manual modification should be avoided if possible as this is a one-time generation
  // and does not run on every build and updates must be merged manually for now.

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

  public static void assertDtoWorkspaceFields(
      ReportingWorkspace workspace,
      long expectedWorkspaceId,
      long expectedCdrVersionId,
      long expectedCreatorId) {
    assertThat(workspace.getBillingAccountType()).isEqualTo(WORKSPACE__BILLING_ACCOUNT_TYPE);
    assertThat(workspace.getBillingStatus()).isEqualTo(WORKSPACE__BILLING_STATUS);
    assertThat(workspace.getCdrVersionId()).isEqualTo(expectedCdrVersionId);
    assertTimeApprox(workspace.getCreationTime(), WORKSPACE__CREATION_TIME);
    assertThat(workspace.getCreatorId()).isEqualTo(expectedCreatorId);
    assertThat(workspace.getDisseminateResearchOther())
        .isEqualTo(WORKSPACE__DISSEMINATE_RESEARCH_OTHER);
    assertTimeApprox(workspace.getLastAccessedTime(), WORKSPACE__LAST_ACCESSED_TIME);
    assertTimeApprox(workspace.getLastModifiedTime(), WORKSPACE__LAST_MODIFIED_TIME);
    assertThat(workspace.getName()).isEqualTo(WORKSPACE__NAME);
    assertThat(workspace.getNeedsRpReviewPrompt()).isEqualTo(WORKSPACE__NEEDS_RP_REVIEW_PROMPT);
    assertThat(workspace.getPublished()).isEqualTo(WORKSPACE__PUBLISHED);
    assertThat(workspace.getRpAdditionalNotes()).isEqualTo(WORKSPACE__RP_ADDITIONAL_NOTES);
    assertThat(workspace.getRpAncestry()).isEqualTo(WORKSPACE__RP_ANCESTRY);
    assertThat(workspace.getRpAnticipatedFindings()).isEqualTo(WORKSPACE__RP_ANTICIPATED_FINDINGS);
    assertThat(workspace.getRpApproved()).isEqualTo(WORKSPACE__RP_APPROVED);
    assertThat(workspace.getRpCommercialPurpose()).isEqualTo(WORKSPACE__RP_COMMERCIAL_PURPOSE);
    assertThat(workspace.getRpControlSet()).isEqualTo(WORKSPACE__RP_CONTROL_SET);
    assertThat(workspace.getRpDiseaseFocusedResearch())
        .isEqualTo(WORKSPACE__RP_DISEASE_FOCUSED_RESEARCH);
    assertThat(workspace.getRpDiseaseOfFocus()).isEqualTo(WORKSPACE__RP_DISEASE_OF_FOCUS);
    assertThat(workspace.getRpDrugDevelopment()).isEqualTo(WORKSPACE__RP_DRUG_DEVELOPMENT);
    assertThat(workspace.getRpEducational()).isEqualTo(WORKSPACE__RP_EDUCATIONAL);
    assertThat(workspace.getRpEthics()).isEqualTo(WORKSPACE__RP_ETHICS);
    assertThat(workspace.getRpIntendedStudy()).isEqualTo(WORKSPACE__RP_INTENDED_STUDY);
    assertThat(workspace.getRpMethodsDevelopment()).isEqualTo(WORKSPACE__RP_METHODS_DEVELOPMENT);
    assertThat(workspace.getRpOtherPopulationDetails())
        .isEqualTo(WORKSPACE__RP_OTHER_POPULATION_DETAILS);
    assertThat(workspace.getRpOtherPurpose()).isEqualTo(WORKSPACE__RP_OTHER_PURPOSE);
    assertThat(workspace.getRpOtherPurposeDetails()).isEqualTo(WORKSPACE__RP_OTHER_PURPOSE_DETAILS);
    assertThat(workspace.getRpPopulationHealth()).isEqualTo(WORKSPACE__RP_POPULATION_HEALTH);
    assertThat(workspace.getRpReasonForAllOfUs()).isEqualTo(WORKSPACE__RP_REASON_FOR_ALL_OF_US);
    assertThat(workspace.getRpReviewRequested()).isEqualTo(WORKSPACE__RP_REVIEW_REQUESTED);
    assertThat(workspace.getRpScientificApproach()).isEqualTo(WORKSPACE__RP_SCIENTIFIC_APPROACH);
    assertThat(workspace.getRpSocialBehavioral()).isEqualTo(WORKSPACE__RP_SOCIAL_BEHAVIORAL);
    assertTimeApprox(workspace.getRpTimeRequested(), WORKSPACE__RP_TIME_REQUESTED);
    assertThat(workspace.getWorkspaceId()).isEqualTo(expectedWorkspaceId);
    assertThat(workspace.getWorkspaceNamespace()).isEqualTo(WORKSPACE__WORKSPACE_NAMESPACE);
  }

  public static void assertDtoWorkspaceFields(ReportingWorkspace workspace) {
    assertDtoWorkspaceFields(
        workspace, WORKSPACE__WORKSPACE_ID, WORKSPACE__CDR_VERSION_ID, WORKSPACE__CREATOR_ID);
  }

  public static ReportingWorkspace createDtoWorkspace() {
    return new ReportingWorkspace()
        .billingAccountType(WORKSPACE__BILLING_ACCOUNT_TYPE)
        .billingStatus(WORKSPACE__BILLING_STATUS)
        .cdrVersionId(WORKSPACE__CDR_VERSION_ID)
        .creationTime(offsetDateTimeUtc(WORKSPACE__CREATION_TIME))
        .creatorId(WORKSPACE__CREATOR_ID)
        .disseminateResearchOther(WORKSPACE__DISSEMINATE_RESEARCH_OTHER)
        .lastAccessedTime(offsetDateTimeUtc(WORKSPACE__LAST_ACCESSED_TIME))
        .lastModifiedTime(offsetDateTimeUtc(WORKSPACE__LAST_MODIFIED_TIME))
        .name(WORKSPACE__NAME)
        .needsRpReviewPrompt(WORKSPACE__NEEDS_RP_REVIEW_PROMPT.intValue()) // manual adjustment
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
    workspace.setBillingAccountType(WORKSPACE__BILLING_ACCOUNT_TYPE);
    workspace.setCdrVersion(cdrVersion);
    workspace.setCreationTime(WORKSPACE__CREATION_TIME);
    workspace.setCreator(creator);
    workspace.setDisseminateResearchOther(WORKSPACE__DISSEMINATE_RESEARCH_OTHER);
    workspace.setLastAccessedTime(WORKSPACE__LAST_ACCESSED_TIME);
    workspace.setLastModifiedTime(WORKSPACE__LAST_MODIFIED_TIME);
    workspace.setName(WORKSPACE__NAME);
    workspace.setNeedsResearchPurposeReviewPrompt(WORKSPACE__NEEDS_RP_REVIEW_PROMPT);
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

  public static void assertCohortFields(ReportingCohort cohort) {
    assertThat(cohort.getCohortId()).isEqualTo(COHORT__COHORT_ID);
    assertTimeApprox(cohort.getCreationTime(), COHORT__CREATION_TIME);
    assertThat(cohort.getCreatorId()).isEqualTo(COHORT__CREATOR_ID);
    assertThat(cohort.getCriteria()).isEqualTo(COHORT__CRITERIA);
    assertThat(cohort.getDescription()).isEqualTo(COHORT__DESCRIPTION);
    assertTimeApprox(cohort.getLastModifiedTime(), COHORT__LAST_MODIFIED_TIME);
    assertThat(cohort.getName()).isEqualTo(COHORT__NAME);
    assertThat(cohort.getWorkspaceId()).isEqualTo(COHORT__WORKSPACE_ID);
  }

  public static ReportingCohort createReportingCohort() {
    return new ReportingCohort()
        .cohortId(COHORT__COHORT_ID)
        .creationTime(offsetDateTimeUtc(COHORT__CREATION_TIME))
        .creatorId(COHORT__CREATOR_ID)
        .criteria(COHORT__CRITERIA)
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
    cohort.setCriteria(COHORT__CRITERIA);
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
        + oneForNonEmpty(reportingSnapshot.getInstitutions())
        + oneForNonEmpty(reportingSnapshot.getUsers())
        + oneForNonEmpty(reportingSnapshot.getWorkspaces());
  }

  public static ReportingInstitution createReportingInstitution() {
    return new ReportingInstitution()
        .displayName(INSTITUTION__DISPLAY_NAME)
        .duaTypeEnum(INSTITUTION__DUA_TYPE_ENUM)
        .institutionId(INSTITUTION__INSTITUTION_ID)
        .organizationTypeEnum(INSTITUTION__ORGANIZATION_TYPE_ENUM)
        .organizationTypeOtherText(INSTITUTION__ORGANIZATION_TYPE_OTHER_TEXT)
        .shortName(INSTITUTION__SHORT_NAME);
  }

  public static DbInstitution createDbInstitution() {
    final DbInstitution institution = new DbInstitution();
    institution.setDisplayName(INSTITUTION__DISPLAY_NAME);
    institution.setDuaTypeEnum(INSTITUTION__DUA_TYPE_ENUM);
    institution.setInstitutionId(INSTITUTION__INSTITUTION_ID);
    institution.setOrganizationTypeEnum(INSTITUTION__ORGANIZATION_TYPE_ENUM);
    institution.setOrganizationTypeOtherText(INSTITUTION__ORGANIZATION_TYPE_OTHER_TEXT);
    institution.setShortName(INSTITUTION__SHORT_NAME);
    return institution;
  }

  public static void assertInstitutionFields(ReportingInstitution institution) {
    assertThat(institution.getDisplayName()).isEqualTo(INSTITUTION__DISPLAY_NAME);
    assertThat(institution.getDuaTypeEnum()).isEqualTo(INSTITUTION__DUA_TYPE_ENUM);
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
    assertThat(dataset.getIncludesAllParticipants()).isEqualTo(DATASET__INCLUDES_ALL_PARTICIPANTS);
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

  public static ReportingSnapshot createEmptySnapshot() {
    return new ReportingSnapshot()
        .cohorts(new ArrayList<>())
        .datasets(new ArrayList<>())
        .datasetConceptSets(new ArrayList<>())
        .datasetDomainIdValues(new ArrayList<>())
        .datasetCohorts(new ArrayList<>())
        .institutions(new ArrayList<>())
        .users(new ArrayList<>())
        .workspaces(new ArrayList<>())
        .workspaceFreeTierUsage(new ArrayList<>());
  }
}
