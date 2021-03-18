package org.pmiops.workbench.genomics;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.storage.Blob;
import com.google.common.collect.ImmutableList;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.pmiops.workbench.cohorts.CohortService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.WgsExtractCromwellSubmissionDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWgsExtractCromwellSubmission;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.firecloud.ApiException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.api.MethodConfigurationsApi;
import org.pmiops.workbench.firecloud.api.SubmissionsApi;
import org.pmiops.workbench.firecloud.model.FirecloudMethodConfiguration;
import org.pmiops.workbench.firecloud.model.FirecloudSubmissionResponse;
import org.pmiops.workbench.firecloud.model.FirecloudValidatedMethodConfiguration;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspace;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceResponse;
import org.pmiops.workbench.google.CloudStorageClient;
import org.pmiops.workbench.google.StorageConfig;
import org.pmiops.workbench.test.FakeClock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class WgsCohortExtractionServiceTest {

  private static final FakeClock CLOCK = new FakeClock(Instant.now(), ZoneId.systemDefault());
  private static final String FC_SUBMISSION_ID = "123";

  @Autowired WgsCohortExtractionService wgsCohortExtractionService;
  @Autowired FireCloudService fireCloudService;
  @Autowired MethodConfigurationsApi methodConfigurationsApi;
  @Autowired SubmissionsApi submissionsApi;
  @Autowired WgsExtractCromwellSubmissionDao wgsExtractCromwellSubmissionDao;
  @Autowired UserDao userDao;
  @Autowired WorkspaceDao workspaceDao;
  @Autowired CdrVersionDao cdrVersionDao;
  @Autowired CohortService mockCohortService;

  private DbWorkspace targetWorkspace;

  private static CloudStorageClient cloudStorageClient;
  private static WorkbenchConfig workbenchConfig;
  private static DbUser currentUser;

  @TestConfiguration
  @Import({WgsCohortExtractionService.class})
  @MockBean({
    CohortService.class,
    FireCloudService.class,
    MethodConfigurationsApi.class,
    SubmissionsApi.class
  })
  static class Configuration {
    @Bean
    @Scope("prototype")
    @Qualifier(StorageConfig.WGS_EXTRACTION_STORAGE_CLIENT)
    CloudStorageClient cloudStorageClient() {
      return cloudStorageClient;
    }

    // XXX: revert - CI trigger
    @Bean
    @Scope("prototype")
    DbUser user() {
      return currentUser;
    }

    @Bean
    @Scope("prototype")
    WorkbenchConfig workbenchConfig() {
      return workbenchConfig;
    }

    @Bean
    Clock clock() {
      return CLOCK;
    }
  }

  @Before
  public void setUp() throws ApiException {
    cloudStorageClient = mock(CloudStorageClient.class);
    Blob blob = mock(Blob.class);
    doReturn("bucket").when(blob).getBucket();
    doReturn("filename").when(blob).getName();
    doReturn(blob).when(cloudStorageClient).writeFile(any(), any(), any());
    workbenchConfig = new WorkbenchConfig();
    workbenchConfig.wgsCohortExtraction = new WorkbenchConfig.WgsCohortExtractionConfig();
    workbenchConfig.wgsCohortExtraction.operationalTerraWorkspaceBucket = "terraBucket";
    workbenchConfig.wgsCohortExtraction.extractionMethodConfigurationName = "methodName";
    workbenchConfig.wgsCohortExtraction.extractionMethodConfigurationNamespace = "methodNamespace";
    workbenchConfig.wgsCohortExtraction.extractionMethodConfigurationVersion = 1;

    FirecloudWorkspace fcWorkspace = new FirecloudWorkspace().bucketName("user-bucket");
    FirecloudWorkspaceResponse fcWorkspaceResponse =
        new FirecloudWorkspaceResponse().workspace(fcWorkspace);
    doReturn(Optional.of(fcWorkspaceResponse)).when(fireCloudService).getWorkspace(any());
    currentUser = createUser("a@fake-research-aou.org");

    FirecloudMethodConfiguration firecloudMethodConfiguration = new FirecloudMethodConfiguration();
    firecloudMethodConfiguration.setNamespace("methodNamespace");
    firecloudMethodConfiguration.setName("methodName");

    FirecloudValidatedMethodConfiguration validatedMethodConfiguration =
        new FirecloudValidatedMethodConfiguration();
    validatedMethodConfiguration.setMethodConfiguration(firecloudMethodConfiguration);
    doReturn(validatedMethodConfiguration)
        .when(methodConfigurationsApi)
        .createWorkspaceMethodConfig(any(), any(), any());

    DbCdrVersion cdrVersion = new DbCdrVersion();
    cdrVersion.setBigqueryProject("bigquery_project");
    cdrVersion.setWgsBigqueryDataset("wgs_dataset");
    cdrVersion = cdrVersionDao.save(cdrVersion);

    DbWorkspace workspace = new DbWorkspace();
    workspace.setName("Target DbWorkspace");
    workspace.setWorkspaceId(2);
    workspace.setCdrVersion(cdrVersion);
    targetWorkspace = workspaceDao.save(workspace);

    FirecloudSubmissionResponse submissionResponse = new FirecloudSubmissionResponse();
    submissionResponse.setSubmissionId(FC_SUBMISSION_ID);
    doReturn(submissionResponse).when(submissionsApi).createSubmission(any(), any(), any());
  }

  @Test
  public void submitExtractionJob() throws ApiException {
    when(mockCohortService.getPersonIds(any())).thenReturn(ImmutableList.of("1", "2", "3"));
    wgsCohortExtractionService.submitGenomicsCohortExtractionJob(targetWorkspace, 1l);

    verify(cloudStorageClient)
        .writeFile(
            eq(workbenchConfig.wgsCohortExtraction.operationalTerraWorkspaceBucket),
            matches("wgs-cohort-extractions\\/.*\\/person_ids.txt"),
            any());
    List<DbWgsExtractCromwellSubmission> dbSubmissions =
        ImmutableList.copyOf(wgsExtractCromwellSubmissionDao.findAll());
    assertThat(dbSubmissions.size()).isEqualTo(1);
    assertThat(dbSubmissions.get(0).getSubmissionId()).isEqualTo(FC_SUBMISSION_ID);
  }

  @Test
  public void submitExtractionJob_outputVcfsInCorrectBucket() throws ApiException {
    wgsCohortExtractionService.submitGenomicsCohortExtractionJob(targetWorkspace, 1l);

    ArgumentCaptor<FirecloudMethodConfiguration> argument =
        ArgumentCaptor.forClass(FirecloudMethodConfiguration.class);

    verify(methodConfigurationsApi).createWorkspaceMethodConfig(any(), any(), argument.capture());
    String actualOutputDir = argument.getValue().getInputs().get("WgsCohortExtract.output_gcs_dir");

    assertThat(actualOutputDir)
        .matches("\"gs:\\/\\/user-bucket\\/wgs-cohort-extractions\\/.*\\/vcfs\\/\"");
  }

  private DbUser createUser(String email) {
    DbUser user = new DbUser();
    user.setUsername(email);
    return userDao.save(user);
  }
}
