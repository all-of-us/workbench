package org.pmiops.workbench.cohorts;

import java.util.List;
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

    FirecloudSubmissionRequest request = new FirecloudSubmissionRequest()
            .deleteIntermediateOutputFiles(false)
            .methodConfigurationNamespace(config.terraExtractionMethodConfigurationNamespace)
            .methodConfigurationName(config.terraExtractionMethodConfigurationName)
            .useCallCache(false);

    FirecloudSubmissionResponse submissionResponse = submissionApiProvider.get().createSubmission(
            config.terraWorkspaceNamespace, config.terraWorkspaceNamespace, request);
    TerraJob job = new TerraJob().submissionId(submissionResponse.getSubmissionId());

    FirecloudNewMethodConfigIngest newMethodConfigIngest = new FirecloudNewMethodConfigIngest()
            .deleted(false)
            .inputs()
            .methodConfigVersion(1) // add to config
            .methodRepoMethod()
            .name("") //generate unique for this run
            .namespace(config.terraWorkspaceNamespace)
            .outputs() // why do we need this?
            .prerequisites(null)
            .rootEntityType("participant");
    
    methodConfigurationsApiProvider.get().createWorkspaceMethodConfig(config.terraWorkspaceNamespace, config.terraWorkspaceName);

    return job;
  }

  public List<Cohort> findAll(List<Long> cohortIds) {
    return ((List<DbCohort>) cohortDao.findAll(cohortIds))
        .stream().map(cohortMapper::dbModelToClient).collect(Collectors.toList());
  }
}
