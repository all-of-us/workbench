package org.pmiops.workbench.reporting;

import org.mapstruct.Mapper;
import org.pmiops.workbench.db.model.DbStorageEnums;
import org.pmiops.workbench.model.ReportingSnapshot;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.pmiops.workbench.utils.mappers.MapStructConfig;

@Mapper(
    config = MapStructConfig.class,
    uses = {CommonMappers.class, DbStorageEnums.class})
public interface ReportingMapper {
  default ReportingSnapshot toReportingSnapshot(QueryResultBundle bundle, long snapshotTimestamp) {
    return new ReportingSnapshot()
        .captureTimestamp(snapshotTimestamp)
        .cohorts(bundle.getCohorts())
        .datasets(bundle.getDatasets())
        .datasetConceptSets(bundle.getDatasetConceptSets())
        .datasetDomainIdValues(bundle.getDatasetDomainIdValues())
        .datasetCohorts(bundle.getDatasetCohorts())
        .institutions(bundle.getInstitutions())
        .users(bundle.getUsers())
        .workspaces(bundle.getWorkspaces());
  }
}
