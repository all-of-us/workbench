package org.pmiops.workbench.genomics;

import com.google.cloud.storage.Blob;
import com.google.common.collect.ImmutableMap;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.inject.Provider;
import org.pmiops.workbench.cohorts.CohortService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.firecloud.ApiException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.api.MethodConfigurationsApi;
import org.pmiops.workbench.firecloud.api.SubmissionsApi;
import org.pmiops.workbench.firecloud.model.FirecloudMethodConfiguration;
import org.pmiops.workbench.firecloud.model.FirecloudSubmissionRequest;
import org.pmiops.workbench.firecloud.model.FirecloudSubmissionResponse;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspace;
import org.pmiops.workbench.google.CloudStorageClient;
import org.pmiops.workbench.google.StorageConfig;
import org.pmiops.workbench.model.TerraJob;
import org.pmiops.workbench.model.TerraJobStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class WgsCohortExtractionService {

  private final CohortService cohortService;
  private final FireCloudService fireCloudService;
  private final Provider<CloudStorageClient> cloudStorageClientProvider;
  private final Provider<SubmissionsApi> submissionApiProvider;
  private final Provider<MethodConfigurationsApi> methodConfigurationsApiProvider;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;

  @Autowired
  public WgsCohortExtractionService(
      CohortService cohortService,
      FireCloudService fireCloudService,
      @Qualifier(StorageConfig.WGS_EXTRACTION_STORAGE_CLIENT)
          Provider<CloudStorageClient> cloudStorageClientProvider,
      Provider<SubmissionsApi> submissionsApiProvider,
      Provider<MethodConfigurationsApi> methodConfigurationsApiProvider,
      Provider<WorkbenchConfig> workbenchConfigProvider) {
    this.cohortService = cohortService;
    this.fireCloudService = fireCloudService;
    this.submissionApiProvider = submissionsApiProvider;
    this.cloudStorageClientProvider = cloudStorageClientProvider;
    this.methodConfigurationsApiProvider = methodConfigurationsApiProvider;
    this.workbenchConfigProvider = workbenchConfigProvider;
  }

  private Map<String, String> createRepoMethodParameter(
      WorkbenchConfig.WgsCohortExtractionConfig cohortExtractionConfig) {
    return new ImmutableMap.Builder<String, String>()
        .put("methodName", cohortExtractionConfig.extractionMethodConfigurationName)
        .put(
            "methodVersion", cohortExtractionConfig.extractionMethodConfigurationVersion.toString())
        .put("methodNamespace", cohortExtractionConfig.extractionMethodConfigurationNamespace)
        .put(
            "methodUri",
            "agora://"
                + cohortExtractionConfig.extractionMethodConfigurationNamespace
                + "/"
                + cohortExtractionConfig.extractionMethodConfigurationName
                + "/"
                + cohortExtractionConfig.extractionMethodConfigurationVersion)
        .put("sourceRepo", "agora")
        .build();
  }

  public TerraJob submitGenomicsCohortExtractionJob(DbWorkspace workspace, Long cohortId)
      throws ApiException {
    // Currently only creates the temporary extraction tables
    // No files are being written to the user bucket

    WorkbenchConfig.WgsCohortExtractionConfig cohortExtractionConfig =
        workbenchConfigProvider.get().wgsCohortExtraction;

    FirecloudWorkspace fcWorkspace = fireCloudService.getWorkspace(workspace).get().getWorkspace();

    String extractionUuid = UUID.randomUUID().toString();
    String extractionFolder = "wgs-cohort-extractions/" + extractionUuid;

    Blob personIdsFile =
        cloudStorageClientProvider
            .get()
            .writeFile(
                // It is critical that this file is written to a bucket that the user cannot write
                // to because its contents will feed into a SQL query with the cohort
                // extraction SA's permissions
                cohortExtractionConfig.operationalTerraWorkspaceBucket,
                extractionFolder + "/person_ids.txt",
                String.join("\n", cohortService.getPersonIds(cohortId))
                    .getBytes(StandardCharsets.UTF_8));

    FirecloudMethodConfiguration methodConfig =
        methodConfigurationsApiProvider
            .get()
            .createWorkspaceMethodConfig(
                cohortExtractionConfig.operationalTerraWorkspaceNamespace,
                cohortExtractionConfig.operationalTerraWorkspaceName,
                new FirecloudMethodConfiguration()
                    .inputs(
                        new ImmutableMap.Builder<String, String>()
                            .put(
                                "WgsCohortExtract.participant_ids",
                                "\"gs://" // Cromwell string inputs require double quotes
                                    + personIdsFile.getBucket()
                                    + "/"
                                    + personIdsFile.getName()
                                    + "\"")
                            .put(
                                "WgsCohortExtract.query_project",
                                "\"" + workspace.getWorkspaceNamespace() + "\"")
                            .put("WgsCohortExtract.extraction_uuid", "\"" + extractionUuid + "\"")
                            .put(
                                "WgsCohortExtract.wgs_dataset",
                                "\""
                                    + workspace.getCdrVersion().getBigqueryProject()
                                    + "."
                                    + workspace.getCdrVersion().getWgsBigqueryDataset()
                                    + "\"")
                            .put(
                                "WgsCohortExtract.wgs_extraction_cohorts_dataset",
                                "\"" + cohortExtractionConfig.extractionCohortsDataset + "\"")
                            .put(
                                "WgsCohortExtract.wgs_extraction_destination_dataset",
                                "\"" + cohortExtractionConfig.extractionDestinationDataset + "\"")
                            .put(
                                "WgsCohortExtract.wgs_extraction_temp_tables_dataset",
                                "\"" + cohortExtractionConfig.extractionTempTablesDataset + "\"")
                            .put(
                                "WgsCohortExtract.wgs_intervals",
                                "\"gs://gcp-public-data--broad-references/hg38/v0/wgs_calling_regions.hg38.interval_list\"")
                            // This value will need to be dynamically adjusted through testing
                            .put("WgsCohortExtract.scatter_count", "1000")
                            .put(
                                "WgsCohortExtract.reference",
                                "\"gs://gcp-public-data--broad-references/hg38/v0/Homo_sapiens_assembly38.fasta\"")
                            .put(
                                "WgsCohortExtract.reference_index",
                                "\"gs://gcp-public-data--broad-references/hg38/v0/Homo_sapiens_assembly38.fasta.fai\"")
                            .put(
                                "WgsCohortExtract.reference_dict",
                                "\"gs://gcp-public-data--broad-references/hg38/v0/Homo_sapiens_assembly38.dict\"")
                            // Will produce files named "interval_1.vcf.gz", "interval_32.vcf.gz",
                            // etc
                            .put("WgsCohortExtract.output_file_base_name", "\"interval\"")
                            .put(
                                "WgsCohortExtract.output_gcs_dir",
                                "\"gs://"
                                    + fcWorkspace.getBucketName()
                                    + "/"
                                    + extractionFolder
                                    + "/vcfs/\"")
                            .put(
                                "WgsCohortExtract.gatk_override",
                                "\"gs://all-of-us-workbench-test-genomics/wgs/gatk-package-4.1.9.0-200-gfabc915-SNAPSHOT-local.jar\"")
                            .build())
                    .methodConfigVersion(
                        cohortExtractionConfig.extractionMethodConfigurationVersion)
                    .methodRepoMethod(createRepoMethodParameter(cohortExtractionConfig))
                    .name(extractionUuid)
                    .namespace(cohortExtractionConfig.extractionMethodConfigurationNamespace)
                    .outputs(new HashMap<>()))
            .getMethodConfiguration();

    FirecloudSubmissionResponse submissionResponse =
        submissionApiProvider
            .get()
            .createSubmission(
                cohortExtractionConfig.operationalTerraWorkspaceNamespace,
                cohortExtractionConfig.operationalTerraWorkspaceName,
                new FirecloudSubmissionRequest()
                    .deleteIntermediateOutputFiles(false)
                    .methodConfigurationNamespace(methodConfig.getNamespace())
                    .methodConfigurationName(methodConfig.getName())
                    .useCallCache(false));

    methodConfigurationsApiProvider
        .get()
        .deleteWorkspaceMethodConfig(
            cohortExtractionConfig.operationalTerraWorkspaceNamespace,
            cohortExtractionConfig.operationalTerraWorkspaceName,
            cohortExtractionConfig.extractionMethodConfigurationNamespace,
            methodConfig.getName());

    return new TerraJob()
        .submissionId(submissionResponse.getSubmissionId())
        .status(TerraJobStatus.RUNNING);
  }
}
