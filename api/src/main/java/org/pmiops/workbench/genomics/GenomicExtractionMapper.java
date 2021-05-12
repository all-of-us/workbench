package org.pmiops.workbench.genomics;

import com.google.common.collect.ImmutableBiMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.pmiops.workbench.db.model.DbStorageEnums;
import org.pmiops.workbench.db.model.DbWgsExtractCromwellSubmission;
import org.pmiops.workbench.firecloud.model.FirecloudSubmission;
import org.pmiops.workbench.firecloud.model.FirecloudSubmissionStatus;
import org.pmiops.workbench.firecloud.model.FirecloudWorkflow;
import org.pmiops.workbench.firecloud.model.FirecloudWorkflowStatus;
import org.pmiops.workbench.model.GenomicExtractionJob;
import org.pmiops.workbench.model.TerraJobStatus;
import org.pmiops.workbench.model.TerraWorkflowStatus;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.pmiops.workbench.utils.mappers.MapStructConfig;

@Mapper(
    config = MapStructConfig.class,
    collectionMappingStrategy = CollectionMappingStrategy.TARGET_IMMUTABLE,
    uses = {CommonMappers.class, DbStorageEnums.class})
public interface GenomicExtractionMapper {

  @Mapping(
      target = "genomicExtractionJobId",
      source = "dbSubmission.wgsExtractCromwellSubmissionId")
  @Mapping(target = "datasetName", source = "dbSubmission.dataset.name")
  @Mapping(target = "cost", source = "dbSubmission.userCost")
  @Mapping(target = "status", source = "dbSubmission.terraStatusEnum")
  @Mapping(target = "submissionDate", source = "dbSubmission.terraSubmissionDate")
  GenomicExtractionJob toApi(DbWgsExtractCromwellSubmission dbSubmission);

  default TerraJobStatus convertJobStatus(FirecloudSubmission submission) {
    // FirecloudSubmission.status doesn't capture the distinction between 'succeeded' and 'failed'
    // in 'done' so we have to check the workflows themselves.
    Set<FirecloudWorkflowStatus> workflowStatuses = submission.getWorkflows().stream()
        .map(FirecloudWorkflow::getStatus).collect(Collectors.toSet());

    if (submission.getStatus() == FirecloudSubmissionStatus.DONE) {
      if (workflowStatuses.contains(FirecloudWorkflowStatus.FAILED)) {
        return TerraJobStatus.FAILED;
      }
      return TerraJobStatus.SUCCEEDED;
    } else if (submission.getStatus() == FirecloudSubmissionStatus.ABORTED
        || submission.getStatus() == FirecloudSubmissionStatus.ABORTING) {
      return TerraJobStatus.FAILED;
    } else {
      // Accepted, Evaluating, Submitting, Submitted
      return TerraJobStatus.RUNNING;
    }
  }
}
