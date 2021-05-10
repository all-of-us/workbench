package org.pmiops.workbench.genomics;

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.pmiops.workbench.db.model.DbStorageEnums;
import org.pmiops.workbench.db.model.DbWgsExtractCromwellSubmission;
import org.pmiops.workbench.firecloud.model.FirecloudSubmissionStatus;
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
  @Mapping(target = "status", source = "dbSubmission.terraStatus")
  @Mapping(target = "submissionDate", source = "dbSubmission.terraSubmissionDate")
  GenomicExtractionJob toApi(DbWgsExtractCromwellSubmission dbSubmission);

  default TerraJobStatus convertJobStatus(FirecloudSubmissionStatus status) {
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
