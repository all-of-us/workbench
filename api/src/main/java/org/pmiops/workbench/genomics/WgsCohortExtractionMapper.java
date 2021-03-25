package org.pmiops.workbench.genomics;

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.pmiops.workbench.db.model.DbWgsExtractCromwellSubmission;
import org.pmiops.workbench.firecloud.model.FirecloudSubmission;
import org.pmiops.workbench.firecloud.model.FirecloudSubmissionStatus;
import org.pmiops.workbench.model.TerraJobStatus;
import org.pmiops.workbench.model.WgsCohortExtractionJob;
import org.pmiops.workbench.utils.mappers.MapStructConfig;

@Mapper(
        config = MapStructConfig.class,
        collectionMappingStrategy = CollectionMappingStrategy.TARGET_IMMUTABLE
)
public interface WgsCohortExtractionMapper {

    @Mapping(target = "wgsCohortExtractionJobId", source = "dbSubmission.wgsExtractCromwellSubmissionId")
    @Mapping(target = "status", source = "firecloudSubmission.status")
    @Mapping(target = "submissionDate", source = "firecloudSubmission.submissionDate")
    WgsCohortExtractionJob toApi(DbWgsExtractCromwellSubmission dbSubmission, FirecloudSubmission firecloudSubmission);

    default TerraJobStatus cdrVersionId(FirecloudSubmissionStatus status) {
        // TODO eric: not actually sure if this logic is right.
        // check with Batch team or Terra UI
        if (status == FirecloudSubmissionStatus.DONE) {
            return TerraJobStatus.SUCCEEDED;
        } else if (status == FirecloudSubmissionStatus.ABORTED || status == FirecloudSubmissionStatus.ABORTING) {
            return TerraJobStatus.FAILED;
        } else {
            return TerraJobStatus.RUNNING;
        }
    }
}
