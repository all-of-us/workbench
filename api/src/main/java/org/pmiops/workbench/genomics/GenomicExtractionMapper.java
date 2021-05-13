package org.pmiops.workbench.genomics;

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.pmiops.workbench.db.model.DbStorageEnums;
import org.pmiops.workbench.db.model.DbWgsExtractCromwellSubmission;
import org.pmiops.workbench.firecloud.model.FirecloudWorkflowStatus;
import org.pmiops.workbench.model.GenomicExtractionJob;
import org.pmiops.workbench.model.TerraJobStatus;
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

  default TerraJobStatus convertWorkflowStatus(FirecloudWorkflowStatus status) {
    if (status == FirecloudWorkflowStatus.SUCCEEDED) {
      return TerraJobStatus.SUCCEEDED;
    } else if (status == FirecloudWorkflowStatus.FAILED) {
      return TerraJobStatus.FAILED;
    } else if (status == FirecloudWorkflowStatus.ABORTED
        || status == FirecloudWorkflowStatus.ABORTING) {
      return TerraJobStatus.ABORTED;
    } else {
      // Launching, Queued, Running, Submitted
      return TerraJobStatus.RUNNING;
    }
  }
}
