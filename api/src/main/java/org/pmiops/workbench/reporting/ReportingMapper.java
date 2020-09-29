package org.pmiops.workbench.reporting;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import org.mapstruct.Mapper;
import org.pmiops.workbench.db.dao.projection.ProjectedReportingCohort;
import org.pmiops.workbench.db.dao.projection.ProjectedReportingUser;
import org.pmiops.workbench.db.dao.projection.ProjectedReportingWorkspace;
import org.pmiops.workbench.db.model.DbStorageEnums;
import org.pmiops.workbench.model.ReportingCohort;
import org.pmiops.workbench.model.ReportingUser;
import org.pmiops.workbench.model.ReportingWorkspace;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.pmiops.workbench.utils.mappers.MapStructConfig;

@Mapper(
    config = MapStructConfig.class,
    uses = {CommonMappers.class, DbStorageEnums.class})
public interface ReportingMapper {
  int BATCH_SIZE = 1000;

  ReportingUser toDto(ProjectedReportingUser prjUser);

  List<ReportingUser> toReportingUserList(Collection<ProjectedReportingUser> users);

  default <PRJ_T, MODEL_T> List<MODEL_T> mapList(List<PRJ_T> projections,
      Function<Collection<PRJ_T>, List<MODEL_T>> listMapFn) {
    return Lists.partition(projections, BATCH_SIZE).stream()
        .flatMap(batch -> listMapFn.apply(batch).stream())
        .collect(ImmutableList.toImmutableList());
  }

  ReportingWorkspace toDto(ProjectedReportingWorkspace prjWorkspace);

  List<ReportingWorkspace> toReportingWorkspaceList(
      Collection<ProjectedReportingWorkspace> dbWorkspace);

  ReportingCohort toDto(ProjectedReportingCohort cohort);

  List<ReportingCohort> toModelList(Collection<ProjectedReportingCohort> cohorts);
}
