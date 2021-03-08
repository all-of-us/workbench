package org.pmiops.workbench.genomics;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.google.cloud.storage.Blob;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.pmiops.workbench.cohorts.CohortService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.model.DbCdrVersion;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Optional;

@RunWith(SpringRunner.class)
public class WgsCohortExtractionServiceTest {

  @Autowired WgsCohortExtractionService wgsCohortExtractionService;
  @Autowired FireCloudService fireCloudService;
  @Autowired MethodConfigurationsApi methodConfigurationsApi;
  @Autowired SubmissionsApi submissionsApi;

  private static CloudStorageClient cloudStorageClient;
  private static WorkbenchConfig workbenchConfig;

  @TestConfiguration
  @Import({WgsCohortExtractionService.class})
  @MockBean({CohortService.class, FireCloudService.class, MethodConfigurationsApi.class, SubmissionsApi.class})
  static class Configuration {
    @Bean
    @Scope("prototype")
    @Qualifier(StorageConfig.WGS_EXTRACTION_STORAGE_CLIENT)
    CloudStorageClient cloudStorageClient() {
      return cloudStorageClient;
    }

    @Bean
    @Scope("prototype")
    WorkbenchConfig workbenchConfig() {
      return workbenchConfig;
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

    FirecloudWorkspace fcWorkspace = new FirecloudWorkspace()
            .bucketName("user-bucket");
    FirecloudWorkspaceResponse fcWorkspaceResponse = new FirecloudWorkspaceResponse()
            .workspace(fcWorkspace);
    doReturn(Optional.of(fcWorkspaceResponse)).when(fireCloudService).getWorkspace(any());

    FirecloudMethodConfiguration firecloudMethodConfiguration = new FirecloudMethodConfiguration();
    firecloudMethodConfiguration.setNamespace("methodNamespace");
    firecloudMethodConfiguration.setName("methodName");

    FirecloudValidatedMethodConfiguration validatedMethodConfiguration =
        new FirecloudValidatedMethodConfiguration();
    validatedMethodConfiguration.setMethodConfiguration(firecloudMethodConfiguration);
    doReturn(validatedMethodConfiguration)
        .when(methodConfigurationsApi)
        .createWorkspaceMethodConfig(any(), any(), any());

    FirecloudSubmissionResponse submissionResponse = new FirecloudSubmissionResponse();
    submissionResponse.setSubmissionId("123");
    doReturn(submissionResponse).when(submissionsApi).createSubmission(any(), any(), any());
  }

  @Test
  public void submitExtractionJob_personIdFileInExtractionBucket() throws ApiException {
    wgsCohortExtractionService.submitGenomicsCohortExtractionJob(mockWorkspace(), 1l);

    verify(cloudStorageClient)
        .writeFile(
            eq(workbenchConfig.wgsCohortExtraction.operationalTerraWorkspaceBucket),
            matches("wgs-cohort-extractions\\/.*\\/person_ids.txt"),
            any());
  }

  @Test
  public void submitExtractionJob_outputVcfsInCorrectBucket() throws ApiException {
    wgsCohortExtractionService.submitGenomicsCohortExtractionJob(mockWorkspace(), 1l);

    ArgumentCaptor<FirecloudMethodConfiguration> argument = ArgumentCaptor.forClass(FirecloudMethodConfiguration.class);

    verify(methodConfigurationsApi).createWorkspaceMethodConfig(any(), any(), argument.capture());
    String actualOutputDir = argument.getValue().getInputs().get("WgsCohortExtract.output_gcs_dir");

    assertThat(actualOutputDir).matches("\"gs:\\/\\/user-bucket\\/wgs-cohort-extractions\\/.*\\/vcfs\\/\"");
  }

  private DbWorkspace mockWorkspace() {
    DbWorkspace workspace = new DbWorkspace();
    workspace.setName("Target DbWorkspace");
    workspace.setWorkspaceId(2);
    DbCdrVersion cdrVersion = new DbCdrVersion();
    cdrVersion.setBigqueryProject("bigquery_project");
    cdrVersion.setWgsBigqueryDataset("wgs_dataset");
    workspace.setCdrVersion(cdrVersion);
    return workspace;
  }
}
