package org.pmiops.workbench.utils.mappers;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableSet;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.SpringTest;
import org.pmiops.workbench.api.Etags;
import org.pmiops.workbench.cohortreview.mapper.CohortReviewMapperImpl;
import org.pmiops.workbench.cohorts.CohortMapperImpl;
import org.pmiops.workbench.cohorts.CohortService;
import org.pmiops.workbench.conceptset.ConceptSetService;
import org.pmiops.workbench.conceptset.mapper.ConceptSetMapperImpl;
import org.pmiops.workbench.dataset.mapper.DataSetMapperImpl;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbAccessTier;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.db.model.DbWorkspace.BillingMigrationStatus;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspace;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceResponse;
import org.pmiops.workbench.model.BillingStatus;
import org.pmiops.workbench.model.ResearchOutcomeEnum;
import org.pmiops.workbench.model.ResearchPurpose;
import org.pmiops.workbench.model.SpecificPopulationEnum;
import org.pmiops.workbench.model.Workspace;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.model.WorkspaceActiveStatus;
import org.pmiops.workbench.model.WorkspaceResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

public class WorkspaceMapperTest extends SpringTest {
  private static final String FIRECLOUD_NAMESPACE = "aou-xxxxxxx";
  private static final String CREATOR_EMAIL = "ojc@verily.biz";
  private static final long CREATOR_USER_ID = 101L;
  private static final long WORKSPACE_DB_ID = 222L;
  private static final int WORKSPACE_VERSION = 2;
  private static final String WORKSPACE_AOU_NAME = "studyallthethings";
  private static final String WORKSPACE_FIRECLOUD_NAME = "aaaa-bbbb-cccc-dddd";
  private static final String BILLING_ACCOUNT_NAME = "billing-account";
  private static final String GOOGLE_PROJECT = "google_project";

  private static final Timestamp DB_CREATION_TIMESTAMP =
      Timestamp.from(Instant.parse("2000-01-01T00:00:00.00Z"));
  private static final int CDR_VERSION_ID = 2;
  private static final String FIRECLOUD_BUCKET_NAME = "my-favorite-bucket";
  private static final ImmutableSet<SpecificPopulationEnum> SPECIFIC_POPULATIONS =
      ImmutableSet.of(SpecificPopulationEnum.DISABILITY_STATUS, SpecificPopulationEnum.GEOGRAPHY);
  private static final ImmutableSet<ResearchOutcomeEnum> RESEARCH_OUTCOMES =
      ImmutableSet.of(ResearchOutcomeEnum.DECREASE_ILLNESS_BURDEN);
  private static final String DISSEMINATE_FINDINGS_OTHER = "Everywhere except MIT.";
  private static final String ACCESS_TIER_SHORT_NAME = "registered";

  private DbWorkspace sourceDbWorkspace;
  private FirecloudWorkspace sourceFirecloudWorkspace;

  @Autowired private WorkspaceMapper workspaceMapper;

  @TestConfiguration
  @Import({
    CohortMapperImpl.class,
    CohortReviewMapperImpl.class,
    CommonMappers.class,
    ConceptSetMapperImpl.class,
    DataSetMapperImpl.class,
    FirecloudMapperImpl.class,
    WorkspaceMapperImpl.class,
  })
  @MockBean({
    UserDao.class,
    WorkspaceDao.class,
    Clock.class,
    ConceptSetService.class,
    CohortService.class
  })
  static class Configuration {}

  @BeforeEach
  public void setUp() {
    sourceFirecloudWorkspace =
        new FirecloudWorkspace()
            .workspaceId(Long.toString(CREATOR_USER_ID))
            .bucketName(FIRECLOUD_BUCKET_NAME)
            .createdBy(CREATOR_EMAIL)
            .namespace(FIRECLOUD_NAMESPACE)
            .name(WORKSPACE_FIRECLOUD_NAME)
            .googleProject(GOOGLE_PROJECT);

    final DbUser creatorUser = new DbUser();
    creatorUser.setUsername(CREATOR_EMAIL);
    creatorUser.setUserId(CREATOR_USER_ID);

    final DbAccessTier accessTier = new DbAccessTier().setShortName(ACCESS_TIER_SHORT_NAME);

    final DbCdrVersion cdrVersion = new DbCdrVersion();
    cdrVersion.setCdrVersionId(CDR_VERSION_ID);
    cdrVersion.setAccessTier(accessTier);

    sourceDbWorkspace = new DbWorkspace();
    sourceDbWorkspace.setWorkspaceId(WORKSPACE_DB_ID);
    sourceDbWorkspace.setVersion(WORKSPACE_VERSION);
    sourceDbWorkspace.setName(WORKSPACE_AOU_NAME);
    sourceDbWorkspace.setFirecloudName(WORKSPACE_FIRECLOUD_NAME);
    sourceDbWorkspace.setCdrVersion(cdrVersion);
    sourceDbWorkspace.setCreator(creatorUser);
    sourceDbWorkspace.setCreationTime(DB_CREATION_TIMESTAMP);
    sourceDbWorkspace.setLastModifiedTime(DB_CREATION_TIMESTAMP);
    sourceDbWorkspace.setLastAccessedTime(
        Timestamp.from(DB_CREATION_TIMESTAMP.toInstant().plus(Duration.ofMinutes(15))));
    sourceDbWorkspace.setCohorts(Collections.emptySet());
    sourceDbWorkspace.setConceptSets(Collections.emptySet());
    sourceDbWorkspace.setDataSets(Collections.emptySet());
    sourceDbWorkspace.setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.ACTIVE);
    sourceDbWorkspace.setBillingMigrationStatusEnum(BillingMigrationStatus.MIGRATED);
    sourceDbWorkspace.setPublished(false);
    sourceDbWorkspace.setDiseaseFocusedResearch(true);
    sourceDbWorkspace.setDiseaseOfFocus("leukemia");
    sourceDbWorkspace.setMethodsDevelopment(false);
    sourceDbWorkspace.setControlSet(true);
    sourceDbWorkspace.setAncestry(false);
    sourceDbWorkspace.setCommercialPurpose(false);
    sourceDbWorkspace.setSpecificPopulationsEnum(
        ImmutableSet.of(SpecificPopulationEnum.AGE_GROUPS, SpecificPopulationEnum.INCOME_LEVEL));
    sourceDbWorkspace.setSocialBehavioral(false);
    sourceDbWorkspace.setPopulationHealth(true);
    sourceDbWorkspace.setEducational(true);
    sourceDbWorkspace.setDrugDevelopment(false);
    sourceDbWorkspace.setOtherPurpose(true);
    sourceDbWorkspace.setOtherPurposeDetails("I want to discover a new disease.");
    sourceDbWorkspace.setOtherPopulationDetails(null);
    sourceDbWorkspace.setAdditionalNotes(null);
    sourceDbWorkspace.setReasonForAllOfUs("We can't get this data anywhere else.");
    sourceDbWorkspace.setIntendedStudy(null);
    sourceDbWorkspace.setReviewRequested(false);
    sourceDbWorkspace.setApproved(true);
    sourceDbWorkspace.setTimeRequested(DB_CREATION_TIMESTAMP);
    sourceDbWorkspace.setBillingStatus(BillingStatus.ACTIVE);
    sourceDbWorkspace.setBillingAccountName(BILLING_ACCOUNT_NAME);
    sourceDbWorkspace.setSpecificPopulationsEnum(SPECIFIC_POPULATIONS);
    sourceDbWorkspace.setResearchOutcomeEnumSet(RESEARCH_OUTCOMES);
    sourceDbWorkspace.setDisseminateResearchEnumSet(Collections.emptySet());
    sourceDbWorkspace.setDisseminateResearchOther(DISSEMINATE_FINDINGS_OTHER);
    sourceDbWorkspace.setGoogleProject(GOOGLE_PROJECT);
  }

  @Test
  public void testConvertsDbToApiWorkspace() {

    final Workspace ws =
        workspaceMapper.toApiWorkspace(sourceDbWorkspace, sourceFirecloudWorkspace);
    assertThat(ws.getId()).isEqualTo(WORKSPACE_FIRECLOUD_NAME);
    assertThat(ws.getEtag()).isEqualTo(Etags.fromVersion(WORKSPACE_VERSION));
    assertThat(ws.getName()).isEqualTo(WORKSPACE_AOU_NAME);
    assertThat(ws.getNamespace()).isEqualTo(FIRECLOUD_NAMESPACE);
    assertThat(ws.getCdrVersionId()).isEqualTo(Long.toString(CDR_VERSION_ID));
    assertThat(ws.getCreator()).isEqualTo(CREATOR_EMAIL);
    assertThat(ws.getGoogleBucketName()).isEqualTo(FIRECLOUD_BUCKET_NAME);
    assertThat(ws.getBillingAccountName()).isEqualTo(BILLING_ACCOUNT_NAME);
    assertThat(ws.getAccessTierShortName()).isEqualTo(ACCESS_TIER_SHORT_NAME);
    assertThat(ws.getGoogleProject()).isEqualTo(GOOGLE_PROJECT);

    final ResearchPurpose rp = ws.getResearchPurpose();
    assertResearchPurposeMatches(rp);

    assertThat(ws.getCreationTime()).isEqualTo(DB_CREATION_TIMESTAMP.toInstant().toEpochMilli());
    assertThat(ws.getPublished()).isEqualTo(sourceDbWorkspace.getPublished());
  }

  @Test
  public void testConvertsFirecloudResponseToApiResponse() {
    final WorkspaceResponse resp =
        workspaceMapper.toApiWorkspaceResponse(
            sourceDbWorkspace,
            new FirecloudWorkspaceResponse()
                .workspace(sourceFirecloudWorkspace)
                .accessLevel("PROJECT_OWNER"));

    assertThat(resp.getAccessLevel()).isEqualTo(WorkspaceAccessLevel.OWNER);

    // Verify data came from the DB workspace.
    assertThat(resp.getWorkspace().getBillingAccountName()).isEqualTo(BILLING_ACCOUNT_NAME);

    // Verify data came from the Firecloud workspace.
    assertThat(resp.getWorkspace().getGoogleBucketName()).isEqualTo(FIRECLOUD_BUCKET_NAME);
  }

  private void assertResearchPurposeMatches(ResearchPurpose rp) {
    assertThat(rp.getAdditionalNotes()).isEqualTo(sourceDbWorkspace.getAdditionalNotes());
    assertThat(rp.getApproved()).isEqualTo(sourceDbWorkspace.getApproved());
    assertThat(rp.getAncestry()).isEqualTo(sourceDbWorkspace.getAncestry());
    assertThat(rp.getAnticipatedFindings()).isEqualTo(sourceDbWorkspace.getAnticipatedFindings());
    assertThat(rp.getCommercialPurpose()).isEqualTo(sourceDbWorkspace.getCommercialPurpose());
    assertThat(rp.getControlSet()).isEqualTo(sourceDbWorkspace.getControlSet());
    assertThat(rp.getDiseaseFocusedResearch())
        .isEqualTo(sourceDbWorkspace.getDiseaseFocusedResearch());
    assertThat(rp.getDiseaseOfFocus()).isEqualTo(sourceDbWorkspace.getDiseaseOfFocus());
    assertThat(rp.getDrugDevelopment()).isEqualTo(sourceDbWorkspace.getDrugDevelopment());
    assertThat(rp.getEducational()).isEqualTo(sourceDbWorkspace.getEducational());
    assertThat(rp.getIntendedStudy()).isEqualTo(sourceDbWorkspace.getIntendedStudy());
    assertThat(rp.getMethodsDevelopment()).isEqualTo(sourceDbWorkspace.getMethodsDevelopment());
    assertThat(rp.getOtherPopulationDetails())
        .isEqualTo(sourceDbWorkspace.getOtherPopulationDetails());
    assertThat(rp.getOtherPurpose()).isEqualTo(sourceDbWorkspace.getOtherPurpose());
    assertThat(rp.getOtherPurposeDetails()).isEqualTo(sourceDbWorkspace.getOtherPurposeDetails());
    assertThat(rp.getPopulationDetails())
        .containsExactlyElementsIn(sourceDbWorkspace.getSpecificPopulationsEnum());
    assertThat(rp.getPopulationHealth()).isEqualTo(sourceDbWorkspace.getPopulationHealth());
    assertThat(rp.getReasonForAllOfUs()).isEqualTo(sourceDbWorkspace.getReasonForAllOfUs());
    assertThat(rp.getReviewRequested()).isEqualTo(sourceDbWorkspace.getReviewRequested());
    assertThat(rp.getSocialBehavioral()).isEqualTo(sourceDbWorkspace.getSocialBehavioral());
    assertThat(rp.getTimeRequested())
        .isEqualTo(sourceDbWorkspace.getTimeRequested().toInstant().toEpochMilli());
    assertThat(rp.getTimeReviewed()).isNull();

    assertThat(rp.getPopulationDetails()).containsAllIn(SPECIFIC_POPULATIONS);
    assertThat(rp.getResearchOutcomeList()).containsAllIn(RESEARCH_OUTCOMES);
    assertThat(rp.getDisseminateResearchFindingList()).isEmpty();
    assertThat(rp.getOtherDisseminateResearchFindings()).isEqualTo(DISSEMINATE_FINDINGS_OTHER);
  }
}
