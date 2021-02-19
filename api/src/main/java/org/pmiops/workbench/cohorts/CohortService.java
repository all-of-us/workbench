package org.pmiops.workbench.cohorts;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.model.DbCohort;
import org.pmiops.workbench.firecloud.ApiException;
import org.pmiops.workbench.firecloud.api.MethodConfigurationsApi;
import org.pmiops.workbench.firecloud.api.SubmissionsApi;
import org.pmiops.workbench.firecloud.model.FirecloudNewMethodConfigIngest;
import org.pmiops.workbench.firecloud.model.FirecloudSubmissionRequest;
import org.pmiops.workbench.firecloud.model.FirecloudSubmissionResponse;
import org.pmiops.workbench.model.Cohort;
import org.pmiops.workbench.model.TerraJob;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.inject.Provider;

@Service
public class CohortService {

  private final CohortDao cohortDao;
  private final CohortMapper cohortMapper;
  private final Provider<SubmissionsApi> submissionApiProvider;
  private final Provider<MethodConfigurationsApi> methodConfigurationsApiProvider;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;

  @Autowired
  public CohortService(CohortDao cohortDao,
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

  public TerraJob submitGenomicsCohortExtractionJob(String workspaceNamespace, String workspaceName) throws ApiException {
    WorkbenchConfig.WgsCohortExtractionConfig config = workbenchConfigProvider.get().wgsCohortExtraction;

    Map<String, String> inputs = new HashMap<>();
    inputs.put("TestWf.msg", "\"Hello from AoU!\"");
    Map<String, String> repoMethod = new HashMap<>();
    repoMethod.put("methodName", "HelloWorld");
    repoMethod.put("methodVersion", "1");
    repoMethod.put("methodNamespace", "aouwgscohortextraction");
    repoMethod.put("methodUri", "agora://aouwgscohortextraction/HelloWorld/1");
    repoMethod.put("sourceRepo", "agora");
    Map<String, String> outputs = new HashMap<>();

    String methodConfigurationName = UUID.randomUUID().toString();

    FirecloudNewMethodConfigIngest newMethodConfigIngest = new FirecloudNewMethodConfigIngest()
            .deleted(false)
            .inputs(inputs)
            .methodConfigVersion(1) // TODO: add to config
            .methodRepoMethod(repoMethod)
            .name(methodConfigurationName) // TODO: generate unique for this run
            .namespace(config.terraExtractionMethodConfigurationNamespace)
            .outputs(outputs);

    methodConfigurationsApiProvider.get().createWorkspaceMethodConfig(config.terraWorkspaceNamespace, config.terraWorkspaceName, newMethodConfigIngest);

    FirecloudSubmissionRequest request = new FirecloudSubmissionRequest()
            .deleteIntermediateOutputFiles(false)
            .methodConfigurationNamespace(config.terraExtractionMethodConfigurationNamespace)
            .methodConfigurationName(methodConfigurationName)
            .useCallCache(false);

    FirecloudSubmissionResponse submissionResponse = null;
    try {
      submissionResponse = submissionApiProvider.get().createSubmission(
              config.terraWorkspaceNamespace, config.terraWorkspaceName, request);
    } catch (ApiException e) {
      System.out.println(e);
    }

    TerraJob job = new TerraJob().submissionId(submissionResponse.getSubmissionId());

    return job;
  }

  public List<Cohort> findAll(List<Long> cohortIds) {
    return ((List<DbCohort>) cohortDao.findAll(cohortIds))
        .stream().map(cohortMapper::dbModelToClient).collect(Collectors.toList());
  }
}
