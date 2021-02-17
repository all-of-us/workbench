package org.pmiops.workbench.cohorts;

import java.util.List;
import java.util.stream.Collectors;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.model.DbCohort;
import org.pmiops.workbench.firecloud.ApiException;
import org.pmiops.workbench.firecloud.api.SubmissionsApi;
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

  @Autowired
  public CohortService(CohortDao cohortDao, CohortMapper cohortMapper, Provider<SubmissionsApi> submissionsApiProvider) {
    this.cohortDao = cohortDao;
    this.cohortMapper = cohortMapper;
    this.submissionApiProvider = submissionsApiProvider;
  }

  public TerraJob submitGenomicsCohortExtractionJob(String workspaceNamespace, String workspaceName) throws ApiException {
    FirecloudSubmissionRequest request = new FirecloudSubmissionRequest()
            .deleteIntermediateOutputFiles(false)
            .methodConfigurationName("Test")
            .methodConfigurationNamespace("aou-terra-ops-test-8")
            .useCallCache(false);

    workspaceNamespace = "aou-terra-ops-test-20";
    workspaceName = "aouterraopstestworkspace20";

    FirecloudSubmissionResponse submissionResponse = submissionApiProvider.get().createSubmission(workspaceNamespace, workspaceName, request);
    TerraJob job = new TerraJob().submissionId(submissionResponse.getSubmissionId());
    return job;
  }

  public List<Cohort> findAll(List<Long> cohortIds) {
    return ((List<DbCohort>) cohortDao.findAll(cohortIds))
        .stream().map(cohortMapper::dbModelToClient).collect(Collectors.toList());
  }
}
