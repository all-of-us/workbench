package org.pmiops.workbench.cohorts;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.model.DbCohort;
import org.pmiops.workbench.firecloud.ApiException;
import org.pmiops.workbench.firecloud.api.MethodConfigurationsApi;
import org.pmiops.workbench.firecloud.api.SubmissionsApi;
import org.pmiops.workbench.firecloud.model.FirecloudMethodConfiguration;
import org.pmiops.workbench.firecloud.model.FirecloudSubmissionRequest;
import org.pmiops.workbench.firecloud.model.FirecloudSubmissionResponse;
import org.pmiops.workbench.model.Cohort;
import org.pmiops.workbench.model.TerraJob;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CohortService {

  private final CohortDao cohortDao;
  private final CohortMapper cohortMapper;
  private final Provider<SubmissionsApi> submissionApiProvider;
  private final Provider<MethodConfigurationsApi> methodConfigurationsApiProvider;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;

  @Autowired
  public CohortService(
      CohortDao cohortDao,
      CohortMapper cohortMapper,
      Provider<SubmissionsApi> submissionsApiProvider,
      Provider<MethodConfigurationsApi> methodConfigurationsApiProvider,
      Provider<WorkbenchConfig> workbenchConfigProvider) {
    this.cohortDao = cohortDao;
    this.cohortMapper = cohortMapper;
    this.submissionApiProvider = submissionsApiProvider;
    this.methodConfigurationsApiProvider = methodConfigurationsApiProvider;
    this.workbenchConfigProvider = workbenchConfigProvider;
  }

  private Map<String, String> createInputParameter(String msg) {
    Map<String, String> inputs = new HashMap<>();
    inputs.put("TestWf.msg", "\"" + msg + "\"");
    return inputs;
  }

  private Map<String, String> createRepoMethodParameter(
      WorkbenchConfig.WgsCohortExtractionConfig cohortExtractionConfig) {
    Map<String, String> repoMethod = new HashMap<>();
    repoMethod.put("methodName", cohortExtractionConfig.terraExtractionMethodConfigurationName);
    repoMethod.put(
        "methodVersion",
        cohortExtractionConfig.terraExtractionMethodConfigurationVersion.toString());
    repoMethod.put(
        "methodNamespace", cohortExtractionConfig.terraExtractionMethodConfigurationNamespace);
    repoMethod.put(
        "methodUri",
        "agora://"
            + cohortExtractionConfig.terraExtractionMethodConfigurationNamespace
            + "/"
            + cohortExtractionConfig.terraExtractionMethodConfigurationName
            + "/"
            + cohortExtractionConfig.terraExtractionMethodConfigurationVersion);
    repoMethod.put("sourceRepo", "agora");

    return repoMethod;
  }

  public TerraJob submitGenomicsCohortExtractionJob(String workspaceNamespace, String workspaceName)
      throws ApiException {
    WorkbenchConfig.WgsCohortExtractionConfig cohortExtractionConfig =
        workbenchConfigProvider.get().wgsCohortExtraction;

    FirecloudMethodConfiguration newMethodConfig =
        new FirecloudMethodConfiguration()
            .deleted(false)
            .inputs(
                createInputParameter(
                    "Hello from AoU (" + workspaceNamespace + "/" + workspaceName + ")!"))
            .methodConfigVersion(cohortExtractionConfig.terraExtractionMethodConfigurationVersion)
            .methodRepoMethod(createRepoMethodParameter(cohortExtractionConfig))
            .name(UUID.randomUUID().toString())
            .namespace(cohortExtractionConfig.terraExtractionMethodConfigurationNamespace)
            .outputs(new HashMap<>());

    FirecloudMethodConfiguration methodConfig =
        methodConfigurationsApiProvider
            .get()
            .createWorkspaceMethodConfig(
                cohortExtractionConfig.terraWorkspaceNamespace,
                cohortExtractionConfig.terraWorkspaceName,
                newMethodConfig)
            .getMethodConfiguration();

    FirecloudSubmissionRequest submissionRequest =
        new FirecloudSubmissionRequest()
            .deleteIntermediateOutputFiles(false)
            .methodConfigurationNamespace(methodConfig.getNamespace())
            .methodConfigurationName(methodConfig.getName())
            .useCallCache(false);

    FirecloudSubmissionResponse submissionResponse =
        submissionApiProvider
            .get()
            .createSubmission(
                cohortExtractionConfig.terraWorkspaceNamespace,
                cohortExtractionConfig.terraWorkspaceName,
                submissionRequest);

    methodConfigurationsApiProvider
        .get()
        .deleteWorkspaceMethodConfig(
            cohortExtractionConfig.terraWorkspaceNamespace,
            cohortExtractionConfig.terraWorkspaceName,
            cohortExtractionConfig.terraExtractionMethodConfigurationNamespace,
            methodConfig.getName());

    return new TerraJob().submissionId(submissionResponse.getSubmissionId());
  }

  public List<Cohort> findAll(List<Long> cohortIds) {
    return ((List<DbCohort>) cohortDao.findAll(cohortIds))
        .stream().map(cohortMapper::dbModelToClient).collect(Collectors.toList());
  }
}
