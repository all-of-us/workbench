package org.pmiops.workbench.genomics;

import com.google.cloud.storage.Blob;
import com.google.common.collect.ImmutableMap;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.config.WorkbenchConfig.WgsCohortExtractionConfig;
import org.pmiops.workbench.dataset.DataSetService;
import org.pmiops.workbench.db.dao.WgsExtractCromwellSubmissionDao;
import org.pmiops.workbench.db.model.DbDataset;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWgsExtractCromwellSubmission;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.FailedPreconditionException;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.firecloud.ApiException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.api.MethodConfigurationsApi;
import org.pmiops.workbench.firecloud.api.SubmissionsApi;
import org.pmiops.workbench.firecloud.model.FirecloudMethodConfiguration;
import org.pmiops.workbench.firecloud.model.FirecloudSubmission;
import org.pmiops.workbench.firecloud.model.FirecloudSubmissionRequest;
import org.pmiops.workbench.firecloud.model.FirecloudSubmissionResponse;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspace;
import org.pmiops.workbench.google.CloudStorageClient;
import org.pmiops.workbench.google.StorageConfig;
import org.pmiops.workbench.model.GenomicExtractionJob;
import org.pmiops.workbench.model.TerraJobStatus;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.pmiops.workbench.workspaces.WorkspaceAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class GenomicExtractionService {

  private final DataSetService dataSetService;
  private final FireCloudService fireCloudService;
  private final Provider<CloudStorageClient> extractionServiceAccountCloudStorageClientProvider;
  private final Provider<SubmissionsApi> submissionApiProvider;
  private final Provider<MethodConfigurationsApi> methodConfigurationsApiProvider;
  private final WgsExtractCromwellSubmissionDao wgsExtractCromwellSubmissionDao;
  private final GenomicExtractionMapper genomicExtractionMapper;
  private final Provider<DbUser> userProvider;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;
  private final WorkspaceAuthService workspaceAuthService;
  private final Clock clock;

  @Autowired
  public GenomicExtractionService(
      DataSetService dataSetService,
      FireCloudService fireCloudService,
      @Qualifier(StorageConfig.GENOMIC_EXTRACTION_STORAGE_CLIENT)
          Provider<CloudStorageClient> extractionServiceAccountCloudStorageClientProvider,
      Provider<SubmissionsApi> submissionsApiProvider,
      Provider<MethodConfigurationsApi> methodConfigurationsApiProvider,
      WgsExtractCromwellSubmissionDao wgsExtractCromwellSubmissionDao,
      GenomicExtractionMapper genomicExtractionMapper,
      Provider<DbUser> userProvider,
      Provider<WorkbenchConfig> workbenchConfigProvider,
      WorkspaceAuthService workspaceAuthService,
      Clock clock) {
    this.dataSetService = dataSetService;
    this.fireCloudService = fireCloudService;
    this.submissionApiProvider = submissionsApiProvider;
    this.extractionServiceAccountCloudStorageClientProvider =
        extractionServiceAccountCloudStorageClientProvider;
    this.methodConfigurationsApiProvider = methodConfigurationsApiProvider;
    this.wgsExtractCromwellSubmissionDao = wgsExtractCromwellSubmissionDao;
    this.genomicExtractionMapper = genomicExtractionMapper;
    this.userProvider = userProvider;
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.workspaceAuthService = workspaceAuthService;
    this.clock = clock;
  }

  private Map<String, String> createRepoMethodParameter(
      WgsCohortExtractionConfig cohortExtractionConfig) {
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

  public List<GenomicExtractionJob> getGenomicExtractionJobs(
      String workspaceNamespace, String workspaceId) {
    DbWorkspace dbWorkspace =
        workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
            workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);

    return wgsExtractCromwellSubmissionDao.findAllByWorkspace(dbWorkspace).stream()
        .map(
            dbSubmission -> {
              try {
                // RW-6537: Don't make a call to Terra for every submission. Submissions in a non
                // running state will not change
                WgsCohortExtractionConfig cohortExtractionConfig =
                    workbenchConfigProvider.get().wgsCohortExtraction;
                FirecloudSubmission firecloudSubmission =
                    submissionApiProvider
                        .get()
                        .getSubmission(
                            cohortExtractionConfig.operationalTerraWorkspaceNamespace,
                            cohortExtractionConfig.operationalTerraWorkspaceName,
                            dbSubmission.getSubmissionId());

                if (genomicExtractionMapper.convertJobStatus(firecloudSubmission.getStatus())
                    != TerraJobStatus.RUNNING) {
                  dbSubmission.setCompletionTime(
                      CommonMappers.timestamp(
                          firecloudSubmission.getWorkflows().get(0).getStatusLastChangedDate()));
                  wgsExtractCromwellSubmissionDao.save(dbSubmission);
                }

                return genomicExtractionMapper.toApi(dbSubmission, firecloudSubmission);
              } catch (ApiException e) {
                throw new ServerErrorException("Could not fetch submission status from Terra", e);
              }
            })
        .collect(Collectors.toList());
  }

  public GenomicExtractionJob submitGenomicExtractionJob(DbWorkspace workspace, DbDataset dataSet)
      throws ApiException {
    WgsCohortExtractionConfig cohortExtractionConfig =
        workbenchConfigProvider.get().wgsCohortExtraction;

    FirecloudWorkspace fcUserWorkspace =
        fireCloudService.getWorkspace(workspace).get().getWorkspace();

    String extractionUuid = UUID.randomUUID().toString();
    String extractionFolder = "genomic-extractions/" + extractionUuid;

    List<String> personIds = dataSetService.getPersonIdsWithWholeGenome(dataSet);
    if (personIds.isEmpty()) {
      throw new FailedPreconditionException(
          "provided cohort contains no participants with whole genome data");
    }

    Blob personIdsFile =
        extractionServiceAccountCloudStorageClientProvider
            .get()
            .writeFile(
                // It is critical that this file is written to a bucket that the user cannot write
                // to because its contents will feed into a SQL query with the cohort
                // extraction SA's permissions
                cohortExtractionConfig.operationalTerraWorkspaceBucket,
                extractionFolder + "/person_ids.txt",
                String.join("\n", personIds).getBytes(StandardCharsets.UTF_8));

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
                                    + fcUserWorkspace.getBucketName()
                                    + "/"
                                    + extractionFolder
                                    + "/vcfs/\"")
                            .put(
                                "WgsCohortExtract.gatk_override",
                                "\"gs://all-of-us-workbench-test-genomics/wgs/gatk-package-4.1.9.0-204-g6449d52-SNAPSHOT-local.jar\"")
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

    // Note: if this save fails we may have an orphaned job. Will likely need a cleanup task to
    // check for such jobs.
    DbWgsExtractCromwellSubmission dbSubmission = new DbWgsExtractCromwellSubmission();
    dbSubmission.setSubmissionId(submissionResponse.getSubmissionId());
    dbSubmission.setWorkspace(workspace);
    dbSubmission.setDataset(dataSet);
    dbSubmission.setCreator(userProvider.get());
    dbSubmission.setCreationTime(new Timestamp(clock.instant().toEpochMilli()));
    dbSubmission.setSampleCount((long) personIds.size());
    wgsExtractCromwellSubmissionDao.save(dbSubmission);

    methodConfigurationsApiProvider
        .get()
        .deleteWorkspaceMethodConfig(
            cohortExtractionConfig.operationalTerraWorkspaceNamespace,
            cohortExtractionConfig.operationalTerraWorkspaceName,
            cohortExtractionConfig.extractionMethodConfigurationNamespace,
            methodConfig.getName());

    return new GenomicExtractionJob().status(TerraJobStatus.RUNNING);
  }

  public void abortExtract(DbWorkspace workspace, String wgsCohortExtractionId) throws ApiException {
    submissionApiProvider
        .get()
        .abortSubmission(
            workspace.getWorkspaceNamespace(),
            workspace.getFirecloudName(),
            wgsCohortExtractionId
        );
  }
}
