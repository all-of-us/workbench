package org.pmiops.workbench.cohorts;

import java.util.List;
import java.util.stream.Collectors;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.model.DbCohort;
import org.pmiops.workbench.firecloud.ApiException;
import org.pmiops.workbench.firecloud.api.SubmissionsApi;
import org.pmiops.workbench.firecloud.model.FirecloudSubmissionRequest;
import org.pmiops.workbench.model.Cohort;
import org.pmiops.workbench.model.TerraJob;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CohortService {

  private final CohortDao cohortDao;
  private final CohortMapper cohortMapper;
  private final SubmissionsApi submissionApi;

  @Autowired
  public CohortService(CohortDao cohortDao, CohortMapper cohortMapper, SubmissionsApi submissionsApi) {
    this.cohortDao = cohortDao;
    this.cohortMapper = cohortMapper;
    this.submissionApi = submissionsApi;
  }

  public TerraJob submitGenomicsCohortExtractionJob(String workspaceNamespace, String workspaceName) throws ApiException {
    FirecloudSubmissionRequest request = new FirecloudSubmissionRequest()
            .deleteIntermediateOutputFiles(false)
            .methodConfigurationName("Test")
            .methodConfigurationNamespace("aou-terra-ops-test-8")
            .useCallCache(false);

    submissionApi.createSubmission(workspaceNamespace, workspaceName, request);

    // TODO return something from submissions call and create TerraJob object
  }

  public List<Cohort> findAll(List<Long> cohortIds) {
    return ((List<DbCohort>) cohortDao.findAll(cohortIds))
        .stream().map(cohortMapper::dbModelToClient).collect(Collectors.toList());
  }
}
