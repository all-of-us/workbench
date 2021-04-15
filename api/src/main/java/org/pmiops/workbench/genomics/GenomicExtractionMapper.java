package org.pmiops.workbench.genomics;

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.pmiops.workbench.db.model.DbWgsExtractCromwellSubmission;
import org.pmiops.workbench.firecloud.model.FirecloudSubmission;
import org.pmiops.workbench.firecloud.model.FirecloudSubmissionStatus;
import org.pmiops.workbench.model.GenomicExtractionJob;
import org.pmiops.workbench.model.TerraJobStatus;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.pmiops.workbench.utils.mappers.MapStructConfig;

@Mapper(
    config = MapStructConfig.class,
    collectionMappingStrategy = CollectionMappingStrategy.TARGET_IMMUTABLE,
    uses = {CommonMappers.class})
public interface GenomicExtractionMapper {

  @Mapping(
      target = "genomicExtractionJobId",
      source = "dbSubmission.wgsExtractCromwellSubmissionId")
  @Mapping(target = "status", source = "firecloudSubmission.status")
  @Mapping(target = "submissionDate", source = "firecloudSubmission.submissionDate")
  GenomicExtractionJob toApi(
      DbWgsExtractCromwellSubmission dbSubmission, FirecloudSubmission firecloudSubmission);

  default TerraJobStatus cdrVersionId(FirecloudSubmissionStatus status) {
    if (status == FirecloudSubmissionStatus.DONE) {
      return TerraJobStatus.SUCCEEDED;
    } else if (status == FirecloudSubmissionStatus.ABORTED
        || status == FirecloudSubmissionStatus.ABORTING) {
      return TerraJobStatus.FAILED;
    } else {
      return TerraJobStatus.RUNNING;
    }
  }
}
