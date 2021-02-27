package org.pmiops.workbench.cohorts;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.storage.Blob;
import com.google.common.collect.ImmutableMap;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.inject.Provider;

import com.google.common.collect.Streams;
import com.google.gson.Gson;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.cohortbuilder.CohortQueryBuilder;
import org.pmiops.workbench.cohortbuilder.ParticipantCriteria;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.model.DbCohort;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.firecloud.ApiException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.api.MethodConfigurationsApi;
import org.pmiops.workbench.firecloud.api.SubmissionsApi;
import org.pmiops.workbench.firecloud.model.FirecloudMethodConfiguration;
import org.pmiops.workbench.firecloud.model.FirecloudSubmissionRequest;
import org.pmiops.workbench.firecloud.model.FirecloudSubmissionResponse;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspace;
import org.pmiops.workbench.google.CloudStorageService;
import org.pmiops.workbench.model.Cohort;
import org.pmiops.workbench.model.SearchRequest;
import org.pmiops.workbench.model.TerraJob;
import org.pmiops.workbench.model.TerraJobStatus;
import org.pmiops.workbench.notebooks.NotebooksService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CohortService {

  private final BigQueryService bigQueryService;
  private final CohortQueryBuilder cohortQueryBuilder;
  private final CohortDao cohortDao;
  private final CohortMapper cohortMapper;
  private final CloudStorageService cloudStorageService;
  private final FireCloudService fireCloudService;
  private final Provider<SubmissionsApi> submissionApiProvider;
  private final Provider<MethodConfigurationsApi> methodConfigurationsApiProvider;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;

  @Autowired
  public CohortService(
      BigQueryService bigQueryService,
      CohortQueryBuilder cohortQueryBuilder,
      CohortDao cohortDao,
      CohortMapper cohortMapper,
      CloudStorageService cloudStorageService,
      FireCloudService fireCloudService,
      Provider<SubmissionsApi> submissionsApiProvider,
      Provider<MethodConfigurationsApi> methodConfigurationsApiProvider,
      Provider<WorkbenchConfig> workbenchConfigProvider) {
    this.bigQueryService = bigQueryService;
    this.cohortQueryBuilder = cohortQueryBuilder;
    this.cohortDao = cohortDao;
    this.cohortMapper = cohortMapper;
    this.submissionApiProvider = submissionsApiProvider;
    this.cloudStorageService = cloudStorageService;
    this.fireCloudService = fireCloudService;
    this.methodConfigurationsApiProvider = methodConfigurationsApiProvider;
    this.workbenchConfigProvider = workbenchConfigProvider;
  }

  private Map<String, String> createInputParameter(FirecloudWorkspace fcWorkspace) {
    return new ImmutableMap.Builder<String, String>()
            .put("WgsCohortExtract.participant_ids", "\"gs://fc-secure-29a76157-95db-49d6-842c-68bdc869e99d/sample_names.txt\"")
            .put("WgsCohortExtract.query_project", "\"" + fcWorkspace.getNamespace()  + "\"")
            .build();
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

  private List<String> getPersonIds(Long cohortId) {
    String cohortDefinition = cohortDao.findOne(cohortId).getCriteria();

    final SearchRequest searchRequest = new Gson().fromJson(cohortDefinition, SearchRequest.class);

    final QueryJobConfiguration participantIdQuery =
            cohortQueryBuilder.buildParticipantIdQuery(new ParticipantCriteria(searchRequest));

    return Streams.stream(bigQueryService.executeQuery(bigQueryService.filterBigQueryConfig(participantIdQuery))
            .getValues())
            .map(personId -> personId.get(0).getValue().toString())
            .collect(Collectors.toList());
  }

  public TerraJob submitGenomicsCohortExtractionJob(DbWorkspace workspace, Long cohortId)
      throws ApiException {
    WorkbenchConfig.WgsCohortExtractionConfig cohortExtractionConfig =
        workbenchConfigProvider.get().wgsCohortExtraction;

    FirecloudWorkspace fcWorkspace = fireCloudService.getWorkspace(workspace).get().getWorkspace();
    String extractionUuid = UUID.randomUUID().toString();
    String filename = extractionUuid + "_person_ids.txt";
    Blob personIdsFile = cloudStorageService.writeFile(
            fcWorkspace.getBucketName(), filename, String.join("\n", getPersonIds(cohortId)).getBytes(StandardCharsets.UTF_8));

    FirecloudMethodConfiguration methodConfig =
        methodConfigurationsApiProvider
            .get()
            .createWorkspaceMethodConfig(
                cohortExtractionConfig.operationalTerraWorkspaceNamespace,
                cohortExtractionConfig.operationalTerraWorkspaceName,
                new FirecloudMethodConfiguration()
                    .deleted(false)
                    .inputs(new ImmutableMap.Builder<String, String>()
                            .put("WgsCohortExtract.participant_ids", "\"gs://" + personIdsFile.getBlobId().getBucket() + "/" + personIdsFile.getBlobId().getName() + "\"")
                            .put("WgsCohortExtract.query_project", "\"" + fcWorkspace.getNamespace()  + "\"")
                            .put("WgsCohortExtract.extraction_uuid", "\"" + extractionUuid  + "\"")
                            .put("WgsCohortExtract.wgs_dataset", "\"fc-aou-cdr-synth-test.1kg_wgs\"") // TODO swap out with RW-6336
                            .put("WgsCohortExtract.wgs_extraction_cohorts_dataset", "\"" + cohortExtractionConfig.extractionCohortsDataset  + "\"")
                            .put("WgsCohortExtract.wgs_extraction_destination_dataset", "\"" + cohortExtractionConfig.extractionDestinationDataset  + "\"")
                            .put("WgsCohortExtract.wgs_extraction_temp_tables_dataset", "\"" + cohortExtractionConfig.extractionTempTablesDataset  + "\"")
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

  public List<Cohort> findAll(List<Long> cohortIds) {
    return ((List<DbCohort>) cohortDao.findAll(cohortIds))
        .stream().map(cohortMapper::dbModelToClient).collect(Collectors.toList());
  }
}
