package org.pmiops.workbench.utils.mappers;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableSet;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.api.Etags;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.db.model.DbWorkspace.BillingMigrationStatus;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspace;
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
  private static final int CDR_VERSION_ID = 2;
  private static final String FIRECLOUD_BUCKET_NAME = "my-favorite-bucket";
  private static final ImmutableSet<SpecificPopulationEnum> SPECIFIC_POPULATIONS =
      ImmutableSet.of(SpecificPopulationEnum.DISABILITY_STATUS, SpecificPopulationEnum.GEOGRAPHY);
  private static final ImmutableSet<ResearchOutcomeEnum> RESEARCH_OUTCOMES =
      ImmutableSet.of(ResearchOutcomeEnum.DECREASE_ILLNESS_BURDEN);
  private static final String DISSEMINATE_FINDINGS_OTHER = "Everywhere except MIT.";

  private DbWorkspace sourceDbWorkspace;
  private FirecloudWorkspace sourceFirecloudWorkspace;

  @Autowired private WorkspaceMapper workspaceMapper;

  @TestConfiguration
  @Import({WorkspaceMapperImpl.class})
  @MockBean({WorkspaceDao.class})
  static class Configuration {}

  @Before
  public void setUp() {
    sourceFirecloudWorkspace =
        new FirecloudWorkspace()
            .workspaceId(Long.toString(CREATOR_USER_ID))
            .bucketName(FIRECLOUD_BUCKET_NAME)
            .createdBy(CREATOR_EMAIL)
            .namespace(FIRECLOUD_NAMESPACE)
            .name(WORKSPACE_FIRECLOUD_NAME);

    final DbUser creatorUser = new DbUser();
    creatorUser.setUsername(CREATOR_EMAIL);
    creatorUser.setDataAccessLevelEnum(DATA_ACCESS_LEVEL);
    creatorUser.setUserId(CREATOR_USER_ID);

    final DbCdrVersion cdrVersion = new DbCdrVersion();
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
    assertResearchPurposeMatches(rp);

    assertThat(ws.getCreationTime()).isEqualTo(DB_CREATION_TIMESTAMP.toInstant().toEpochMilli());
    assertThat(ws.getPublished()).isEqualTo(sourceDbWorkspace.getPublished());
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
