package org.pmiops.workbench.utils.mappers;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableSet;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.api.Etags;
import org.pmiops.workbench.cohortreview.CohortReviewMapperImpl;
import org.pmiops.workbench.cohorts.CohortMapperImpl;
import org.pmiops.workbench.conceptset.ConceptSetMapperImpl;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.dataset.DataSetMapperImpl;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.db.model.DbWorkspace.BillingMigrationStatus;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspace;
import org.pmiops.workbench.model.BillingAccountType;
import org.pmiops.workbench.model.BillingStatus;
import org.pmiops.workbench.model.DataAccessLevel;
import org.pmiops.workbench.model.ResearchOutcomeEnum;
import org.pmiops.workbench.model.ResearchPurpose;
import org.pmiops.workbench.model.SpecificPopulationEnum;
import org.pmiops.workbench.model.Workspace;
import org.pmiops.workbench.model.WorkspaceActiveStatus;
import org.pmiops.workbench.utils.WorkspaceMapper;
import org.pmiops.workbench.utils.WorkspaceMapperImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class WorkspaceMapperTest {

  private static final String FIRECLOUD_NAMESPACE = "aou-xxxxxxx";
  private static final String CREATOR_EMAIL = "ojc@verily.biz";
  private static final long CREATOR_USER_ID = 101L;
  private static final long WORKSPACE_DB_ID = 222L;
  private static final int WORKSPACE_VERSION = 2;
  private static final String WORKSPACE_AOU_NAME = "studyallthethings";
  private static final String WORKSPACE_FIRECLOUD_NAME = "aaaa-bbbb-cccc-dddd";
  private static final String BILLING_ACCOUNT_NAME = "billing-account";

  private static final DataAccessLevel DATA_ACCESS_LEVEL = DataAccessLevel.REGISTERED;
  private static final Timestamp DB_CREATION_TIMESTAMP =
      Timestamp.from(Instant.parse("2000-01-01T00:00:00.00Z"));
  private static final Timestamp DB_LAST_MODIFIED_TIMESTAMP =
      Timestamp.from(Instant.parse("2011-10-31T00:00:00.00Z"));
  private static final int CDR_VERSION_ID = 2;
  private static final String FIRECLOUD_BUCKET_NAME = "my-favorite-bucket";
  private static final ImmutableSet<SpecificPopulationEnum> SPECIFIC_POPULATIONS =
      ImmutableSet.of(SpecificPopulationEnum.DISABILITY_STATUS, SpecificPopulationEnum.GEOGRAPHY);
  private static final ImmutableSet<ResearchOutcomeEnum> RESEARCH_OUTCOMES =
      ImmutableSet.of(ResearchOutcomeEnum.DECREASE_ILLNESS_BURDEN);
  private static final String DISSEMINATE_FINDINGS_OTHER = "Everywhere except MIT.";
  private static final boolean PUBLISHED = false;
  private static final BillingMigrationStatus BILLING_MIGRATION_STATUS = BillingMigrationStatus.MIGRATED;
  private static final String DISEASE_OF_FOCUS = "leukemia";
  private static final String OTHER_PURPOSE_DETAILS = "I want to discover a new disease.";
  private static final String ADDITIONAL_NOTES = "remember to wash hands.";
  private static final String AOU_REASON = "We can't get this data anywhere else.";
  private static final String INTENDED_STUDY = null;

  private DbCdrVersion cdrVersion;
  private DbUser creatorUser;
  private DbWorkspace sourceDbWorkspace;
  private FirecloudWorkspace sourceFirecloudWorkspace;
  private ResearchPurpose sourceResearchPurpose;
  private Workspace sourceApiWorkspace;

  @Autowired private WorkspaceMapper workspaceMapper;

  @TestConfiguration
  @Import({
    CohortMapperImpl.class,
    CohortReviewMapperImpl.class,
    CommonMappers.class,
    ConceptSetMapperImpl.class,
    DataSetMapperImpl.class,
    WorkspaceMapperImpl.class,
  })
  @MockBean({UserDao.class, WorkspaceDao.class})
  static class Configuration {
    @Bean
    WorkbenchConfig workbenchConfig() {
      WorkbenchConfig workbenchConfig = new WorkbenchConfig();
      workbenchConfig.featureFlags = new WorkbenchConfig.FeatureFlagsConfig();
      workbenchConfig.featureFlags.enableBillingLockout = false;
      return workbenchConfig;
    }
  }

  @Before
  public void setUp() {
    sourceFirecloudWorkspace =
        new FirecloudWorkspace()
            .workspaceId(Long.toString(CREATOR_USER_ID))
            .bucketName(FIRECLOUD_BUCKET_NAME)
            .createdBy(CREATOR_EMAIL)
            .namespace(FIRECLOUD_NAMESPACE)
            .name(WORKSPACE_FIRECLOUD_NAME);

    creatorUser = new DbUser();
    creatorUser.setUsername(CREATOR_EMAIL);
    creatorUser.setDataAccessLevelEnum(DATA_ACCESS_LEVEL);
    creatorUser.setUserId(CREATOR_USER_ID);

    cdrVersion = new DbCdrVersion();
    cdrVersion.setCdrVersionId(CDR_VERSION_ID);

    sourceDbWorkspace = new DbWorkspace();
    sourceDbWorkspace.setWorkspaceId(WORKSPACE_DB_ID);
    sourceDbWorkspace.setVersion(WORKSPACE_VERSION);
    sourceDbWorkspace.setName(WORKSPACE_AOU_NAME);
    sourceDbWorkspace.setFirecloudName(WORKSPACE_FIRECLOUD_NAME);
    sourceDbWorkspace.setDataAccessLevelEnum(DATA_ACCESS_LEVEL);
    sourceDbWorkspace.setCdrVersion(cdrVersion);
    sourceDbWorkspace.setCreator(creatorUser);
    sourceDbWorkspace.setCreationTime(DB_CREATION_TIMESTAMP);
    sourceDbWorkspace.setLastModifiedTime(DB_CREATION_TIMESTAMP);
    sourceDbWorkspace.setLastAccessedTime(
        Timestamp.from(DB_CREATION_TIMESTAMP.toInstant().plus(Duration.ofMinutes(15))));
    sourceDbWorkspace.setCohorts(Collections.emptySet());
    sourceDbWorkspace.setDataSets(Collections.emptySet());
    sourceDbWorkspace.setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.ACTIVE);
    sourceDbWorkspace.setBillingMigrationStatusEnum(BILLING_MIGRATION_STATUS);
    sourceDbWorkspace.setPublished(false);
    sourceDbWorkspace.setDiseaseFocusedResearch(true);
    sourceDbWorkspace.setDiseaseOfFocus(DISEASE_OF_FOCUS);
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
    sourceDbWorkspace.setOtherPurposeDetails(OTHER_PURPOSE_DETAILS);
    sourceDbWorkspace.setOtherPopulationDetails(null);
    sourceDbWorkspace.setAdditionalNotes(ADDITIONAL_NOTES);
    sourceDbWorkspace.setReasonForAllOfUs(AOU_REASON);
    sourceDbWorkspace.setIntendedStudy(INTENDED_STUDY);
    sourceDbWorkspace.setReviewRequested(false);
    sourceDbWorkspace.setApproved(true);
    sourceDbWorkspace.setTimeRequested(DB_CREATION_TIMESTAMP);
    sourceDbWorkspace.setBillingStatus(BillingStatus.ACTIVE);
    sourceDbWorkspace.setBillingAccountName(BILLING_ACCOUNT_NAME);
    sourceDbWorkspace.setSpecificPopulationsEnum(SPECIFIC_POPULATIONS);
    sourceDbWorkspace.setResearchOutcomeEnumSet(RESEARCH_OUTCOMES);
    sourceDbWorkspace.setDisseminateResearchEnumSet(Collections.emptySet());
    sourceDbWorkspace.setDisseminateResearchOther(DISSEMINATE_FINDINGS_OTHER);

    sourceResearchPurpose = new ResearchPurpose()
        .additionalNotes(null)
        .ancestry(false)
        .anticipatedFindings("at least 2 four-leaf clovers")
        .approved(null)
        .commercialPurpose(false)
        .controlSet(false)
        .diseaseFocusedResearch(false)
        .diseaseOfFocus(DISEASE_OF_FOCUS)
        .disseminateResearchFindingList(Collections.emptyList())
        .drugDevelopment(false)
        .educational(false)
        .ethics(false)
        .intendedStudy(INTENDED_STUDY)
        .methodsDevelopment(true)
        .otherDisseminateResearchFindings(DISSEMINATE_FINDINGS_OTHER)
        .otherPopulationDetails("")
        .otherPurpose(false)
        .otherPurposeDetails("")
        .populationDetails(new ArrayList<>(SPECIFIC_POPULATIONS))
        .populationHealth(false)
        .reasonForAllOfUs(AOU_REASON)
        .researchOutcomeList(new ArrayList<>(RESEARCH_OUTCOMES))
        .reviewRequested(false)
        .scientificApproach(null)
        .socialBehavioral(false)
        .timeRequested(null)
        .timeReviewed(null);

    sourceApiWorkspace = new Workspace()
      .id(WORKSPACE_FIRECLOUD_NAME)
      .etag(Etags.fromVersion(WORKSPACE_VERSION))
      .name("thequickbrownfox")
      .namespace("aou-xxxxxxx")
      .cdrVersionId("2")
      .creator("ojc@verily.biz")
      .billingAccountName("billing-account")
      .billingAccountType(BillingAccountType.FREE_TIER)
      .googleBucketName("buckekt-1")
      .dataAccessLevel(DataAccessLevel.REGISTERED)
      .researchPurpose(sourceResearchPurpose)
      .billingStatus(BillingStatus.ACTIVE)
      .creationTime(DB_CREATION_TIMESTAMP.getTime())
      .lastModifiedTime(DB_CREATION_TIMESTAMP.getTime())
      .published(PUBLISHED);
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
    assertThat(ws.getDataAccessLevel()).isEqualTo(DATA_ACCESS_LEVEL);
    assertThat(ws.getBillingAccountName()).isEqualTo(BILLING_ACCOUNT_NAME);

    final ResearchPurpose rp = ws.getResearchPurpose();
    assertResearchPurposeMatches(sourceDbWorkspace, rp);

    assertThat(ws.getCreationTime()).isEqualTo(DB_CREATION_TIMESTAMP.toInstant().toEpochMilli());
    assertThat(ws.getPublished()).isEqualTo(sourceDbWorkspace.getPublished());
  }

  // Note that this can be used two ways, but the error messages' expected vs actal labels will
  // be swapped if your research purpose is observed instead of expected.
  private void assertResearchPurposeMatches(DbWorkspace dbWorkspace, ResearchPurpose researchPurpose) {
    assertThat(researchPurpose.getAdditionalNotes()).isEqualTo(dbWorkspace.getAdditionalNotes());
    assertThat(researchPurpose.getApproved()).isEqualTo(dbWorkspace.getApproved());
    assertThat(researchPurpose.getAncestry()).isEqualTo(dbWorkspace.getAncestry());
    assertThat(researchPurpose.getAnticipatedFindings()).isEqualTo(dbWorkspace.getAnticipatedFindings());
    assertThat(researchPurpose.getCommercialPurpose()).isEqualTo(dbWorkspace.getCommercialPurpose());
    assertThat(researchPurpose.getControlSet()).isEqualTo(dbWorkspace.getControlSet());
    assertThat(researchPurpose.getDiseaseFocusedResearch())
        .isEqualTo(dbWorkspace.getDiseaseFocusedResearch());
    assertThat(researchPurpose.getDiseaseOfFocus()).isEqualTo(dbWorkspace.getDiseaseOfFocus());
    assertThat(researchPurpose.getDrugDevelopment()).isEqualTo(dbWorkspace.getDrugDevelopment());
    assertThat(researchPurpose.getEducational()).isEqualTo(dbWorkspace.getEducational());
    assertThat(researchPurpose.getIntendedStudy()).isEqualTo(dbWorkspace.getIntendedStudy());
    assertThat(researchPurpose.getMethodsDevelopment()).isEqualTo(dbWorkspace.getMethodsDevelopment());
    assertThat(researchPurpose.getOtherPopulationDetails())
        .isEqualTo(dbWorkspace.getOtherPopulationDetails());
    assertThat(researchPurpose.getOtherPurpose()).isEqualTo(dbWorkspace.getOtherPurpose());
    assertThat(researchPurpose.getOtherPurposeDetails()).isEqualTo(dbWorkspace.getOtherPurposeDetails());
    assertThat(researchPurpose.getPopulationDetails())
        .containsExactlyElementsIn(dbWorkspace.getSpecificPopulationsEnum());
    assertThat(researchPurpose.getPopulationHealth()).isEqualTo(dbWorkspace.getPopulationHealth());
    assertThat(researchPurpose.getReasonForAllOfUs()).isEqualTo(dbWorkspace.getReasonForAllOfUs());
    assertThat(researchPurpose.getReviewRequested()).isEqualTo(dbWorkspace.getReviewRequested());
    assertThat(researchPurpose.getSocialBehavioral()).isEqualTo(dbWorkspace.getSocialBehavioral());
    assertThat(researchPurpose.getTimeRequested())
        .isEqualTo(Optional.ofNullable(dbWorkspace.getTimeRequested())
            .map(t -> t.toInstant().toEpochMilli()).orElse(null));
    assertThat(researchPurpose.getTimeReviewed()).isNull();

    assertThat(researchPurpose.getPopulationDetails()).containsAllIn(SPECIFIC_POPULATIONS);
    assertThat(researchPurpose.getResearchOutcomeList()).containsAllIn(RESEARCH_OUTCOMES);
    assertThat(researchPurpose.getDisseminateResearchFindingList()).isEmpty();
    assertThat(researchPurpose.getOtherDisseminateResearchFindings()).isEqualTo(DISSEMINATE_FINDINGS_OTHER);
  }

  @Test
  public void testToDbWorkspace() {
    final DbWorkspace convertedDbWorkspace = workspaceMapper.toDbWorkspace(
        sourceApiWorkspace,
        sourceFirecloudWorkspace,
        creatorUser,
        WorkspaceActiveStatus.ACTIVE,
        Timestamp.from(DB_CREATION_TIMESTAMP.toInstant().plus(Duration.ofMinutes(15))),
        BILLING_MIGRATION_STATUS,
        cdrVersion,
        Collections.emptySet(),
        Collections.emptySet());

    assertResearchPurposeMatches(convertedDbWorkspace, sourceApiWorkspace.getResearchPurpose());

    //    assertThat(convertedDbWorkspace.)
  }
}
